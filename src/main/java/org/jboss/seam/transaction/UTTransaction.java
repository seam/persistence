package org.jboss.seam.transaction;

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.weld.extensions.core.Veto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps JTA transaction management in a Seam UserTransaction interface.
 * 
 * @author Mike Youngstrom
 * @author Gavin King
 * 
 */
@Veto
public class UTTransaction extends AbstractUserTransaction
{
   private static final Logger log = LoggerFactory.getLogger(UTTransaction.class);

   private final javax.transaction.UserTransaction delegate;

   UTTransaction(javax.transaction.UserTransaction delegate, Synchronizations sync)
   {
      super(sync);
      this.delegate = delegate;
      if (delegate == null)
      {
         throw new IllegalArgumentException("null UserTransaction");
      }
   }

   public void begin() throws NotSupportedException, SystemException
   {
      log.debug("beginning JTA transaction");
      delegate.begin();
      getSynchronizations().afterTransactionBegin();
   }

   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      log.debug("committing JTA transaction");
      boolean success = false;
      Synchronizations synchronizations = getSynchronizations();
      synchronizations.beforeTransactionCommit();
      try
      {
         delegate.commit();
         success = true;
      }
      finally
      {
         synchronizations.afterTransactionCommit(success);
      }
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      log.debug("rolling back JTA transaction");
      try
      {
         delegate.rollback();
      }
      finally
      {
         getSynchronizations().afterTransactionRollback();
      }
   }

   public int getStatus() throws SystemException
   {
      return delegate.getStatus();
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      delegate.setRollbackOnly();
   }

   public void setTransactionTimeout(int timeout) throws SystemException
   {
      delegate.setTransactionTimeout(timeout);
   }

   @Override
   public void registerSynchronization(Synchronization sync)
   {
      getSynchronizations().registerSynchronization(sync);
   }

}
