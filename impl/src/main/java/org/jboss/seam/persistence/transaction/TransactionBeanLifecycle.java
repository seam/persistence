/**
 * 
 */
package org.jboss.seam.persistence.transaction;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.jboss.weld.extensions.bean.BeanLifecycle;

/**
 * BeanLifecycle for the default SeamTransaction bean
 * 
 * @author Stuart Douglas
 * 
 */
class TransactionBeanLifecycle implements BeanLifecycle<SeamTransaction>
{
   /**
    * proxy class for SeamTransaction
    */
   private static final Class proxy = Proxy.getProxyClass(SeamTransaction.class.getClassLoader(), SeamTransaction.class);

   private static final Constructor<SeamTransaction> proxyConstructor;

   static
   {
      try
      {
         proxyConstructor = proxy.getConstructor(InvocationHandler.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   private final BeanManager manager;

   public TransactionBeanLifecycle(BeanManager manager)
   {
      this.manager = manager;
   }

   public SeamTransaction create(Bean<SeamTransaction> bean, CreationalContext<SeamTransaction> ctx)
   {
      try
      {
         return proxyConstructor.newInstance(new TransactionInvocationHandler(manager));
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public void destroy(Bean<SeamTransaction> bean, SeamTransaction arg0, CreationalContext<SeamTransaction> arg1)
   {
      arg1.release();
   }
}