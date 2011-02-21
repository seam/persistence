/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.persistence.transaction;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.transaction.Synchronization;

import org.jboss.seam.solder.core.Veto;

/**
 * This implementation does not have access to the JTA TransactionManager, so it
 * is not fully aware of container managed transaction lifecycle, and is not
 * able to register Synchronizations with a container managed transaction.
 *
 *
 * @author Gavin King
 * @author Stuart Douglas
 */
@ApplicationScoped
@Veto
public class SeSynchronizations implements Synchronizations
{
   protected ThreadLocalStack<SynchronizationRegistry> synchronizations = new ThreadLocalStack<SynchronizationRegistry>();

   @Inject
   private BeanManager beanManager;

   public void afterTransactionBegin()
   {
      synchronizations.push(new SynchronizationRegistry(beanManager));
   }

   public void afterTransactionCompletion(boolean success)
   {
      if (!synchronizations.isEmpty())
      {
         synchronizations.pop().afterTransactionCompletion(success);
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
