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
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.weld.extensions.bean.BeanImpl;
import org.jboss.weld.extensions.bean.BeanLifecycle;
import org.jboss.weld.extensions.util.BeanResolutionException;
import org.jboss.weld.extensions.util.BeanResolver;

/**
 * Class that is responsible for creating and destroying the seam managed
 * persistence context
 * 
 * @author Stuart Douglas
 * 
 */
public class ManagedPersistenceContextBeanLifecycle implements BeanLifecycle<EntityManager>
{

   private EntityManagerFactory emf;
   private final Annotation[] qualifiers;
   private final BeanManager manager;

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

   public ManagedPersistenceContextBeanLifecycle(Set<Annotation> qualifiers, BeanManager manager)
   {
      this.qualifiers = new Annotation[qualifiers.size()];
      int i = 0;
      for (Annotation a : qualifiers)
      {
         this.qualifiers[i++] = a;
      }
      this.manager = manager;
   }

   /**
    * creates the proxy
    */
   public EntityManager create(BeanImpl<EntityManager> bean, CreationalContext<EntityManager> arg0)
   {
      try
      {
         EntityManagerFactory emf = getEntityManagerFactory();
         EntityManager entityManager = emf.createEntityManager();
         ManagedPersistenceContextProxyHandler handler = new ManagedPersistenceContextProxyHandler(entityManager, manager);
         EntityManager proxy = (EntityManager) proxyConstructor.newInstance(handler);
         return proxy;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public void destroy(BeanImpl<EntityManager> bean, EntityManager em, CreationalContext<EntityManager> arg1)
   {
      em.close();
      arg1.release();
   }

   /**
    * lazily resolve the relevant EMF
    */
   private EntityManagerFactory getEntityManagerFactory()
   {
      if (emf == null)
      {
         try
         {
            emf = BeanResolver.getReference(EntityManagerFactory.class, manager, qualifiers);
         }
         catch (BeanResolutionException e)
         {
            throw new RuntimeException(e);
         }
      }
      return emf;
   }
}
