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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.persistence.transaction.TransactionScoped;
import org.jboss.seam.persistence.transaction.literal.DefaultTransactionLiteral;
import org.jboss.weld.extensions.util.BeanResolutionException;
import org.jboss.weld.extensions.util.BeanResolver;

/**
 * Context for the {@link TransactionScoped} scope
 * 
 * @author stuart
 * 
 */
public class TransactionScopeContext implements Context, Synchronization
{

   private SeamTransaction userTransaction;

   private final BeanManager beanManager;

   private final ContextualIdentifierStore identifierStore = new ContextualIdentifierStore();

   private final ThreadLocal<TransactionScopeData> contextData = new ThreadLocal<TransactionScopeData>()
   {
      protected TransactionScopeData initialValue()
      {
         return new TransactionScopeData();
      };
   };

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
               try
               {
                  userTransaction = BeanResolver.getReference(SeamTransaction.class, beanManager, DefaultTransactionLiteral.INSTANCE);
               }
               catch (BeanResolutionException e)
               {
                  throw new RuntimeException(e);
               }
            }
         }
      }
   }

   private void registerSyncronization()
   {
      TransactionScopeData data = contextData.get();
      if (!data.isSyncronisationRegistered())
      {
         userTransaction.registerSynchronization(this);
         data.setSyncronisationRegistered(true);
      }
   }

   public <T> T get(Contextual<T> contextual)
   {
      lazyInitialization();
      registerSyncronization();
      String id = identifierStore.getId(contextual);
      Map<String, Object> map = contextData.get().getInstanceStore();
      return (T) map.get(id);
   }

   public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext)
   {
      lazyInitialization();
      registerSyncronization();
      String id = identifierStore.getId(contextual);
      TransactionScopeData data = contextData.get();
      T instance = (T) data.getInstanceStore().get(id);
      if (instance == null)
      {
         instance = contextual.create(creationalContext);
         data.getCreationalContexts().put(id, creationalContext);
         data.getInstanceStore().put(id, instance);
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
      TransactionScopeData data = contextData.get();
      for (Entry<String, Object> e : data.getInstanceStore().entrySet())
      {
         Contextual contextual = identifierStore.getContextual(e.getKey());
         CreationalContext<?> ctx = data.getCreationalContexts().get(e.getKey());
         contextual.destroy(e.getValue(), ctx);
         ctx.release();
      }
      contextData.remove();
   }

   public void beforeCompletion()
   {

   }

   private class TransactionScopeData
   {
      private final Map<String, Object> instanceStore = new HashMap<String, Object>();
      private final Map<String, CreationalContext<?>> creationalContexts = new HashMap<String, CreationalContext<?>>();
      private boolean syncronisationRegistered;

      public boolean isSyncronisationRegistered()
      {
         return syncronisationRegistered;
      }

      public void setSyncronisationRegistered(boolean syncronisationRegistered)
      {
         this.syncronisationRegistered = syncronisationRegistered;
      }

      public Map<String, Object> getInstanceStore()
      {
         return instanceStore;
      }

      public Map<String, CreationalContext<?>> getCreationalContexts()
      {
         return creationalContexts;
      }

   }

}
