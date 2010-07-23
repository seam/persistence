package org.jboss.seam.persistence.transaction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.interceptor.InterceptorBinding;

/**
 * Interceptor binding for {@link Transactional} beans
 * 
 * @author Stuart Douglas
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@InterceptorBinding
@Target( { ElementType.TYPE, ElementType.METHOD })
public @interface TransactionalInterceptorBinding
{

}
