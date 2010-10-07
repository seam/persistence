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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.weld.extensions.bean.ContextualLifecycle;
import org.jboss.weld.extensions.literal.DefaultLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SMPC lifecycle for SMPC's configured via @SeamManaged
 * 
 * @author Stuart Douglas
 * 
 */
public class ManagedPersistenceContextBeanLifecycle implements ContextualLifecycle<EntityManager>
{
   private static final Logger log = LoggerFactory.getLogger(ManagedPersistenceContextBeanLifecycle.class);

   private final Class<?> proxyClass;

   private final Constructor<?> proxyConstructor;

   private SeamPersistenceProvider persistenceProvider;

   private PersistenceContexts persistenceContexts;

   protected final Annotation[] qualifiers;

   protected final BeanManager manager;

   private EntityManagerFactory emf;

   private final List<SeamPersistenceProvider> persistenceProviders;

   public ManagedPersistenceContextBeanLifecycle(Set<Annotation> qualifiers, ClassLoader loader, BeanManager manager, Set<Class<?>> additionalinterfaces, List<SeamPersistenceProvider> persistenceProviders)
   {
      this.manager = manager;
      this.persistenceProviders = new ArrayList<SeamPersistenceProvider>(persistenceProviders);
      Class<?>[] interfaces = new Class[additionalinterfaces.size() + 3];
      int count = 0;
      for (Class<?> i : additionalinterfaces)
      {
         interfaces[count++] = i;
      }

      interfaces[count++] = EntityManager.class;
      interfaces[count++] = Serializable.class;
      interfaces[count++] = ManagedPersistenceContext.class;
      proxyClass = Proxy.getProxyClass(loader, interfaces);
      try
      {
         proxyConstructor = proxyClass.getConstructor(InvocationHandler.class);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
      this.qualifiers = new Annotation[qualifiers.size()];
      int i = 0;
      for (Annotation a : qualifiers)
      {
         this.qualifiers[i++] = a;
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
         entityManager = getPersistenceProvider(entityManager).proxyEntityManager(entityManager);
         PersistenceContexts persistenceContexts = null;
         try
         {
            persistenceContexts = getPersistenceContexts();
         }
         catch (ContextNotActiveException e)
         {
            // it's null already
         }
         ManagedPersistenceContextProxyHandler handler = new ManagedPersistenceContextProxyHandler(entityManager, manager, bean.getQualifiers(), persistenceContexts, getPersistenceProvider(entityManager));
         EntityManager proxy = (EntityManager) proxyConstructor.newInstance(handler);
         arg0.push(proxy);
         getPersistenceProvider(entityManager).setFlushMode(proxy, getFlushMode());
         manager.fireEvent(new SeamManagedPersistenceContextCreated(entityManager), qualifiers);

         return proxy;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   private FlushModeType getFlushMode()
   {
      try
      {
         return getPersistenceContexts().getFlushMode();
      }
      catch (ContextNotActiveException e)
      {
         // TODO Set the default flush mode for the app
         return FlushModeType.AUTO;
      }
   }

   public void destroy(Bean<EntityManager> bean, EntityManager em, CreationalContext<EntityManager> arg1)
   {
      ((ManagedPersistenceContext) em).closeAfterTransaction();
      arg1.release();
      try
      {
         getPersistenceContexts().untouch((ManagedPersistenceContext) em);
      }
      catch (ContextNotActiveException e)
      {
         log.debug("Could not untouch PersistenceContext as conversation scope not active");
      }
   }

   private PersistenceContexts getPersistenceContexts()
   {
      if (persistenceContexts == null)
      {
         Bean<PersistenceContexts> bean = (Bean) manager.resolve(manager.getBeans(PersistenceContexts.class, DefaultLiteral.INSTANCE));
         if (bean == null)
         {
            throw new RuntimeException("Could not find PersistenceContexts bean");
         }
         CreationalContext<PersistenceContexts> ctx = manager.createCreationalContext(bean);
         persistenceContexts = (PersistenceContexts) manager.getReference(bean, PersistenceContexts.class, ctx);
      }
      return persistenceContexts;
   }

   private SeamPersistenceProvider getPersistenceProvider(EntityManager em)
   {
      if (persistenceProvider == null)
      {
         for (SeamPersistenceProvider i : persistenceProviders)
         {
            if (i.isCorrectProvider(em))
            {
               persistenceProvider = i;
               break;
            }
         }
      }
      return persistenceProvider;
   }

   /**
    * lazily resolve the relevant EMF
    */
   protected EntityManagerFactory getEntityManagerFactory()
   {
      if (emf == null)
      {
         Bean<EntityManagerFactory> bean = (Bean) manager.resolve(manager.getBeans(EntityManagerFactory.class, qualifiers));
         if (bean == null)
         {
            throw new RuntimeException("Could not find EntityManagerFactory bean with qualifiers" + Arrays.toString(qualifiers));
         }
         CreationalContext<EntityManagerFactory> ctx = manager.createCreationalContext(bean);
         emf = (EntityManagerFactory) manager.getReference(bean, EntityManagerFactory.class, ctx);
      }
      return emf;
   }

}
