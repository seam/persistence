package org.jboss.seam.transaction;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.weld.extensions.core.Veto;

/**
 * When no kind of transaction management exists.
 * 
 * @author Mike Youngstrom
 * @author Gavin King
 * 
 */
@Veto
public class NoTransaction extends AbstractUserTransaction
{

   public NoTransaction()
   {
      super(null);
   }

   public void begin() throws NotSupportedException, SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   public int getStatus() throws SystemException
   {
      return Status.STATUS_NO_TRANSACTION;
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   public void setTransactionTimeout(int timeout) throws SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   @Override
   public void registerSynchronization(Synchronization sync)
   {
      throw new UnsupportedOperationException("no transaction");
   }

   @Override
   public void enlist(EntityManager entityManager) throws SystemException
   {
      // no-op
   }

}
