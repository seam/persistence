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
package org.jboss.seam.persistence.transaction.scope;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.transaction.TransactionScoped;
import org.jboss.seam.persistence.transaction.UserTransaction;
import org.jboss.weld.extensions.literal.DefaultLiteral;

/**
 * Context for the {@link TransactionScoped} scope
 * 
 * @author stuart
 * 
 */
public class TransactionScopeContext implements Context, Synchronization
{

   private UserTransaction userTransaction;

   private final BeanManager beanManager;

   private final ContextualIdentifierStore identifierStore = new ContextualIdentifierStore();

   public TransactionScopeContext(BeanManager beanManager)
   {
      this.beanManager = beanManager;

   }

   private void lazyInitialization()
   {
      if (userTransaction == null)
      {
         synchronized (this)
         {
            if (userTransaction == null)
            {
               Set<Bean<?>> beans = beanManager.getBeans(UserTransaction.class, new DefaultLiteral());
               Bean<UserTransaction> userTransactionBean = (Bean<UserTransaction>) beans.iterator().next();
               CreationalContext<?> ctx = beanManager.createCreationalContext(userTransactionBean);
               userTransaction = (UserTransaction) beanManager.getReference(userTransactionBean, UserTransaction.class, ctx);
               userTransaction.registerSynchronization(this);
            }
         }
      }
   }

   private final ThreadLocal<Map<String, Object>> instanceStore = new ThreadLocal<Map<String, Object>>()
   {
      protected Map<String, Object> initialValue()
      {
         return new ConcurrentHashMap<String, Object>();
      };
   };

   private final ThreadLocal<Map<String, CreationalContext<?>>> creationalContextStore = new ThreadLocal<Map<String, CreationalContext<?>>>()
   {
      protected Map<String, CreationalContext<?>> initialValue()
      {
         return new ConcurrentHashMap<String, CreationalContext<?>>();
      };
   };

   public <T> T get(Contextual<T> contextual)
   {
      lazyInitialization();
      String id = identifierStore.getId(contextual);
      Map<String, Object> map = instanceStore.get();
      return (T) map.get(id);
   }

   public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext)
   {
      lazyInitialization();
      String id = identifierStore.getId(contextual);
      Map<String, Object> map = instanceStore.get();
      T instance = (T) map.get(id);
      if (instance == null)
      {
         instance = contextual.create(creationalContext);
         creationalContextStore.get().put(id, creationalContext);
         map.put(id, instance);
      }
      return instance;
   }

   public Class<? extends Annotation> getScope()
   {
      return TransactionScoped.class;
   }

   public boolean isActive()
   {
      lazyInitialization();
      try
      {
         return userTransaction.isActive();
      }
      catch (SystemException e)
      {
         throw new RuntimeException(e);
      }
   }

   public void afterCompletion(int status)
   {
      Map<String, Object> map = instanceStore.get();
      Map<String, CreationalContext<?>> creationalContexts = creationalContextStore.get();
      for (Entry<String, Object> e : map.entrySet())
      {
         Contextual contextual = identifierStore.getContextual(e.getKey());
         CreationalContext<?> ctx = creationalContexts.get(e.getKey());
         contextual.destroy(e.getValue(), ctx);
         ctx.release();
      }
      instanceStore.remove();
      creationalContextStore.remove();
   }

   public void beforeCompletion()
   {

   }

}
