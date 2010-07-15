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
