package org.jboss.seam.transaction;

import javax.ejb.EJBContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.weld.extensions.core.Veto;

/**
 * Wraps EJBContext transaction management in a UserTransaction interface. Note
 * that container managed transactions cannot be controlled by the application,
 * so begin(), commit() and rollback() are disallowed in a CMT.
 * 
 * @author Mike Youngstrom
 * @author Gavin King
 * @author Stuart Douglas
 * 
 */
@Veto
public class CMTTransaction extends AbstractUserTransaction
{

   private final EJBContext ejbContext;

   public CMTTransaction(EJBContext ejbContext, Synchronizations sync)
   {
      super(sync);
      this.ejbContext = ejbContext;
      if (ejbContext == null)
      {
         throw new IllegalArgumentException("null EJBContext");
      }
   }

   public void begin() throws NotSupportedException, SystemException
   {
      ejbContext.getUserTransaction().begin();
      getSynchronizations().afterTransactionBegin();
   }

   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      UserTransaction userTransaction = ejbContext.getUserTransaction();
      boolean success = false;
      Synchronizations synchronizations = getSynchronizations();
      synchronizations.beforeTransactionCommit();
      try
      {
         userTransaction.commit();
         success = true;
      }
      finally
      {
         synchronizations.afterTransactionCommit(success);
      }
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      UserTransaction userTransaction = ejbContext.getUserTransaction();
      try
      {
         userTransaction.rollback();
      }
      finally
      {
         getSynchronizations().afterTransactionRollback();
      }
   }

   public int getStatus() throws SystemException
   {
      try
      {
         // TODO: not correct for SUPPORTS or NEVER!
         if (!ejbContext.getRollbackOnly())
         {
            return Status.STATUS_ACTIVE;
         }
         else
         {
            return Status.STATUS_MARKED_ROLLBACK;
         }
      }
      catch (IllegalStateException ise)
      {
         try
         {
            return ejbContext.getUserTransaction().getStatus();
         }
         catch (IllegalStateException is)
         {
            return Status.STATUS_NO_TRANSACTION;
         }
      }
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      ejbContext.setRollbackOnly();
   }

   public void setTransactionTimeout(int timeout) throws SystemException
   {
      ejbContext.getUserTransaction().setTransactionTimeout(timeout);
   }

   @Override
   public void registerSynchronization(Synchronization sync)
   {
      Synchronizations synchronizations = getSynchronizations();
      if (synchronizations.isAwareOfContainerTransactions())
      {
         synchronizations.registerSynchronization(sync);
      }
      else
      {
         throw new UnsupportedOperationException("cannot register synchronization with container transaction, use <transaction:ejb-transaction/>");
      }
   }

}
