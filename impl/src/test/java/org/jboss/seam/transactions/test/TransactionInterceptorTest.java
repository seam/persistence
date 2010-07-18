package org.jboss.seam.transactions.test;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.transaction.UserTransaction;
import org.jboss.seam.transaction.Transaction;
import org.jboss.seam.transaction.TransactionInterceptor;
import org.jboss.seam.transactions.test.util.ArtifactNames;
import org.jboss.seam.transactions.test.util.DontRollBackException;
import org.jboss.seam.transactions.test.util.EntityManagerProvider;
import org.jboss.seam.transactions.test.util.MavenArtifactResolver;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class TransactionInterceptorTest
{
   @Deployment
   public static Archive<?> createTestArchive()
   {

      WebArchive war = ShrinkWrap.create("test.war", WebArchive.class);
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addPackage(Transaction.class.getPackage());
      war.addClasses(TransactionInterceptorTest.class, TransactionManagedBean.class, Hotel.class, EntityManagerProvider.class, DontRollBackException.class, TransactionObserver.class);
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(("<beans><interceptors><class>" + TransactionInterceptor.class.getName() + "</class></interceptors></beans>").getBytes()), "beans.xml");
      war.addWebResource("META-INF/services/javax.enterprise.inject.spi.Extension", "classes/META-INF/services/javax.enterprise.inject.spi.Extension");
      return war;
   }

   @Inject
   TransactionManagedBean bean;

   @Inject
   UserTransaction transaction;

   @PersistenceContext
   EntityManager em;

   @Inject
   TransactionObserver observer;

   @Test
   public void testTransactionInterceptor() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      observer.setEnabled(true);
      try
      {
      observer.reset(true);
      bean.addHotel();
      assertHotels(1);
      observer.verify();
      observer.reset(false);
      try
      {
         bean.failToAddHotel();
      }
      catch (Exception e)
      {
      }
      assertHotels(1);
      observer.verify();
      observer.reset(true);
      try
      {
         bean.addHotelWithApplicationException();
      }
      catch (DontRollBackException e)
      {
      }
      assertHotels(2);
      observer.verify();
      }
      catch (Exception e)
      {
         observer.setEnabled(false);
         throw new RuntimeException(e);
      }

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
