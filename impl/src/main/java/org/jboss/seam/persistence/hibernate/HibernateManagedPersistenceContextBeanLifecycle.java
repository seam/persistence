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
package org.jboss.seam.persistence.hibernate;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.seam.persistence.HibernatePersistenceProvider;
import org.jboss.seam.persistence.ManagedPersistenceContext;
import org.jboss.seam.persistence.PersistenceContexts;
import org.jboss.weld.extensions.bean.BeanLifecycle;
import org.jboss.weld.extensions.literal.DefaultLiteral;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * lifecycle for seam managed hibernate sessions
 * 
 * @author Stuart Douglas
 * 
 */
public class HibernateManagedPersistenceContextBeanLifecycle implements BeanLifecycle<Session>
{
   private static final Logger log = LoggerFactory.getLogger(HibernateManagedPersistenceContextBeanLifecycle.class);

   private final Class<?> proxyClass;

   private final Constructor<?> proxyConstructor;

   private HibernatePersistenceProvider persistenceProvider = new HibernatePersistenceProvider();

   private PersistenceContexts persistenceContexts;

   protected final Annotation[] qualifiers;

   protected final BeanManager manager;

   private SessionFactory sessionFactory;

   public HibernateManagedPersistenceContextBeanLifecycle(Set<Annotation> qualifiers, ClassLoader loader, BeanManager manager)
   {
      this.manager = manager;
      Set<Class<?>> additionalinterfaces = persistenceProvider.getAdditionalSessionInterfaces();
      Class<?>[] interfaces = new Class[additionalinterfaces.size() + 3];
      int count = 0;
      for (Class<?> i : additionalinterfaces)
      {
         interfaces[count++] = i;
      }

      interfaces[count++] = Session.class;
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
   public Session create(Bean<Session> bean, CreationalContext<Session> arg0)
   {
      try
      {
         SessionFactory sf = getSessionFactory();
         Session session = sf.openSession();
         session = (Session) persistenceProvider.proxyDelegate(session);
         HibernateManagedPersistenceContextProxyHandler handler = new HibernateManagedPersistenceContextProxyHandler(session, manager, bean.getQualifiers(), getPersistenceContexts(), persistenceProvider);
         Session proxy = (Session) proxyConstructor.newInstance(handler);
         ((ManagedPersistenceContext) proxy).changeFlushMode(getPersistenceContexts().getFlushMode());
         manager.fireEvent(new SeamManagedHibernateSessionCreated(proxy), qualifiers);

         return proxy;
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public void destroy(Bean<Session> bean, Session session, CreationalContext<Session> arg1)
   {
      ((ManagedPersistenceContext) session).closeAfterTransaction();
      arg1.release();
      try
      {
         getPersistenceContexts().untouch((ManagedPersistenceContext) session);
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

   /**
    * lazily resolve the relevant EMF
    */
   protected SessionFactory getSessionFactory()
   {
      if (sessionFactory == null)
      {
         Bean<SessionFactory> bean = (Bean) manager.resolve(manager.getBeans(SessionFactory.class, qualifiers));
         if (bean == null)
         {
            throw new RuntimeException("Could not find SessionFactory bean with qualifiers" + qualifiers);
         }
         CreationalContext<SessionFactory> ctx = manager.createCreationalContext(bean);
         sessionFactory = (SessionFactory) manager.getReference(bean, SessionFactory.class, ctx);
      }
      return sessionFactory;
   }

}
