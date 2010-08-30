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
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.PersistenceContextExtension;
import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.persistence.transaction.TransactionExtension;
import org.jboss.seam.persistence.transaction.scope.TransactionScopeExtension;
import org.jboss.seam.persistence.util.NamingUtils;
import org.jboss.seam.transactions.test.util.ArtifactNames;
import org.jboss.seam.transactions.test.util.HelloService;
import org.jboss.seam.transactions.test.util.IndexedHotel;
import org.jboss.seam.transactions.test.util.ManagedPersistenceContextProvider;
import org.jboss.seam.transactions.test.util.MavenArtifactResolver;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class HibernateSearchTest
{
   @Deployment
   public static Archive<?> createTestArchive()
   {
      WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.SEAM_PERSISTENCE_API));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.HIBERNATE_SEARCH));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.LUCENE_ANALYZERS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.LUCENE_CORE));
      war.addPackage(TransactionExtension.class.getPackage());
      war.addPackage(PersistenceContextExtension.class.getPackage());
      war.addPackage(TransactionScopeExtension.class.getPackage());
      war.addPackage(NamingUtils.class.getPackage());
      war.addClasses(HibernateSearchTest.class, IndexedHotel.class, ManagedPersistenceContextProvider.class, HelloService.class);
      war.addWebResource("META-INF/persistence-search.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(new byte[0]), "beans.xml");
      war.addWebResource("META-INF/services/javax.enterprise.inject.spi.Extension", "classes/META-INF/services/javax.enterprise.inject.spi.Extension");
      return war;
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @Inject
   FullTextEntityManager em;

   @Test
   public void testFullTextEntityManager() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      Assert.assertTrue(em instanceof FullTextEntityManager);
   }

   @Test
   public void testSearchingForHotel() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, ParseException
   {
      transaction.begin();
      IndexedHotel h = new IndexedHotel("Hilton", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.commit();

      transaction.begin();
      h = new IndexedHotel("Other Hotel", "Real St ", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.commit();

      transaction.begin();
      String[] fields = new String[] { "name" };
      MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, new StandardAnalyzer());
      org.apache.lucene.search.Query query = parser.parse("Other");

      // wrap Lucene query in a javax.persistence.Query
      javax.persistence.Query persistenceQuery = em.createFullTextQuery(query, IndexedHotel.class);
      IndexedHotel hotel = (IndexedHotel) persistenceQuery.getSingleResult();
      Assert.assertTrue(hotel.getName().equals("Other Hotel"));
      transaction.commit();

   }
}
