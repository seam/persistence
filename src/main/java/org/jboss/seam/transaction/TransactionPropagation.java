package org.jboss.seam.transaction;

/**
 * @author Dan Allen
 */
public enum TransactionPropagation
{
   REQUIRED, SUPPORTS, MANDATORY, NEVER;

   public boolean isNewTransactionRequired(boolean transactionActive)
   {
      switch (this)
      {
      case REQUIRED:
         return !transactionActive;
      case SUPPORTS:
         return false;
      case MANDATORY:
         if (!transactionActive)
         {
            throw new IllegalStateException("No transaction active on call to MANDATORY method");
         }
         else
         {
            return false;
         }
      case NEVER:
         if (transactionActive)
         {
            throw new IllegalStateException("Transaction active on call to NEVER method");
         }
         else
         {
            return false;
         }
      default:
         throw new IllegalArgumentException();
      }
   }
}