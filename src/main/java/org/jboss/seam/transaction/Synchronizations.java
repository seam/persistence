package org.jboss.seam.transaction;

import javax.transaction.Synchronization;

/**
 * Interface for registering transaction synchronizations
 * 
 * @author Gavin King
 * 
 */
public interface Synchronizations
{
   public void afterTransactionBegin();

   public void afterTransactionCommit(boolean success);

   public void afterTransactionRollback();

   public void beforeTransactionCommit();

   public void registerSynchronization(Synchronization sync);

   public boolean isAwareOfContainerTransactions();
}