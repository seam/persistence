package org.jboss.seam.persistence;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.transaction.UserTransaction;
import org.jboss.weld.extensions.beanManager.BeanManagerAccessor;
import org.jboss.weld.extensions.literal.DefaultLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ManagedPersistenceContextProxyHandler implements InvocationHandler, Serializable, Synchronization
{

   private final EntityManager delegate;

   private transient BeanManager beanManager;

   private transient UserTransaction userTransaction;

   private transient boolean synchronizationRegistered;

   static final Logger log = LoggerFactory.getLogger(ManagedPersistenceContextProxyHandler.class);

   public ManagedPersistenceContextProxyHandler(EntityManager delegate, BeanManager beanManager)
   {
      this.delegate = delegate;
      this.beanManager = beanManager;
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if (!synchronizationRegistered)
      {
         joinTransaction();
      }
      return method.invoke(delegate, args);
   }

   private void joinTransaction() throws SystemException
   {
      UserTransaction transaction = getUserTransaction();
      if (transaction.isActive())
      {
         transaction.enlist(delegate);
         try
         {
            transaction.registerSynchronization(this);
            synchronizationRegistered = true;
         }
         catch (Exception e)
         {
            // synchronizationRegistered =
            // PersistenceProvider.instance().registerSynchronization(this,
            // entityManager);
            throw new RuntimeException(e);
         }
      }
   }

   private BeanManager getBeanManager()
   {
      if (beanManager == null)
      {
         beanManager = BeanManagerAccessor.getManager();
      }
      return beanManager;
   }

   private UserTransaction getUserTransaction()
   {
      if (userTransaction == null)
      {
         Set<Bean<?>> beans = beanManager.getBeans(UserTransaction.class, DefaultLiteral.INSTANCE);
         if (beans.size() == 0)
         {
            throw new RuntimeException("No bean with class" + UserTransaction.class.getName() + " and qualifiers Default found");
         }
         else if (beans.size() != 1)
         {
            throw new RuntimeException("More than 1 bean with class" + UserTransaction.class.getName() + " and qualifiers Default found");
         }
         Bean<UserTransaction> userTransactionBean = (Bean<UserTransaction>) beans.iterator().next();
         CreationalContext<UserTransaction> ctx = beanManager.createCreationalContext(userTransactionBean);
         userTransaction = (UserTransaction) beanManager.getReference(userTransactionBean, UserTransaction.class, ctx);
      }
      return userTransaction;
   }

   public void afterCompletion(int status)
   {
      synchronizationRegistered = false;
   }

   public void beforeCompletion()
   {
      // TODO Auto-generated method stub

   }

}
