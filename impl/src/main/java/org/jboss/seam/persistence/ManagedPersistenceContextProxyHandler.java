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

import org.jboss.solder.logging.Logger;
import org.jboss.seam.persistence.util.BeanManagerUtils;
import org.jboss.seam.transaction.SeamTransaction;
import org.jboss.seam.transaction.literal.DefaultTransactionLiteral;

import javax.enterprise.context.ContextNotActiveException;
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
 * Proxy handler for the seam managed persistence context. This handler makes
 * sure that the EntityManager is enrolled in the current transaction before
 * passing the call through to the delegate
 *
 * @author Stuart Douglas
 */
public class ManagedPersistenceContextProxyHandler extends PersistenceContextProxyHandler implements InvocationHandler, Serializable, Synchronization {

    private static final long serialVersionUID = -6539267789786229774L;

    private final EntityManager delegate;

    private final PersistenceContexts persistenceContexts;

    private final Set<Annotation> qualifiers;

    private final SeamPersistenceProvider provider;

    private final BeanManager beanManager;

    private boolean persistenceContextsTouched = false;

    private boolean closeOnTransactionCommit = false;

    private transient SeamTransaction seamTransaction;

    private transient boolean synchronizationRegistered;

    static final Logger log = Logger.getLogger(ManagedPersistenceContextProxyHandler.class);

    public ManagedPersistenceContextProxyHandler(EntityManager delegate, BeanManager beanManager, Set<Annotation> qualifiers, PersistenceContexts persistenceContexts, SeamPersistenceProvider provider) {
        super(delegate, beanManager);
        this.qualifiers = qualifiers;
        this.provider = provider;
        this.delegate = delegate;
        this.persistenceContexts = persistenceContexts;
        this.beanManager = beanManager;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        if ("getTransaction".equals(method.getName()) && method.getParameterTypes().length == 0) {
            return super.invoke(proxy, method, args);
        }
        // we do not join the transaction for setFlushMode calls, as this may
        // result in an infinite loop, as this is called during SMPC
        // initialisation
        if (!"setFlushMode".equals(method.getName())) {
            if (!synchronizationRegistered) {
                joinTransaction();
            }
        }

        touch((ManagedPersistenceContext) proxy);

        return super.invoke(proxy, method, args);
    }

    private void joinTransaction() throws SystemException {
        SeamTransaction transaction = getTransaction();
        if (transaction.isActive()) {
            synchronizationRegistered = true;
            transaction.enlist(delegate);
            try {
                transaction.registerSynchronization(this);
            } catch (Exception e) {
                // synchronizationRegistered =
                // PersistenceProvider.instance().registerSynchronization(this,
                // entityManager);
                synchronizationRegistered = false;
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
        provider.setFlushMode(delegate, flushModeType);
    }

    void touch(ManagedPersistenceContext delegate) {
        if (!persistenceContextsTouched) {
            try {
                // we need to do this first to prevent an infinite loop
                persistenceContextsTouched = true;
                if (persistenceContexts != null) {
                    persistenceContexts.touch(delegate);
                }
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


    private SeamTransaction getTransaction() {
        if(seamTransaction == null) {
            seamTransaction = BeanManagerUtils.getContextualInstance(beanManager, SeamTransaction.class, DefaultTransactionLiteral.INSTANCE);
        }
        return seamTransaction;
    }
}
