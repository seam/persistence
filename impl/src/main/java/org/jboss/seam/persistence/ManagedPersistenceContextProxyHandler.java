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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.persistence.transaction.literal.DefaultTransactionLiteral;
import org.jboss.seam.persistence.util.InstanceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy handler for the seam managed persistence context. This handler makes
 * sure that the EntityManager is enrolled in the current transaction before
 * passing the call through to the delegate
 * 
 * @author Stuart Douglas
 * 
 */
public class ManagedPersistenceContextProxyHandler extends PersistenceContextProxyHandler implements InvocationHandler, Serializable, Synchronization
{

   private static final long serialVersionUID = -6539267789786229774L;

   private final EntityManager delegate;

   private final Instance<SeamTransaction> userTransactionInstance;

   private final Set<Annotation> qualifiers;

   private transient boolean synchronizationRegistered;

   private final PersistenceContexts persistenceContexts;

   private boolean persistenceContextsTouched = false;

   static final Logger log = LoggerFactory.getLogger(ManagedPersistenceContextProxyHandler.class);

   public ManagedPersistenceContextProxyHandler(EntityManager delegate, BeanManager beanManager, Set<Annotation> qualifiers, PersistenceContexts persistenceContexts)
   {
      super(delegate, beanManager, qualifiers);
      this.delegate = delegate;
      this.userTransactionInstance = InstanceResolver.getInstance(SeamTransaction.class, beanManager, DefaultTransactionLiteral.INSTANCE);
      this.qualifiers = new HashSet<Annotation>(qualifiers);
      this.persistenceContexts = persistenceContexts;
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if (!synchronizationRegistered)
      {
         joinTransaction();
      }
      touch((PersistenceContext) proxy);
      return super.invoke(proxy, method, args);
   }

   private void joinTransaction() throws SystemException
   {
      SeamTransaction transaction = userTransactionInstance.get();
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

   void touch(PersistenceContext delegate)
   {
      if (!persistenceContextsTouched)
      {
         try
         {
            persistenceContexts.touch(delegate);
            persistenceContextsTouched = true;
         }
         catch (ContextNotActiveException e)
         {
            log.debug("Not touching pc " + this + "as conversation scope not active");
         }
      }
   }

   public void afterCompletion(int status)
   {
      synchronizationRegistered = false;
   }

   public void beforeCompletion()
   {

   }

}
