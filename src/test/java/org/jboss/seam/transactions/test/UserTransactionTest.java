package org.jboss.seam.transactions.test;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.transaction.Transaction;
import org.jboss.seam.transaction.UserTransaction;
import org.jboss.seam.transactions.test.util.ArtifactNames;
import org.jboss.seam.transactions.test.util.MavenArtifactResolver;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class UserTransactionTest
{
   @Deployment
   public static Archive<?> createTestArchive()
   {

      WebArchive war = ShrinkWrap.create("test.war", WebArchive.class);
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addPackage(Transaction.class.getPackage()).addClasses(UserTransactionTest.class, Hotel.class);
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(new byte[0]), "beans.xml");
      
      return war;
   }

   @Inject
   UserTransaction transaction;

   @PersistenceUnit
   EntityManagerFactory emf;

   @Test
   public void userTransactionTest() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      transaction.begin();
      EntityManager em = emf.createEntityManager();
      em.joinTransaction();
      Hotel h = new Hotel("test", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.commit();

      transaction.begin();
      em = emf.createEntityManager();
      em.joinTransaction();
      h = new Hotel("test2", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.rollback();

      transaction.begin();
      em = emf.createEntityManager();
      em.joinTransaction();
      List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
      Assert.assertTrue(hotels.size() == 1);
      transaction.rollback();
   }

}
