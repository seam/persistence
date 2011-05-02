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

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Demarcates transaction boundaries
 * <p/>
 * Note that is you are using seam managed transactions seam will automatically
 * manage your transactions for you, rendering this unnecessary
 * <p/>
 * Note that this annotation is not actually an intercepter binding. It is
 * replaced by an intercepter binding at runtime by a portable extension in the
 * ProcessAnnotatedType phase
 *
 * @author Dan Allen
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Transactional {
    /**
     * The transaction propagation type.
     *
     * @return REQUIRED by default
     */
    TransactionPropagation value() default TransactionPropagation.REQUIRED;
}