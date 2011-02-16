package org.jboss.seam.persistence.transactions.test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.enterprise.inject.Stereotype;

import org.jboss.seam.persistence.transaction.TransactionPropagation;
import org.jboss.seam.persistence.transaction.Transactional;

@Stereotype
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Transactional(TransactionPropagation.REQUIRED)
public @interface TransactionalStereotype
{

}
