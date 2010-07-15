package org.jboss.seam.transaction;

import java.util.Stack;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.transaction.Synchronization;

/**
 * This implementation does not have access to the JTA TransactionManager, so it
 * is not fully aware of container managed transaction lifecycle, and is not
 * able to register Synchronizations with a container managed transaction.
 * 
 * This is an alternative, and as such must be enabled in beans.xml.
 * 
 * @author Gavin King
 * @author Stuart Douglas
 */
@RequestScoped
@Alternative
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
