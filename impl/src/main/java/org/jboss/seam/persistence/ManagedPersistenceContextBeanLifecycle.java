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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.solder.logging.Logger;
import org.jboss.solder.bean.ContextualLifecycle;
import org.jboss.solder.literal.DefaultLiteral;

/**
 * SMPC lifecycle for SMPC's configured via @ExtensionManaged
 *
 * @author Stuart Douglas
 */
public class ManagedPersistenceContextBeanLifecycle implements ContextualLifecycle<EntityManager> {
    private static final Logger log = Logger.getLogger(ManagedPersistenceContextBeanLifecycle.class);

    private final Class<?> proxyClass;

    private final Constructor<?> proxyConstructor;

    private volatile SeamPersistenceProvider persistenceProvider;

    private volatile PersistenceContexts persistenceContexts;

    protected final Annotation[] qualifiers;

    protected final BeanManager manager;

    private EntityManagerFactory emf;

    private final List<SeamPersistenceProvider> persistenceProviders;

    public ManagedPersistenceContextBeanLifecycle(Set<Annotation> qualifiers, ClassLoader loader, BeanManager manager, Set<Class<?>> additionalinterfaces, List<SeamPersistenceProvider> persistenceProviders) {
        this.manager = manager;
        this.persistenceProviders = new ArrayList<SeamPersistenceProvider>(persistenceProviders);
        Class<?>[] interfaces = new Class[additionalinterfaces.size() + 3];
        int count = 0;
        for (Class<?> i : additionalinterfaces) {
            interfaces[count++] = i;
        }

        interfaces[count++] = EntityManager.class;
        interfaces[count++] = Serializable.class;
        interfaces[count++] = ManagedPersistenceContext.class;
        proxyClass = Proxy.getProxyClass(loader, interfaces);
        try {
            proxyConstructor = proxyClass.getConstructor(InvocationHandler.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        this.qualifiers = new Annotation[qualifiers.size()];
        int i = 0;
        for (Annotation a : qualifiers) {
            this.qualifiers[i++] = a;
        }
    }

    /**
     * creates the proxy
     */
    public EntityManager create(Bean<EntityManager> bean, CreationalContext<EntityManager> arg0) {
        try {
            EntityManagerFactory emf = getEntityManagerFactory();
            EntityManager entityManager = emf.createEntityManager();
            entityManager = getPersistenceProvider(entityManager).proxyEntityManager(entityManager);
            PersistenceContexts persistenceContexts = null;
            try {
                persistenceContexts = getPersistenceContexts();
            } catch (ContextNotActiveException e) {
                // it's null already
            }
            ManagedPersistenceContextProxyHandler handler = new ManagedPersistenceContextProxyHandler(entityManager, manager, bean.getQualifiers(), persistenceContexts, getPersistenceProvider(entityManager));
            EntityManager proxy = (EntityManager) proxyConstructor.newInstance(handler);
            arg0.push(proxy);
            getPersistenceProvider(entityManager).setFlushMode(proxy, getFlushMode());
            manager.fireEvent(new SeamManagedPersistenceContextCreated(entityManager), qualifiers);

            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private FlushModeType getFlushMode() {
        try {
            return getPersistenceContexts().getFlushMode();
        } catch (ContextNotActiveException e) {
            // TODO Set the default flush mode for the app
            return FlushModeType.AUTO;
        }
    }

    public void destroy(Bean<EntityManager> bean, EntityManager em, CreationalContext<EntityManager> arg1) {
        ((ManagedPersistenceContext) em).closeAfterTransaction();
        arg1.release();
        try {
            getPersistenceContexts().untouch((ManagedPersistenceContext) em);
        } catch (ContextNotActiveException e) {
            log.debug("Could not untouch PersistenceContext as conversation scope not active");
        }
    }

    private PersistenceContexts getPersistenceContexts() {
        if (persistenceContexts == null) {
            synchronized (this) {
                if (persistenceContexts == null) {
                    Bean<PersistenceContexts> bean = (Bean) manager.resolve(manager.getBeans(PersistenceContexts.class, DefaultLiteral.INSTANCE));
                    if (bean == null) {
                        throw new RuntimeException("Could not find PersistenceContexts bean");
                    }
                    CreationalContext<PersistenceContexts> ctx = manager.createCreationalContext(bean);
                    persistenceContexts = (PersistenceContexts) manager.getReference(bean, PersistenceContexts.class, ctx);
                }
            }
        }
        return persistenceContexts;
    }

    private SeamPersistenceProvider getPersistenceProvider(EntityManager em) {
        if (persistenceProvider == null) {
            synchronized (this) {
                if (persistenceProvider == null) {
                    for (SeamPersistenceProvider i : persistenceProviders) {
                        if (i.isCorrectProvider(em)) {
                            persistenceProvider = i;
                            break;
                        }
                    }
                }
            }
        }
        return persistenceProvider;
    }

    /**
     * lazily resolve the relevant EMF
     */
    protected EntityManagerFactory getEntityManagerFactory() {
        if (emf == null) {
            Bean<EntityManagerFactory> bean = (Bean) manager.resolve(manager.getBeans(EntityManagerFactory.class, qualifiers));
            if (bean == null) {
                throw new RuntimeException("Could not find EntityManagerFactory bean with qualifiers" + Arrays.toString(qualifiers));
            }
            CreationalContext<EntityManagerFactory> ctx = manager.createCreationalContext(bean);
            emf = (EntityManagerFactory) manager.getReference(bean, EntityManagerFactory.class, ctx);
        }
        return emf;
    }

}
