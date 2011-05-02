/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.transaction;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ejb.ApplicationException;

/**
 * Seam Annotation for identifying an Exception class as an Application
 * Exception, which does not cause a transaction rollback.
 * <p/>
 * This will NOT control the behavior of EJB container managed transactions. To
 * avoid confusion, it is recommended that this annotation is only used outside
 * an EE environment when @{link {@link ApplicationException} is not available.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface SeamApplicationException {
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
