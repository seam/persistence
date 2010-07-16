package org.jboss.seam.transaction;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;

/**
 * Internal qualifier that is used to stop some beans from being exposed to the
 * user
 * 
 * @author Stuart Douglas
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Qualifier
@interface TransactionQualifier
{
   public static class TransactionQualifierLiteral extends AnnotationLiteral<TransactionQualifier> implements TransactionQualifier
   {
   }
}
