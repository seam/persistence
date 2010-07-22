package org.jboss.seam.persistence.transactions.test;

import org.jboss.seam.persistence.transaction.TransactionScoped;

@TransactionScoped
public class TransactionScopedObject
{
   int value = 0;

   public int getValue()
   {
      return value;
   }

   public void setValue(int value)
   {
      this.value = value;
   }
}
