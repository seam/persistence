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
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.weld.extensions.bean.BeanImpl;
import org.jboss.weld.extensions.bean.BeanLifecycle;

public class ManagedPersistenceContextBeanLifecycle implements BeanLifecycle<EntityManager>
{

   private EntityManagerFactory emf;
   private final Annotation[] qualifiers;
   private final BeanManager manager;

   static final Class<?> proxyClass = Proxy.getProxyClass(EntityManager.class.getClassLoader(), EntityManager.class, Serializable.class);

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

   private EntityManagerFactory getEntityManagerFactory()
   {
      if (emf == null)
      {
         Set<Bean<?>> beans = manager.getBeans(EntityManagerFactory.class, qualifiers);
         if (beans.size() == 0)
         {
            throw new RuntimeException("No bean found with type EntityManagerFactory and qualifiers " + qualifiers);
         }
         if (beans.size() != 1)
         {
            throw new RuntimeException("More than 1 bean found with type EntityManagerFactory and qualifiers " + qualifiers);
         }
         Bean<?> emfBean = beans.iterator().next();
         CreationalContext<?> emfCreationalContext = manager.createCreationalContext(emfBean);
         emf = (EntityManagerFactory) manager.getReference(emfBean, EntityManagerFactory.class, emfCreationalContext);
      }
      return emf;
   }

}
