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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jboss.seam.persistence.FlushModeType;
import org.jboss.seam.persistence.HibernatePersistenceProvider;
import org.jboss.seam.persistence.ManagedPersistenceContext;
import org.jboss.seam.persistence.PersistenceContexts;
import org.jboss.seam.persistence.QueryParser;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.persistence.transaction.literal.DefaultTransactionLiteral;
import org.jboss.seam.persistence.util.InstanceResolver;
import org.jboss.weld.extensions.el.Expressions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy handler for the seam managed Hibernate session. This handler makes sure
 * that the EntityManager is enrolled in the current transaction before passing
 * the call through to the delegate
 * 
 * @author Stuart Douglas
 * 
 */
public class HibernateManagedSessionProxyHandler implements InvocationHandler, Serializable, Synchronization
{

   private static final long serialVersionUID = -6539267789786229774L;

   private final Session delegate;

   private final Instance<SeamTransaction> userTransactionInstance;

   private transient boolean synchronizationRegistered;

   private final PersistenceContexts persistenceContexts;

   private final Set<Annotation> qualifiers;

   private final HibernatePersistenceProvider provider;

   private boolean persistenceContextsTouched = false;

   private boolean closeOnTransactionCommit = false;

   static final Logger log = LoggerFactory.getLogger(HibernateManagedSessionProxyHandler.class);

   private final Instance<Expressions> expressionsInstance;

   public HibernateManagedSessionProxyHandler(Session delegate, BeanManager beanManager, Set<Annotation> qualifiers, PersistenceContexts persistenceContexts, HibernatePersistenceProvider provider)
   {
      this.qualifiers = qualifiers;
      this.provider = provider;
      this.delegate = delegate;
      this.userTransactionInstance = InstanceResolver.getInstance(SeamTransaction.class, beanManager, DefaultTransactionLiteral.INSTANCE);
      this.persistenceContexts = persistenceContexts;
      expressionsInstance = InstanceResolver.getInstance(Expressions.class, beanManager);
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      touch((ManagedPersistenceContext) proxy);
      if ("changeFlushMode".equals(method.getName()) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(FlushModeType.class))
      {
         changeFushMode((FlushModeType) args[0]);
         return null;
      }
      if ("getBeanType".equals(method.getName()) && method.getParameterTypes().length == 0)
      {
         return EntityManager.class;
      }
      if ("getQualifiers".equals(method.getName()) && method.getParameterTypes().length == 0)
      {
         return Collections.unmodifiableSet(qualifiers);
      }
      if ("getPersistenceProvider".equals(method.getName()) && method.getParameterTypes().length == 0)
      {
         return provider;
      }
      if ("closeAfterTransaction".equals(method.getName()) && method.getParameterTypes().length == 0)
      {
         closeAfterTransaction();
         return null;
      }
      if ("createQuery".equals(method.getName()) && method.getParameterTypes().length > 0 && method.getParameterTypes()[0].equals(String.class))
      {
         return handleCreateQueryWithString(method, args);
      }
      if (!"setFlushMode".equals(method.getName()) && !"getTransaction".equals(method.getName()))
      {
         if (!synchronizationRegistered)
         {
            joinTransaction();
         }
      }

      return method.invoke(delegate, args);
   }

   protected Object handleCreateQueryWithString(Method method, Object[] args) throws Throwable
   {
      if (args[0] == null)
      {
         return method.invoke(delegate, args);
      }
      String ejbql = (String) args[0];
      if (ejbql.indexOf('#') > 0)
      {
         QueryParser qp = new QueryParser(expressionsInstance.get(), ejbql);
         Object[] newArgs = args.clone();
         newArgs[0] = qp.getEjbql();
         Query query = (Query) method.invoke(delegate, newArgs);
         for (int i = 0; i < qp.getParameterValues().size(); i++)
         {
            query.setParameter(QueryParser.getParameterName(i), qp.getParameterValues().get(i));
         }
         return query;
      }
      else
      {
         return method.invoke(delegate, args);
      }
   }

   private void joinTransaction() throws SystemException
   {
      SeamTransaction transaction = userTransactionInstance.get();
      if (transaction.isActive())
      {
         delegate.isOpen();
         try
         {
            transaction.registerSynchronization(this);
            synchronizationRegistered = true;
         }
         catch (Exception e)
         {
            throw new RuntimeException(e);
         }
      }
   }

   private void closeAfterTransaction() throws SystemException
   {
      SeamTransaction transaction = userTransactionInstance.get();
      if (transaction.isActive())
      {
         closeOnTransactionCommit = true;
      }
      else
      {
         if (delegate.isOpen())
         {
            delegate.close();
         }
      }
   }

   private void changeFushMode(FlushModeType flushModeType)
   {
      switch (flushModeType)
      {
      case AUTO:
         delegate.setFlushMode(FlushMode.AUTO);
         break;
      case MANUAL:
         delegate.setFlushMode(FlushMode.MANUAL);
         break;
      case COMMIT:
         delegate.setFlushMode(FlushMode.COMMIT);
         break;
      default:
         throw new RuntimeException("Unkown flush mode: " + flushModeType);
      }
   }

   void touch(ManagedPersistenceContext delegate)
   {
      if (!persistenceContextsTouched)
      {
         try
         {
            // we need to do this first to prevent an infinite loop
            persistenceContextsTouched = true;
            persistenceContexts.touch(delegate);
         }
         catch (ContextNotActiveException e)
         {
            persistenceContextsTouched = false;
            log.debug("Not touching pc " + this + "as conversation scope not active");
         }
      }
   }

   public void afterCompletion(int status)
   {
      synchronizationRegistered = false;
      if (closeOnTransactionCommit && delegate.isOpen())
      {
         delegate.close();
      }
   }

   public void beforeCompletion()
   {

   }

}
