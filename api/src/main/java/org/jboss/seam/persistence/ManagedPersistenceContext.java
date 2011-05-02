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
package org.jboss.seam.persistence;

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Support for additional operations for all seam managed persistence contexts.
 *
 * @author Gavin King
 * @author Stuart Douglas
 */
public interface ManagedPersistenceContext {
    /**
     * changes the flush mode of the persistence context. This allows changing
     * the flush mode to @{link FlushModeType#MANUAL} provided the underlying
     * {@link SeamPersistenceProvider} supports it.
     *
     * @param flushMode the new flush mode
     */
    public void changeFlushMode(FlushModeType flushMode);

    /**
     * @return the persistence contexts qualifiers
     */
    public Set<Annotation> getQualifiers();

    /**
     * Returns the type of this persistence context. For JPA persistence contexts
     * this will be <code>javax.persistence.EntityManager</code>. For pure
     * hibernate PC's this will be <code>org.hibernate.Session</code>
     */
    public Class<?> getBeanType();

    /**
     * Returns the appropriate {@link SeamPersistenceProvider} implementation for
     * this persistence context.
     */
    public SeamPersistenceProvider getProvider();

    /**
     * Closes the persistence context after the current transaction has
     * completed.
     * <p/>
     * If no transaction is active the PC will be closed immediately
     */
    public void closeAfterTransaction();

}
