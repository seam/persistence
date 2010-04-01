package org.jboss.seam.transaction;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A rudimentary transaction interceptor implementation, intended as a proof
 * of concept.
 *
 * @author Matt Corey
 * @author Dan Allen
 */
public @Transactional @Interceptor class JtaTxInterceptor {

   private static final Logger log = LoggerFactory.getLogger(JtaTxInterceptor.class);

   @Inject private UserTransaction utx;

   /**
    * Super quick impl. Needs to be done correctly.
    */
   @AroundInvoke
   public Object workInTransaction(InvocationContext ic) throws Exception {
      // TODO cache this information
      TransactionPropagation type = getTransactionPropagation(ic.getMethod());

      int status = utx.getStatus();
      boolean transactionActive = (status == Status.STATUS_ACTIVE || status == Status.STATUS_MARKED_ROLLBACK);
      boolean beginTransaction = isNewTransactionRequired(type, transactionActive);

      if (beginTransaction) {
         log.debug("Beginning transaction");
         utx.begin();
      }

      Object result = null;
      try {
         result = ic.proceed();

         if (beginTransaction) {
            if (utx.getStatus() == Status.STATUS_MARKED_ROLLBACK) {
               log.debug("Rolling back transaction marked for rollback");
               utx.rollback();
            }
            else {
               log.debug("Committing transaction");
               utx.commit();
            }
         }

         return result;
      } catch (Exception e) {
         if (beginTransaction && utx.getStatus() != Status.STATUS_NO_TRANSACTION) {
            // FIXME don't rollback if this is an application exception which indicates no rollback
            log.debug("Rolling back transaction as the result of an exception");
            utx.rollback();
         }

         throw e;
      }
   }

    /**
     * Get the TransactionPropagation value
     * FIXME cache this information
     */
    private TransactionPropagation getTransactionPropagation(Method m) {
       // first look at the explicit method-level annotation
       if (m.isAnnotationPresent(Transactional.class)) {
          return m.getAnnotation(Transactional.class).value();
       }
        // now look at the method-level meta-annotations
        for (Annotation a: m.getAnnotations()) {
            if (a.annotationType().isAnnotationPresent(Transactional.class)) {
                return a.annotationType().getAnnotation(Transactional.class).value();
            }
        }
        // now try the explicit class-level annotation
        if (m.getDeclaringClass().isAnnotationPresent(Transactional.class)) {
           return m.getDeclaringClass().getAnnotation(Transactional.class).value();
        }
        // finally, try the class-level meta-annotations
        for (Annotation a: m.getDeclaringClass().getAnnotations()) {
            if (a.annotationType().isAnnotationPresent(Transactional.class)) {
                return a.annotationType().getAnnotation(Transactional.class).value();
            }
        }
        return null;
    }

   private boolean isNewTransactionRequired(TransactionPropagation type, boolean transactionActive) {
      switch (type) {
         case REQUIRED:
            return !transactionActive;
         case SUPPORTS:
            return false;
         case MANDATORY:
            if (!transactionActive) {
               throw new IllegalStateException("No transaction active on call to method that requires a transaction.");
            }
            else {
               return false;
            }
         case NEVER:
            if (transactionActive) {
               throw new IllegalStateException("Transaction active on call to method that does not support transactions.");
            }
            else {
               return false;
            }
         default:
            throw new IllegalArgumentException("Unknown transaction type " + type);
      }
   }
}