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

import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;

/**
 * The interface can be implemented to provide extra functionality to a seam
 * managed persistence context.
 * <p/>
 * seam-persistence contains a default implementation and a hibernate based
 * implementation.
 * <p/>
 * Persistence providers are services rather than beans. Injection etc is not
 * availible and the implementations classes must be listed in
 * <p/>
 * META-INF/services/org.jboss.seam.persistence.SeamPersistenceProvider
 *
 * @author Stuart Douglas
 */
public interface SeamPersistenceProvider {

    /**
     * sets the flush mode
     */
    public abstract void setFlushMode(EntityManager entityManager, FlushModeType type);

    /**
     * Should return true if this is the correct persistence provider for the
     * given entity manager factory
     */
    public abstract boolean isCorrectProvider(EntityManager em);

    /**
     * Set the flush mode to manual-only flushing. Called when an atomic
     * persistence context is required.
     */
    public abstract void setFlushModeManual(EntityManager entityManager);

    /**
     * <p>
     * Gets the FlushMode the persistence contexts should use during rendering
     * </p>
     * <p>
     * Ideally, this should be MANUAL since changes should never flush to the
     * database while in render response and the cost of a dirty check can be
     * avoided. However, since the MANUAL mode is not officially part of the JPA
     * specification, the default implementation will perform no operation.
     * </p>
     */
    public abstract FlushModeType getRenderFlushMode();

    /**
     * Does the persistence context have unflushed changes? If it does not,
     * persistence context replication can be optimized.
     *
     * @return true to indicate that there are unflushed changes
     */
    public abstract boolean isDirty(EntityManager entityManager);

    /**
     * Get the value of the entity identifier attribute.
     *
     * @param bean a managed entity instance
     */
    public abstract Object getId(Object bean, EntityManager entityManager);

    /**
     * Get the name of the entity
     *
     * @param bean
     * @param entityManager
     * @throws IllegalArgumentException if the passed object is not an entity
     */
    public abstract String getName(Object bean, EntityManager entityManager) throws IllegalArgumentException;

    /**
     * Get the value of the entity version attribute.
     *
     * @param bean a managed entity instance
     */
    public abstract Object getVersion(Object bean, EntityManager entityManager);

    public abstract void checkVersion(Object bean, EntityManager entityManager, Object oldVersion, Object version);

    /**
     * Register a Synchronization with the current transaction.
     */
    public abstract boolean registerSynchronization(Synchronization sync, EntityManager entityManager);

    /**
     * Wrap the delegate before returning it to the application
     */
    public abstract Object proxyDelegate(Object delegate);

    public abstract EntityManager proxyEntityManager(EntityManager entityManager);

    public abstract Set<Class<?>> getAdditionalEntityManagerInterfaces();

    /**
     * Returns the class of an entity bean instance
     *
     * @param bean The entity bean instance
     * @return The class of the entity bean
     */
    public abstract Class<?> getBeanClass(Object bean);

}