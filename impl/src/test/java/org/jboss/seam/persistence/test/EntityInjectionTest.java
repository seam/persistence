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
package org.jboss.seam.persistence.test;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.InjectionEventListener;
import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.persistence.transaction.TransactionExtension;
import org.jboss.seam.persistence.util.NamingUtils;
import org.jboss.seam.transactions.test.util.ArtifactNames;
import org.jboss.seam.transactions.test.util.HelloService;
import org.jboss.seam.transactions.test.util.Hotel;
import org.jboss.seam.transactions.test.util.ManagedPersistenceContextProvider;
import org.jboss.seam.transactions.test.util.MavenArtifactResolver;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests that injection is working for JPA entities
 * 
 * @author Stuart Douglas
 * 
 */
@RunWith(Arquillian.class)
public class EntityInjectionTest
{
   @Deployment
   public static Archive<?> createTestArchive()
   {
      WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.SEAM_PERSISTENCE_API));
      war.addPackage(TransactionExtension.class.getPackage());
      war.addPackage(InjectionEventListener.class.getPackage());
      war.addPackage(NamingUtils.class.getPackage());
      war.addClasses(EntityInjectionTest.class, Hotel.class, ManagedPersistenceContextProvider.class, HelloService.class);
      war.addWebResource("META-INF/services/javax.enterprise.inject.spi.Extension", "classes/META-INF/services/javax.enterprise.inject.spi.Extension");
      war.addWebResource("META-INF/persistence-orm.xml", "classes/META-INF/persistence.xml");
      war.addWebResource("META-INF/orm.xml", "classes/META-INF/orm.xml");
      war.addWebResource(new ByteArrayAsset(new byte[0]), "beans.xml");
      return war;
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @Inject
   EntityManagerFactory emf;

   @Test
   public void testInjectionIntoEntity() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      EntityManager em = null;
      try
      {
         em = emf.createEntityManager();
         transaction.begin();
         em.joinTransaction();
         Hotel h = new Hotel("Hilton", "Fake St", "Wollongong", "NSW", "2518", "Australia");
         em.persist(h);
         em.flush();
         transaction.commit();
         em.close();
         transaction.begin();
         em = emf.createEntityManager();
         em.joinTransaction();

         h = (Hotel) em.createQuery("select h from Hotel h where h.name='Hilton'").getSingleResult();
         Assert.assertTrue(h.isInitalizerCalled());
         Assert.assertEquals(h.sayHello(), "Hello");
      }
      finally
      {
         em.close();
         transaction.rollback();
      }

   }

}
