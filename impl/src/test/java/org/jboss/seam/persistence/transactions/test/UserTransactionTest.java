package org.jboss.seam.persistence.transactions.test;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
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
import org.jboss.seam.persistence.util.NamingUtils;
import org.jboss.seam.transactions.test.util.ArtifactNames;
import org.jboss.seam.transactions.test.util.HelloService;
import org.jboss.seam.transactions.test.util.Hotel;
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
      WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.SEAM_PERSISTENCE_API));
      war.addPackage(TransactionExtension.class.getPackage());
      war.addPackage(PersistenceException.class.getPackage());
      war.addClasses(UserTransactionTest.class, Hotel.class, HelloService.class);
      war.addClass(SeamPersistenceProvider.class);
      war.addPackage(NamingUtils.class.getPackage());
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(new byte[0]), "beans.xml");
      war.addWebResource("META-INF/services/javax.enterprise.inject.spi.Extension", "classes/META-INF/services/javax.enterprise.inject.spi.Extension");
      return war;
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @PersistenceContext
   EntityManager em;

   @Test
   public void userTransactionTest() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      transaction.begin();
      em.joinTransaction();
      Hotel h = new Hotel("test", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.commit();
      em.clear();

      transaction.begin();
      em.joinTransaction();
      h = new Hotel("test2", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.rollback();
      em.clear();

      transaction.begin();
      em.joinTransaction();
      List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
      Assert.assertTrue(hotels.size() == 1);
      transaction.rollback();
      em.clear();

   }

}
