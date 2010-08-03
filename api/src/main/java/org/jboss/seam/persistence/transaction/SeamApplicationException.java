package org.jboss.seam.persistence.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Seam Annotation for identifying an Exception class as an Application
 * Exception, which does not cause a transaction rollback
 * 
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SeamApplicationException
{
   /**
    * Indicates whether the application exception designation should apply to
    * subclasses of the annotated exception class.
    */
   boolean inherited() default true;

   /**
    * Indicates whether the container should cause the transaction to rollback
    * when the exception is thrown.
    */
   boolean rollback() default false;
}
