/**
 * 
 */
package org.jboss.seam.persistence.transaction.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.jboss.seam.persistence.transaction.TransactionScoped;

public class TransactionScopedLiteral extends AnnotationLiteral<TransactionScoped> implements TransactionScoped
{
   private TransactionScopedLiteral()
   {
   }

   TransactionScopedLiteral INSTANCE = new TransactionScopedLiteral();

}