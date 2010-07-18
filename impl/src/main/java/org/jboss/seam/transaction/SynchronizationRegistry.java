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
package org.jboss.seam.transaction;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A list of Synchronizations to be invoked before and after transaction
 * completion. This class is used when we can't register a synchronization
 * directly with JTA.
 * 
 * @author Gavin King
 * 
 */
class SynchronizationRegistry
{

   private final BeanManager beanManager;

   public SynchronizationRegistry(BeanManager beanManager)
   {
      this.beanManager = beanManager;
   }

   private static final Logger log = LoggerFactory.getLogger(SynchronizationRegistry.class);

   private List<Synchronization> synchronizations = new ArrayList<Synchronization>();

   void registerSynchronization(Synchronization sync)
   {
      synchronizations.add(sync);
   }

   void afterTransactionCompletion(boolean success)
   {
      beanManager.fireEvent(new AfterTransactionCompletion(success));
      for (Synchronization sync : synchronizations)
      {
         try
         {
            sync.afterCompletion(success ? Status.STATUS_COMMITTED : Status.STATUS_ROLLEDBACK);
         }
         catch (Exception e)
         {
            log.error("Exception processing transaction Synchronization after completion", e);
         }
      }
      synchronizations.clear();
   }

   void beforeTransactionCompletion()
   {
      beanManager.fireEvent(new BeforeTransactionCompletion());
      for (Synchronization sync : synchronizations)
      {
         try
         {
            sync.beforeCompletion();
         }
         catch (Exception e)
         {
            log.error("Exception processing transaction Synchronization before completion", e);
         }
      }
   }

}
