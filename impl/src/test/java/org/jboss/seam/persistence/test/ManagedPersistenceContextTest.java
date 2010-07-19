package org.jboss.seam.persistence.test;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.PersistenceContextExtension;
import org.jboss.seam.persistence.transaction.Transaction;
import org.jboss.seam.persistence.transaction.UserTransaction;
import org.jboss.seam.transactions.test.util.ArtifactNames;
import org.jboss.seam.transactions.test.util.EntityManagerProvider;
import org.jboss.seam.transactions.test.util.Hotel;
import org.jboss.seam.transactions.test.util.MavenArtifactResolver;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ManagedPersistenceContextTest
{
   @Deployment
   public static Archive<?> createTestArchive()
   {
      WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.SEAM_PERSISTENCE_API));
      war.addPackage(Transaction.class.getPackage());
      war.addPackage(PersistenceContextExtension.class.getPackage());
      war.addClasses(ManagedPersistenceContextTest.class, Hotel.class, EntityManagerProvider.class);
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(new byte[0]), "beans.xml");
      war.addWebResource("META-INF/services/javax.enterprise.inject.spi.Extension", "classes/META-INF/services/javax.enterprise.inject.spi.Extension");
      return war;
   }

   @Inject
   UserTransaction transaction;

   @Inject
   EntityManager em;

   @Test
   public void testManagedPsersistenceContext() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      transaction.begin();
      Hotel h = new Hotel("test", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.commit();

      transaction.begin();
      h = new Hotel("test2", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.rollback();

      transaction.begin();
      List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
      Assert.assertTrue(hotels.size() == 1);
      transaction.rollback();
   }

}
