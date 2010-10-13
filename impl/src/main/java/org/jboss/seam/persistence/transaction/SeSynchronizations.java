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
package org.jboss.seam.persistence.transaction;

import java.util.Stack;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.transaction.Synchronization;

import org.jboss.weld.extensions.core.Veto;

/**
 * This implementation does not have access to the JTA TransactionManager, so it
 * is not fully aware of container managed transaction lifecycle, and is not
 * able to register Synchronizations with a container managed transaction.
 * 
 * 
 * @author Gavin King
 * @author Stuart Douglas
 */
@RequestScoped
@Veto
public class SeSynchronizations implements Synchronizations
{
   protected Stack<SynchronizationRegistry> synchronizations = new Stack<SynchronizationRegistry>();

   @Inject
   private BeanManager beanManager;

   public void afterTransactionBegin()
   {
      synchronizations.push(new SynchronizationRegistry(beanManager));
   }

   public void afterTransactionCommit(boolean success)
   {
      if (!synchronizations.isEmpty())
      {
         synchronizations.pop().afterTransactionCompletion(success);
      }
   }

   public void afterTransactionRollback()
   {
      if (!synchronizations.isEmpty())
      {
         synchronizations.pop().afterTransactionCompletion(false);
      }
   }

   public void beforeTransactionCommit()
   {
      if (!synchronizations.isEmpty())
      {
         synchronizations.peek().beforeTransactionCompletion();
      }
   }

   public void registerSynchronization(Synchronization sync)
   {
      if (synchronizations.isEmpty())
      {
         throw new IllegalStateException("Transaction begin not detected, try installing transaction:ejb-transaction in components.xml");
      }
      else
      {
         synchronizations.peek().registerSynchronization(sync);
      }
   }

   public boolean isAwareOfContainerTransactions()
   {
      return false;
   }

}
