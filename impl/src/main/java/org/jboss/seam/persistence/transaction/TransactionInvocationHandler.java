package org.jboss.seam.persistence.transaction;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.seam.persistence.util.NamingUtils;
import org.jboss.weld.extensions.literal.DefaultLiteral;

/**
 * Invocation handler for the default SeamTransaction proxy
 * 
 * Supports injection of a Seam UserTransaction object that wraps the current
 * JTA transaction or EJB container managed transaction.
 * 
 * @author Stuart Douglas
 * 
 */
public class TransactionInvocationHandler implements InvocationHandler
{

   private final Synchronizations synchronizations;

   public TransactionInvocationHandler(BeanManager manager)
   {
      Bean<Synchronizations> bean = (Bean) manager.resolve(manager.getBeans(Synchronizations.class, DefaultLiteral.INSTANCE));
      CreationalContext<Synchronizations> ctx = manager.createCreationalContext(bean);
      synchronizations = (Synchronizations) manager.getReference(bean, Synchronizations.class, ctx);
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      SeamTransaction instance;
      try
      {
         instance = createUTTransaction();
      }
      catch (NameNotFoundException nnfe)
      {
         try
         {
            instance = createCMTTransaction();
         }
         catch (NameNotFoundException nnfe2)
         {
            instance = createNoTransaction();
         }
      }
      return method.invoke(instance, args);
   }

   protected SeamTransaction createNoTransaction()
   {
      return new NoTransaction();
   }

   protected SeamTransaction createCMTTransaction() throws NamingException
   {
      return new CMTTransaction(EJB.getEJBContext(), synchronizations);
   }

   protected SeamTransaction createUTTransaction() throws NamingException
   {
      return new UTTransaction(getUserTransaction(), synchronizations);
   }

   protected javax.transaction.UserTransaction getUserTransaction() throws NamingException
   {
      InitialContext context = NamingUtils.getInitialContext();
      try
      {
         return (javax.transaction.UserTransaction) context.lookup("java:comp/UserTransaction");
      }
      catch (NameNotFoundException nnfe)
      {
         try
         {
            // Embedded JBoss has no java:comp/UserTransaction
            javax.transaction.UserTransaction ut = (javax.transaction.UserTransaction) context.lookup("UserTransaction");
            ut.getStatus(); // for glassfish, which can return an unusable UT
            return ut;
         }
         catch (Exception e)
         {
            throw nnfe;
         }
      }
   }
}
