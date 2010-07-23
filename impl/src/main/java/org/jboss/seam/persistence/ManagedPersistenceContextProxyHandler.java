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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.persistence.transaction.literal.DefaultTransactionLiteral;
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
public class ManagedPersistenceContextProxyHandler implements InvocationHandler, Serializable, Synchronization
{

   private static final long serialVersionUID = -6539267789786229774L;

   private final EntityManager delegate;

   private transient BeanManager beanManager;

   private transient SeamTransaction userTransaction;

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
      SeamTransaction transaction = getUserTransaction();
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

   private SeamTransaction getUserTransaction()
   {
      if (userTransaction == null)
      {
         Bean<SeamTransaction> bean = (Bean) beanManager.resolve(beanManager.getBeans(SeamTransaction.class, DefaultTransactionLiteral.INSTANCE));
         if (bean == null)
         {
            throw new RuntimeException("Could not find SeamTransaction bean with qualifier " + DefaultTransaction.class.getName());
         }
         CreationalContext<SeamTransaction> ctx = beanManager.createCreationalContext(bean);
         userTransaction = (SeamTransaction) beanManager.getReference(bean, SeamTransaction.class, ctx);
      }
      return userTransaction;
   }

   public void afterCompletion(int status)
   {
      synchronizationRegistered = false;
   }

   public void beforeCompletion()
   {

   }

}
