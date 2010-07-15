package org.jboss.seam.transaction;

public class AfterTransactionCompletion
{
   private final boolean success;

   public AfterTransactionCompletion(boolean success)
   {
      this.success = success;
   }

   public boolean success()
   {
      return success;
   }
}
