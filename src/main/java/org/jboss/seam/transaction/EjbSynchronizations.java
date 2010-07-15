package org.jboss.seam.transaction;

import java.rmi.RemoteException;
import java.util.LinkedList;

import javax.ejb.EJBException;
import javax.ejb.Remove;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.transaction.Synchronization;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives JTA transaction completion notifications from the EJB container, and
 * passes them on to the registered Synchronizations. This implementation is
 * fully aware of container managed transactions and is able to register
 * Synchronizations for the container transaction.
 * 
 * @author Gavin King
 * 
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
public class EjbSynchronizations implements LocalEjbSynchronizations, SessionSynchronization
{
   private static final Logger log = LoggerFactory.getLogger(EjbSynchronizations.class);

   // maintain two lists to work around a bug in JBoss EJB3 where a new
   // SessionSynchronization
   // gets registered each time the bean is called
   protected LinkedList<SynchronizationRegistry> synchronizations = new LinkedList<SynchronizationRegistry>();
   protected LinkedList<SynchronizationRegistry> committing = new LinkedList<SynchronizationRegistry>();

   public void afterBegin()
   {
      log.debug("afterBegin");
      synchronizations.addLast(new SynchronizationRegistry());
   }

   public void beforeCompletion() throws EJBException, RemoteException
   {
      log.debug("beforeCompletion");
      SynchronizationRegistry sync = synchronizations.removeLast();
      sync.beforeTransactionCompletion();
      committing.addLast(sync);
   }

   public void afterCompletion(boolean success) throws EJBException, RemoteException
   {
      log.debug("afterCompletion");
      if (committing.isEmpty())
      {
         if (success)
         {
            throw new IllegalStateException("beforeCompletion was never called");
         }
         else
         {
            synchronizations.removeLast().afterTransactionCompletion(false);
         }
      }
      else
      {
         committing.removeFirst().afterTransactionCompletion(success);
      }
   }

   public boolean isAwareOfContainerTransactions()
   {
      return true;
   }

   public void afterTransactionBegin()
   {
      // noop, let JTA notify us
   }

   public void afterTransactionCommit(boolean success)
   {
      // noop, let JTA notify us
   }

   public void afterTransactionRollback()
   {
      // noop, let JTA notify us
   }

   public void beforeTransactionCommit()
   {
      // noop, let JTA notify us
   }

   public void registerSynchronization(Synchronization sync)
   {
      synchronizations.getLast().registerSynchronization(sync);
   }

   @Remove
   public void destroy()
   {
   }

}
