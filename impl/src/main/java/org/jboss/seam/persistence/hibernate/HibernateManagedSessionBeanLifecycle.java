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
package org.jboss.seam.persistence.hibernate;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.Set;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.solder.logging.Logger;
import org.jboss.seam.persistence.HibernatePersistenceProvider;
import org.jboss.seam.persistence.ManagedPersistenceContext;
import org.jboss.seam.persistence.PersistenceContexts;
import org.jboss.solder.bean.ContextualLifecycle;
import org.jboss.solder.literal.DefaultLiteral;

/**
 * lifecycle for seam managed hibernate sessions
 *
 * @author Stuart Douglas
 */
public class HibernateManagedSessionBeanLifecycle implements ContextualLifecycle<Session> {
    private static final Logger log = Logger.getLogger(HibernateManagedSessionBeanLifecycle.class);

    private final Class<?> proxyClass;

    private final Constructor<?> proxyConstructor;

    private HibernatePersistenceProvider persistenceProvider = new HibernatePersistenceProvider();

    private PersistenceContexts persistenceContexts;

    protected final Annotation[] qualifiers;

    protected final BeanManager manager;

    private SessionFactory sessionFactory;

    public HibernateManagedSessionBeanLifecycle(Set<Annotation> qualifiers, ClassLoader loader, BeanManager manager) {
        this.manager = manager;
        Set<Class<?>> additionalinterfaces = persistenceProvider.getAdditionalSessionInterfaces();
        Class<?>[] interfaces = new Class[additionalinterfaces.size() + 3];
        int count = 0;
        for (Class<?> i : additionalinterfaces) {
            interfaces[count++] = i;
        }

        interfaces[count++] = Session.class;
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
    public Session create(Bean<Session> bean, CreationalContext<Session> arg0) {
        try {
            SessionFactory sf = getSessionFactory();
            Session session = sf.openSession();
            session = (Session) persistenceProvider.proxyDelegate(session);
            HibernateManagedSessionProxyHandler handler = new HibernateManagedSessionProxyHandler(session, manager, bean.getQualifiers(), persistenceProvider, manager);
            Session proxy = (Session) proxyConstructor.newInstance(handler);
            try {
                ((ManagedPersistenceContext) proxy).changeFlushMode(getPersistenceContexts().getFlushMode());
            } catch (ContextNotActiveException e) {

            }
            manager.fireEvent(new SeamManagedHibernateSessionCreated(proxy), qualifiers);

            return proxy;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void destroy(Bean<Session> bean, Session session, CreationalContext<Session> arg1) {
        ((ManagedPersistenceContext) session).closeAfterTransaction();
        arg1.release();
        try {
            getPersistenceContexts().untouch((ManagedPersistenceContext) session);
        } catch (ContextNotActiveException e) {
            log.debug("Could not untouch PersistenceContext as conversation scope not active");
        }
    }

    private PersistenceContexts getPersistenceContexts() {
        if (persistenceContexts == null) {
            Bean<PersistenceContexts> bean = (Bean) manager.resolve(manager.getBeans(PersistenceContexts.class, DefaultLiteral.INSTANCE));
            if (bean == null) {
                throw new RuntimeException("Could not find PersistenceContexts bean");
            }
            CreationalContext<PersistenceContexts> ctx = manager.createCreationalContext(bean);
            persistenceContexts = (PersistenceContexts) manager.getReference(bean, PersistenceContexts.class, ctx);
        }
        return persistenceContexts;
    }

    /**
     * lazily resolve the relevant SessionFactory
     */
    protected SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            Bean<SessionFactory> bean = (Bean) manager.resolve(manager.getBeans(SessionFactory.class, qualifiers));
            if (bean == null) {
                throw new RuntimeException("Could not find SessionFactory bean with qualifiers" + qualifiers);
            }
            CreationalContext<SessionFactory> ctx = manager.createCreationalContext(bean);
            sessionFactory = (SessionFactory) manager.getReference(bean, SessionFactory.class, ctx);
        }
        return sessionFactory;
    }

}
