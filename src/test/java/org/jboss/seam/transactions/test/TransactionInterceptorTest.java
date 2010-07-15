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
import org.jboss.seam.transaction.Transaction;
import org.jboss.seam.transaction.TransactionInterceptor;
import org.jboss.seam.transaction.UserTransaction;
import org.jboss.seam.transactions.test.util.ArtifactNames;
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
      war.addClasses(TransactionInterceptorTest.class, TransactionManagedBean.class, Hotel.class, EntityManagerProvider.class);
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(("<beans><interceptors><class>" + TransactionInterceptor.class.getName() + "</class></interceptors></beans>").getBytes()), "beans.xml");
      
      return war;
   }

   @Inject
   TransactionManagedBean bean;

   @Inject
   UserTransaction transaction;

   @PersistenceContext
   EntityManager em;

   @Test
   public void testTransactionInterceptor() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {

      bean.addHotel();

      try
      {
         bean.failToAddHotel();
      }
      catch (Exception e)
      {

      }

      transaction.begin();
      em.joinTransaction();
      List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
      Assert.assertTrue("Wrong number of hotels: " + hotels.size(), hotels.size() == 1);
      transaction.rollback();

   }
}
