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

import org.hibernate.FlushMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.jboss.solder.logging.Logger;
import org.jboss.seam.persistence.FlushModeType;
import org.jboss.seam.persistence.HibernatePersistenceProvider;
import org.jboss.seam.persistence.ManagedPersistenceContext;
import org.jboss.seam.persistence.PersistenceContexts;
import org.jboss.seam.persistence.QueryParser;
import org.jboss.seam.persistence.util.BeanManagerUtils;
import org.jboss.seam.persistence.util.InstanceResolver;
import org.jboss.solder.el.Expressions;
import org.jboss.solder.literal.DefaultLiteral;
import org.jboss.seam.transaction.SeamTransaction;
import org.jboss.seam.transaction.literal.DefaultTransactionLiteral;

import javax.enterprise.context.ContextNotActiveException;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 * Proxy handler for the seam managed Hibernate session. This handler makes sure
 * that the EntityManager is enrolled in the current transaction before passing
 * the call through to the delegate
 *
 * @author Stuart Douglas
 */
public class HibernateManagedSessionProxyHandler implements InvocationHandler, Serializable, Synchronization {
    private static final long serialVersionUID = -6539267789786229774L;

    private final Session delegate;

    private PersistenceContexts persistenceContexts;

    private final Set<Annotation> qualifiers;

    protected final BeanManager manager;

    private final HibernatePersistenceProvider provider;

    private boolean persistenceContextsTouched = false;

    private boolean closeOnTransactionCommit = false;

    static final Logger log = Logger.getLogger(HibernateManagedSessionProxyHandler.class);

    private final Instance<Expressions> expressionsInstance;

    private transient SeamTransaction seamTransaction;
    private transient boolean synchronizationRegistered;

    public HibernateManagedSessionProxyHandler(Session delegate, BeanManager beanManager, Set<Annotation> qualifiers, HibernatePersistenceProvider provider, BeanManager manager) {
        this.qualifiers = qualifiers;
        this.provider = provider;
        this.delegate = delegate;
        this.expressionsInstance = InstanceResolver.getInstance(Expressions.class, beanManager);
        this.manager = manager;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        touch((ManagedPersistenceContext) proxy);
        if ("changeFlushMode".equals(method.getName()) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(FlushModeType.class)) {
            changeFushMode((FlushModeType) args[0]);
            return null;
        }
        if ("getBeanType".equals(method.getName()) && method.getParameterTypes().length == 0) {
            return EntityManager.class;
        }
        if ("getQualifiers".equals(method.getName()) && method.getParameterTypes().length == 0) {
            return Collections.unmodifiableSet(qualifiers);
        }
        if ("getProvider".equals(method.getName()) && method.getParameterTypes().length == 0) {
            return provider;
        }
        if ("closeAfterTransaction".equals(method.getName()) && method.getParameterTypes().length == 0) {
            closeAfterTransaction();
            return null;
        }
        if ("createQuery".equals(method.getName()) && method.getParameterTypes().length > 0 && method.getParameterTypes()[0].equals(String.class)) {
            return handleCreateQueryWithString(method, args);
        }
        if (!"setFlushMode".equals(method.getName()) && !"getTransaction".equals(method.getName())) {
            if (!synchronizationRegistered) {
                joinTransaction();
            }
        }

        return method.invoke(delegate, args);
    }

    protected Object handleCreateQueryWithString(Method method, Object[] args) throws Throwable {
        if (args[0] == null) {
            return method.invoke(delegate, args);
        }
        String ejbql = (String) args[0];
        if (ejbql.indexOf('#') > 0) {
            QueryParser qp = new QueryParser(expressionsInstance.get(), ejbql);
            Object[] newArgs = args.clone();
            newArgs[0] = qp.getEjbql();
            Query query = (Query) method.invoke(delegate, newArgs);
            for (int i = 0; i < qp.getParameterValues().size(); i++) {
                query.setParameter(QueryParser.getParameterName(i), qp.getParameterValues().get(i));
            }
            return query;
        } else {
            return method.invoke(delegate, args);
        }
    }

    private void joinTransaction() throws SystemException {
        SeamTransaction transaction = getTransaction();
        if (transaction.isActive()) {
            delegate.isOpen();
            try {
                transaction.registerSynchronization(this);
                synchronizationRegistered = true;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void closeAfterTransaction() throws SystemException {
        SeamTransaction transaction = getTransaction();
        if (transaction.isActive()) {
            closeOnTransactionCommit = true;
        } else {
            if (delegate.isOpen()) {
                delegate.close();
            }
        }
    }

    private void changeFushMode(FlushModeType flushModeType) {
        switch (flushModeType) {
            case AUTO:
                delegate.setFlushMode(FlushMode.AUTO);
                break;
            case MANUAL:
                delegate.setFlushMode(FlushMode.MANUAL);
                break;
            case COMMIT:
                delegate.setFlushMode(FlushMode.COMMIT);
                break;
            default:
                throw new RuntimeException("Unkown flush mode: " + flushModeType);
        }
    }

    void touch(ManagedPersistenceContext delegate) {
        if (!persistenceContextsTouched) {
            try {
                // we need to do this first to prevent an infinite loop
                persistenceContextsTouched = true;
                getPersistenceContexts().touch(delegate);
            } catch (ContextNotActiveException e) {
                persistenceContextsTouched = false;
                log.debug("Not touching pc " + this + "as conversation scope not active");
            }
        }
    }

    public void afterCompletion(int status) {
        synchronizationRegistered = false;
        if (closeOnTransactionCommit && delegate.isOpen()) {
            delegate.close();
        }
    }

    public void beforeCompletion() {

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


    private SeamTransaction getTransaction() {
        if(seamTransaction == null) {
            seamTransaction = BeanManagerUtils.getContextualInstance(manager, SeamTransaction.class, DefaultTransactionLiteral.INSTANCE);
        }
        return seamTransaction;
    }

}
