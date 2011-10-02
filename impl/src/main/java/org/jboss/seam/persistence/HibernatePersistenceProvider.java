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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.TransientObjectException;
import org.hibernate.proxy.HibernateProxy;
import org.jboss.solder.logging.Logger;
import org.jboss.solder.core.Veto;
import org.jboss.solder.reflection.Reflections;

/**
 * Support for non-standardized features of Hibernate, when used as the JPA
 * persistence provider.
 *
 * @author Gavin King
 * @author Pete Muir
 * @author Stuart Douglas
 */
@Veto
public class HibernatePersistenceProvider extends DefaultPersistenceProvider {

    private static Logger log = Logger.getLogger(HibernatePersistenceProvider.class);
    private static Class<?> FULL_TEXT_SESSION;
    private static Method FULL_TEXT_SESSION_CONSTRUCTOR;
    private static Method FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR;
    private static Class<?> FULL_TEXT_ENTITYMANAGER;

    static {
        try {
            boolean hibernateSearchPresent = false;
            try {
                Reflections.classForName("org.hibernate.search.Version");
                hibernateSearchPresent = true;
            } catch (Exception e) {
                log.debug("no Hibernate Search", e);
            }
            if (hibernateSearchPresent) {
                Class<?> searchClass = Reflections.classForName("org.hibernate.search.Search");
                try {
                    FULL_TEXT_SESSION_CONSTRUCTOR = searchClass.getDeclaredMethod("getFullTextSession", Session.class);
                } catch (NoSuchMethodException noSuchMethod) {
                    log.debug("org.hibernate.search.Search.getFullTextSession(Session) not found, trying deprecated method name createFullTextSession");
                    FULL_TEXT_SESSION_CONSTRUCTOR = searchClass.getDeclaredMethod("createFullTextSession", Session.class);
                }
                FULL_TEXT_SESSION = Reflections.classForName("org.hibernate.search.FullTextSession");
                Class<?> jpaSearchClass = Reflections.classForName("org.hibernate.search.jpa.Search");
                try {
                    FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR = jpaSearchClass.getDeclaredMethod("getFullTextEntityManager", EntityManager.class);
                } catch (NoSuchMethodException noSuchMethod) {
                    log.debug("org.hibernate.search.jpa.getFullTextSession(EntityManager) not found, trying deprecated method name createFullTextEntityManager");
                    FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR = jpaSearchClass.getDeclaredMethod("createFullTextEntityManager", EntityManager.class);
                }
                FULL_TEXT_ENTITYMANAGER = Reflections.classForName("org.hibernate.search.jpa.FullTextEntityManager");
                log.info("Hibernate Search is available");
            }
        } catch (Exception e) {
            log.debug("no Hibernate Search", e);
        }
    }

    @Inject
    public void init() {
        featureSet.add(Feature.WILDCARD_AS_COUNT_QUERY_SUBJECT);
    }

    @Override
    public boolean isCorrectProvider(EntityManager em) {
        return em.getDelegate() instanceof Session;
    }

    @Override
    public void setFlushModeManual(EntityManager entityManager) {
        try {
            getSession(entityManager).setFlushMode(FlushMode.MANUAL);
        } catch (NotHibernateException nhe) {
            super.setFlushModeManual(entityManager);
        }
    }

    @Override
    public FlushModeType getRenderFlushMode() {
        return FlushModeType.MANUAL;
    }

    @Override
    public boolean isDirty(EntityManager entityManager) {
        try {
            return getSession(entityManager).isDirty();
        } catch (NotHibernateException nhe) {
            return super.isDirty(entityManager);
        }
    }

    @Override
    public Object getId(Object bean, EntityManager entityManager) {
        try {
            return getSession(entityManager).getIdentifier(bean);
        } catch (NotHibernateException nhe) {
            return super.getId(bean, entityManager);
        } catch (TransientObjectException e) {
            if (bean instanceof HibernateProxy) {
                return super.getId(((HibernateProxy) bean).getHibernateLazyInitializer().getImplementation(), entityManager);
            } else {
                return super.getId(bean, entityManager);
            }
        }
    }

    @Override
    public boolean registerSynchronization(Synchronization sync, EntityManager entityManager) {
        try {
            // TODO: just make sure that a Hibernate JPA EntityTransaction
            // delegates to the Hibernate Session transaction
            getSession(entityManager).getTransaction().registerSynchronization(sync);
            return true;
        } catch (NotHibernateException nhe) {
            return super.registerSynchronization(sync, entityManager);
        }

    }

    @Override
    public String getName(Object bean, EntityManager entityManager) throws IllegalArgumentException {
        try {
            return getSession(entityManager).getEntityName(bean);
        } catch (NotHibernateException nhe) {
            return super.getName(bean, entityManager);
        } catch (Exception e) // TODO: what should we actually do here
        {
            return super.getName(bean, entityManager);
        }
    }

    private Session getSession(EntityManager entityManager) {
        Object delegate = entityManager.getDelegate();
        if (delegate instanceof Session) {
            return (Session) delegate;
        } else {
            throw new NotHibernateException();
        }
    }

    /**
     * Wrap the Hibernate Session in a proxy that implements FullTextSession if
     * Hibernate Search is available in the classpath.
     */
    static Session proxySession(Session session) {
        if (FULL_TEXT_SESSION_CONSTRUCTOR == null || FULL_TEXT_SESSION == null) {
            return session;
        } else {
            if (FULL_TEXT_SESSION.isAssignableFrom(session.getClass())) {
                return session;
            }
            try {
                return (Session) FULL_TEXT_SESSION_CONSTRUCTOR.invoke(null, session);
            } catch (Exception e) {
                log.warn("Unable to wrap into a FullTextSessionProxy, regular SessionProxy returned", e);
                return session;
            }
        }
    }

    /**
     * Wrap the delegate Hibernate Session in a proxy that implements
     * FullTextSession if Hibernate Search is available in the classpath.
     */
    @Override
    public Object proxyDelegate(Object delegate) {
        try {
            return proxySession((Session) delegate);
        } catch (NotHibernateException nhe) {
            return super.proxyDelegate(delegate);
        } catch (Exception e) {
            throw new RuntimeException("could not proxy delegate", e);
        }
    }

    @Override
    public EntityManager proxyEntityManager(EntityManager entityManager) {
        if (FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR == null) {
            return super.proxyEntityManager(entityManager);
        } else {
            try {
                return (EntityManager) FULL_TEXT_ENTITYMANAGER_CONSTRUCTOR.invoke(null, super.proxyEntityManager(entityManager));
            } catch (Exception e) {
                // throw new
                // RuntimeException("could not proxy FullTextEntityManager", e);
                return super.proxyEntityManager(entityManager);
            }
        }
    }

    public Set<Class<?>> getAdditionalEntityManagerInterfaces() {
        if (FULL_TEXT_ENTITYMANAGER == null) {
            return Collections.emptySet();
        }
        return (Set) Collections.singleton(FULL_TEXT_ENTITYMANAGER);
    }

    public Set<Class<?>> getAdditionalSessionInterfaces() {
        if (FULL_TEXT_SESSION == null) {
            return Collections.emptySet();
        }
        return (Set) Collections.singleton(FULL_TEXT_SESSION);
    }

    /**
     * Occurs when Hibernate is in the classpath, but this particular
     * EntityManager is not from Hibernate
     *
     * @author Gavin King
     */
    static class NotHibernateException extends IllegalArgumentException {
    }

}
