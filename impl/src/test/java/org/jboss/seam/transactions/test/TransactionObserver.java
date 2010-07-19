package org.jboss.seam.transactions.test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.jboss.seam.persistence.transaction.event.AfterTransactionCompletion;
import org.jboss.seam.persistence.transaction.event.BeforeTransactionCompletion;

@ApplicationScoped
public class TransactionObserver
{
   private boolean expectSuccess, beforeTransaction, afterTransaction;
   private boolean enabled = false;

   public boolean isEnabled()
   {
      return enabled;
   }

   public void setEnabled(boolean enabled)
   {
      this.enabled = enabled;
   }

   public void reset(boolean expected)
   {
      beforeTransaction = false;
      afterTransaction = false;
      expectSuccess = expected;
   }

   public boolean isBeforeTransaction()
   {
      return beforeTransaction;
   }

   public boolean isAfterTransaction()
   {
      return afterTransaction;
   }

   public void observeBeforeTransactionCommit(@Observes BeforeTransactionCompletion event)
   {
      beforeTransaction = true;
   }

   public void observeBeforeTransactionCommit(@Observes AfterTransactionCompletion event)
   {
      afterTransaction = true;
      if (enabled)
      {
         if (expectSuccess != event.success())
         {
            throw new RuntimeException("Expected success to be " + expectSuccess);
         }
      }
   }

   public void verify()
   {
      if (!((beforeTransaction || !expectSuccess) && afterTransaction))
      {
         throw new RuntimeException("Events not recieved before:" + beforeTransaction + " after:" + afterTransaction);
      }
   }
}
