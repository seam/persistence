/**
 * 
 */
package org.jboss.seam.persistence.transaction.literal;

import javax.enterprise.util.AnnotationLiteral;

import org.jboss.seam.persistence.transaction.DefaultTransaction;

public class DefaultTransactionLiteral extends AnnotationLiteral<DefaultTransaction> implements DefaultTransaction
{
   private DefaultTransactionLiteral()
   {
   }

   public static final DefaultTransactionLiteral INSTANCE = new DefaultTransactionLiteral();

}