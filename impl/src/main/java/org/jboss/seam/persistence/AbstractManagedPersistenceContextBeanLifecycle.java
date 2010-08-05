/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.seam.persistence;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.weld.extensions.bean.BeanLifecycle;

/**
 * Class that is responsible for creating and destroying the seam managed
 * persistence context
 * 
 * @author Stuart Douglas
 * 
 */
public abstract class AbstractManagedPersistenceContextBeanLifecycle implements BeanLifecycle<EntityManager>
{

   static final Class<?> proxyClass = Proxy.getProxyClass(PersistenceContext.class.getClassLoader(), EntityManager.class, Serializable.class, PersistenceContext.class);

   static final Constructor<?> proxyConstructor;

   private final BeanManager manager;

   protected AbstractManagedPersistenceContextBeanLifecycle(BeanManager manager)
   {
      this.manager = manager;
   }

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

   /**
    * creates the proxy
    */
   public EntityManager create(Bean<EntityManager> bean, CreationalContext<EntityManager> arg0)
   {
      try
      {
         EntityManagerFactory emf = getEntityManagerFactory();
         EntityManager entityManager = emf.createEntityManager();
         ManagedPersistenceContextProxyHandler handler = new ManagedPersistenceContextProxyHandler(entityManager, manager, bean.getQualifiers());
         EntityManager proxy = (EntityManager) proxyConstructor.newInstance(handler);
         return proxy;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public void destroy(Bean<EntityManager> bean, EntityManager em, CreationalContext<EntityManager> arg1)
   {
      em.close();
      arg1.release();
   }

   protected abstract EntityManagerFactory getEntityManagerFactory();

}
