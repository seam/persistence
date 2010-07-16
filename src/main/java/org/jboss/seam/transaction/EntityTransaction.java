package org.jboss.seam.transaction;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.weld.extensions.core.Veto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for the JPA EntityTransaction API.
 * 
 * Adapts JPA transaction management to a Seam UserTransaction 
 * interface.For use in non-JTA-capable environments.
 * 
 * @author Gavin King
 * 
 */
@RequestScoped
@Veto
public class EntityTransaction extends AbstractUserTransaction
{
   private static final Logger log = LoggerFactory.getLogger(EntityTransaction.class);
   
   @Inject
   private EntityManager entityManager;
   
   
   @Inject
   public EntityTransaction(Synchronizations sync)
   {
      super(sync);
   }

   private javax.persistence.EntityTransaction getDelegate()
   {
      return entityManager.getTransaction();
   }

   public void begin() throws NotSupportedException, SystemException
   {
      log.debug("beginning JPA resource-local transaction");
      //TODO: translate exceptions that occur into the correct JTA exception
      try
      {
         getDelegate().begin();
         getSynchronizations().afterTransactionBegin();
      }
      catch (RuntimeException re)
      {
         throw re;
      }
   }

   public void commit() throws RollbackException, HeuristicMixedException,
            HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      log.debug("committing JPA resource-local transaction");
      javax.persistence.EntityTransaction delegate = getDelegate();
      boolean success = false;
      try
      {
         if ( delegate.getRollbackOnly() )
         {
            delegate.rollback();
            throw new RollbackException();
         }
         else
         {
            getSynchronizations().beforeTransactionCommit();
            delegate.commit();
            success = true;
         }
      }
      finally
      {
         getSynchronizations().afterTransactionCommit(success);
      }
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      log.debug("rolling back JPA resource-local transaction");
      //TODO: translate exceptions that occur into the correct JTA exception
      javax.persistence.EntityTransaction delegate = getDelegate();
      try
      {
         delegate.rollback();
      }
      finally
      {
         getSynchronizations().afterTransactionRollback();
      }
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      log.debug("marking JPA resource-local transaction for rollback");
      getDelegate().setRollbackOnly();
   }

   public int getStatus() throws SystemException
   {
      if (getDelegate().getRollbackOnly())
      {
         return Status.STATUS_MARKED_ROLLBACK;
      }
      else if (getDelegate().isActive())
      {
         return Status.STATUS_ACTIVE;
      }
      else
      {
         return Status.STATUS_NO_TRANSACTION;
      }
   }

   public void setTransactionTimeout(int timeout) throws SystemException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void registerSynchronization(Synchronization sync)
   {
      if ( log.isDebugEnabled() )
      {
         log.debug("registering synchronization: " + sync);
      }
      //try to register the synchronization directly with the
      //persistence provider, but if this fails, just hold
      //on to it myself
      // if ( !PersistenceProvider.instance().registerSynchronization(sync,
      // currentEntityManager) )
      // {
      // getSynchronizations().registerSynchronization(sync);
      // }
   }

   @Override
   public boolean isConversationContextRequired()
   {
      return true;
   }

   @Override
   public void enlist(EntityManager entityManager)
   {
      //no-op
   }

}
