package org.jboss.seam.persistence.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;
import javax.transaction.UserTransaction;

/**
 * Qualifier that is used to denote the SeamTransaction implementation that is
 * used by the transaction interceptor and other seam transaction services.
 * 
 * A qualifier is necessary to prevent the seam provided {@link UserTransaction}
 * wrapper {@link SeamTransaction} from conflicting with the container provided
 * built-in UserTransaction
 * 
 * @author Stuart Douglas
 * 
 */
@Qualifier
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
public @interface DefaultTransaction
{;
}
