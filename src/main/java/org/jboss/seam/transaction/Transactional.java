package org.jboss.seam.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.enterprise.util.Nonbinding;
import javax.interceptor.InterceptorBinding;

/**
 * @author Dan Allen
 */
@Inherited
@InterceptorBinding
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE })
public @interface Transactional
{
   /**
    * The transaction propagation type.
    * 
    * @return REQUIRED by default
    */
   @Nonbinding
   TransactionPropagation value() default TransactionPropagation.REQUIRED;
}