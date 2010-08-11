/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.seam.persistence.transactions.test;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TransactionRequiredException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.SeamPersistenceProvider;
import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.persistence.transaction.TransactionExtension;
import org.jboss.seam.persistence.transaction.TransactionInterceptor;
import org.jboss.seam.persistence.util.NamingUtils;
import org.jboss.seam.transactions.test.util.ArtifactNames;
import org.jboss.seam.transactions.test.util.DontRollBackException;
import org.jboss.seam.transactions.test.util.EntityManagerProvider;
import org.jboss.seam.transactions.test.util.HelloService;
import org.jboss.seam.transactions.test.util.Hotel;
import org.jboss.seam.transactions.test.util.MavenArtifactResolver;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the @TransactionAttribute interceptor for non ee beans.
 * 
 * TODO: refactor the tests to share a common superclass
 * 
 * @author stuart
 * 
 */
@RunWith(Arquillian.class)
public class TransactionAttributeInterceptorTest
{
   @Deployment
   public static Archive<?> createTestArchive()
   {

      WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.SEAM_PERSISTENCE_API));
      war.addPackage(TransactionExtension.class.getPackage());
      war.addPackage(SeamPersistenceProvider.class.getPackage());
      war.addPackage(NamingUtils.class.getPackage());
      war.addClasses(TransactionAttributeInterceptorTest.class, TransactionAttributeManagedBean.class, HelloService.class, Hotel.class, EntityManagerProvider.class, DontRollBackException.class);
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(("<beans><interceptors><class>" + TransactionInterceptor.class.getName() + "</class></interceptors></beans>").getBytes()), "beans.xml");
      war.addWebResource("META-INF/services/javax.enterprise.inject.spi.Extension", "classes/META-INF/services/javax.enterprise.inject.spi.Extension");
      return war;
   }

   @Inject
   TransactionAttributeManagedBean bean;

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @PersistenceContext
   EntityManager em;

   @Test
   public void testTransactionInterceptor() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      bean.addHotel();
      assertHotels(1);
      try
      {
         bean.failToAddHotel();
      }
      catch (Exception e)
      {
      }
      assertHotels(1);
      try
      {
         bean.addHotelWithApplicationException();
      }
      catch (DontRollBackException e)
      {
      }
      assertHotels(2);
   }

   @Test(expected = TransactionRequiredException.class)
   public void testTransactionInterceptorMethodOverrides()
   {
      bean.tryAndAddHotelWithNoTransaction();
   }

   public void assertHotels(int count) throws NotSupportedException, SystemException
   {
      transaction.begin();
      em.joinTransaction();
      List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
      Assert.assertTrue("Wrong number of hotels: " + hotels.size(), hotels.size() == count);
      transaction.rollback();
   }
}
