package org.jboss.seam.persistence;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.Producer;
import javax.persistence.EntityManager;

/**
 * implementation of Producer that proxies a produced EntityManager
 * 
 * @author stuart
 * 
 */
public class ManagedPersistenceContextProducer implements Producer<EntityManager>
{
   static final Class<?> proxyClass = Proxy.getProxyClass(EntityManager.class.getClassLoader(), EntityManager.class, Serializable.class);

   static final Constructor<?> proxyConstructor;

   static
   {
      try
      {
         proxyConstructor = proxyClass.getConstructor(InvocationHandler.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   private final Producer<EntityManager> delegate;

   private final BeanManager beanManager;

   public ManagedPersistenceContextProducer(Producer<EntityManager> delegate, BeanManager beanManager)
   {
      this.delegate = delegate;
      this.beanManager = beanManager;
   }

   public void dispose(EntityManager instance)
   {
      delegate.dispose(instance);
   }

   public Set<InjectionPoint> getInjectionPoints()
   {
      return delegate.getInjectionPoints();
   }

   public EntityManager produce(CreationalContext<EntityManager> ctx)
   {
      try
      {
         EntityManager entityManager = delegate.produce(ctx);
         ManagedPersistenceContextProxyHandler handler = new ManagedPersistenceContextProxyHandler(entityManager, beanManager);
         EntityManager proxy = (EntityManager) proxyConstructor.newInstance(handler);
         return proxy;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }

   }

}
