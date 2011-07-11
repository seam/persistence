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

import javax.persistence.EntityManager;
import javax.persistence.OptimisticLockException;
import javax.transaction.Synchronization;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstraction layer for persistence providers (JPA implementations). This class
 * provides a working base implementation that can be optimized for performance
 * and non-standardized features by extending and overriding the methods.
 * <p/>
 * The methods on this class are a great todo list for the next rev of the JPA
 * spec ;-)
 *
 * @author Gavin King
 * @author Pete Muir
 * @author Stuart Douglas
 */
public class DefaultPersistenceProvider implements SeamPersistenceProvider, Serializable {
    public enum Feature {
        /**
         * Identifies whether this JPA provider supports using a wildcard as the
         * subject of a count query.
         * <p/>
         * <p>
         * Here's a count query that uses a wildcard as the subject.
         * </p>
         * <p/>
         * <pre>
         * select count(*) from Vehicle v
         * </pre>
         * <p>
         * Per the JPA 1.0 spec, using a wildcard as a subject of a count query is
         * not permitted. Instead, the subject must be the entity or the alias, as
         * in this count query:
         * </p>
         * <p/>
         * <pre>
         * select count(v) from Vehicle v
         * </pre>
         * <p>
         * Hibernate supports the wildcard syntax as an vendor extension.
         * Furthermore, Hibernate produces an invalid SQL query when using the
         * compliant subject if the entity has a composite primary key. Therefore,
         * we prefer to use the wildcard syntax if it is supported.
         * </p>
         */
        WILDCARD_AS_COUNT_QUERY_SUBJECT
    }

    protected Set<Feature> featureSet = new HashSet<Feature>();

    /**
     * Indicate whether this JPA provider supports the feature defined by the
     * provided Feature enum value.
     */
    public boolean supportsFeature(Feature feature) {
        return featureSet.contains(feature);
    }

    public boolean isCorrectProvider(EntityManager em) {
        return true;
    }

    public void setFlushMode(EntityManager entityManager, FlushModeType type) {
        switch (type) {
            case AUTO:
                entityManager.setFlushMode(javax.persistence.FlushModeType.AUTO);
                break;
            case COMMIT:
                entityManager.setFlushMode(javax.persistence.FlushModeType.COMMIT);
                break;
            case MANUAL:
                setFlushModeManual(entityManager);
                break;
            default:
                throw new RuntimeException("Unkown flush mode: " + type);
        }
    }

    public void setFlushModeManual(EntityManager entityManager) {
        throw new UnsupportedOperationException("Use of FlushMode.MANUAL requires Hibernate as the persistence provider. Please use Hibernate, a custom persistenceProvider, or remove the MANUAL flush mode setting.");
    }

    public FlushModeType getRenderFlushMode() {
        return FlushModeType.COMMIT;
    }

    public boolean isDirty(EntityManager entityManager) {
        return true; // best we can do!
    }

    public Object getId(Object bean, EntityManager entityManager) {
        // return Entity.forBean(bean).getIdentifier(bean);
        return null;
    }

    public String getName(Object bean, EntityManager entityManager) throws IllegalArgumentException {
        return null;
        // return Entity.forBean(bean).getName();
    }

    public Object getVersion(Object bean, EntityManager entityManager) {
        return null;
        // return Entity.forBean(bean).getVersion(bean);
    }

    public void checkVersion(Object bean, EntityManager entityManager, Object oldVersion, Object version) {
        boolean equal;
        if (oldVersion instanceof Date) {
            equal = ((Date) oldVersion).getTime() == ((Date) version).getTime();
        } else {
            equal = oldVersion.equals(version);
        }
        if (!equal) {
            throw new OptimisticLockException("Current database version number does not match passivated version number");
        }
    }

    public boolean registerSynchronization(Synchronization sync, EntityManager entityManager) {
        return false; // best we can do!
    }

    public Object proxyDelegate(Object delegate) {
        return delegate;
    }

    public EntityManager proxyEntityManager(EntityManager entityManager) {
        return entityManager;
    }

    public Set<Class<?>> getAdditionalEntityManagerInterfaces() {
        return Collections.emptySet();
    }

    public Class<?> getBeanClass(Object bean) {
        return null;
        // return Entity.forBean(bean).getBeanClass();
    }

    public Method getPostLoadMethod(Object bean, EntityManager entityManager) {
        return null;
        // return Entity.forBean(bean).getPostLoadMethod();
    }

    public Method getPrePersistMethod(Object bean, EntityManager entityManager) {
        return null;
        // return Entity.forBean(bean).getPrePersistMethod();
    }

    public Method getPreUpdateMethod(Object bean, EntityManager entityManager) {
        return null;
        // return Entity.forBean(bean).getPreUpdateMethod();
    }

    public Method getPreRemoveMethod(Object bean, EntityManager entityManager) {
        return null;
        // return Entity.forBean(bean).getPreRemoveMethod();
    }

}
