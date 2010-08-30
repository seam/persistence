package org.jboss.seam.persistence.transactions.test;

import javax.inject.Inject;
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
import org.jboss.seam.persistence.DefaultPersistenceProvider;
import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.persistence.transaction.TransactionExtension;
import org.jboss.seam.persistence.transaction.scope.TransactionScopeExtension;
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
public class TransactionScopedTest
{
   @Deployment
   public static Archive<?> createTestArchive()
   {
      WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.SEAM_PERSISTENCE_API));
      war.addPackage(TransactionExtension.class.getPackage());
      war.addPackage(TransactionScopeExtension.class.getPackage());
      war.addPackage(DefaultPersistenceProvider.class.getPackage());
      war.addPackage(NamingUtils.class.getPackage());
      war.addClasses(TransactionScopedTest.class, Hotel.class, HelloService.class, TransactionScopedObject.class);
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(new byte[0]), "beans.xml");
      war.addWebResource("META-INF/services/javax.enterprise.inject.spi.Extension", "classes/META-INF/services/javax.enterprise.inject.spi.Extension");
      return war;
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @PersistenceUnit
   EntityManagerFactory emf;

   @Inject
   TransactionScopedObject transactionScopedObject;

   @Test
   public void transactionScopeTest() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      transaction.begin();
      transactionScopedObject.setValue(10);
      Assert.assertTrue(transactionScopedObject.getValue() == 10);
      transaction.commit();

      transaction.begin();
      Assert.assertTrue(transactionScopedObject.getValue() == 0);
      transactionScopedObject.setValue(20);
      Assert.assertTrue(transactionScopedObject.getValue() == 20);
      transaction.rollback();

      transaction.begin();
      Assert.assertTrue(transactionScopedObject.getValue() == 0);
      transaction.rollback();
   }

}
