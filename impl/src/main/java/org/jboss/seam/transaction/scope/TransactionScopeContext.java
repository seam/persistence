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
package org.jboss.seam.transaction.scope;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.enterprise.context.spi.Context;
import javax.enterprise.context.spi.Contextual;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.transaction.DefaultTransaction;
import org.jboss.seam.transaction.SeamTransaction;
import org.jboss.seam.transaction.TransactionScoped;
import org.jboss.seam.transaction.literal.DefaultTransactionLiteral;

/**
 * Context for the {@link TransactionScoped} scope
 *
 * @author stuart
 */
public class TransactionScopeContext implements Context, Synchronization {

    private SeamTransaction userTransaction;

    private final BeanManager beanManager;

    private final ContextualIdentifierStore identifierStore = new ContextualIdentifierStore();

    private final ThreadLocal<TransactionScopeData> contextData = new ThreadLocal<TransactionScopeData>() {
        protected TransactionScopeData initialValue() {
            return new TransactionScopeData();
        }

        ;
    };

    public TransactionScopeContext(BeanManager beanManager) {
        this.beanManager = beanManager;

    }

    /**
     * we need to resolve the transaction bean lazily, after startup has
     * completed
     */
    private void lazyInitialization() {
        if (userTransaction == null) {
            synchronized (this) {
                if (userTransaction == null) {
                    Bean<SeamTransaction> bean = (Bean) beanManager.resolve(beanManager.getBeans(SeamTransaction.class, DefaultTransactionLiteral.INSTANCE));
                    if (bean == null) {
                        throw new RuntimeException("Could not find SeamTransaction bean with qualifier " + DefaultTransaction.class.getName());
                    }
                    CreationalContext<SeamTransaction> ctx = beanManager.createCreationalContext(bean);
                    userTransaction = (SeamTransaction) beanManager.getReference(bean, SeamTransaction.class, ctx);
                }
            }
        }
    }

    /**
     * registers a syncronization so that the beans can be destroyed when the
     * transaction ends
     */
    private void registerSyncronization() {
        TransactionScopeData data = contextData.get();
        if (!data.isSyncronisationRegistered()) {
            data.setSyncronisationRegistered(true);
            userTransaction.registerSynchronization(this);
        }
    }

    public <T> T get(Contextual<T> contextual) {
        lazyInitialization();
        registerSyncronization();
        String id = identifierStore.getId(contextual);
        Map<String, Object> map = contextData.get().getInstanceStore();
        return (T) map.get(id);
    }

    public <T> T get(Contextual<T> contextual, CreationalContext<T> creationalContext) {
        lazyInitialization();
        registerSyncronization();
        String id = identifierStore.getId(contextual);
        TransactionScopeData data = contextData.get();
        T instance = (T) data.getInstanceStore().get(id);
        if (instance == null) {
            instance = contextual.create(creationalContext);
            data.getCreationalContexts().put(id, creationalContext);
            data.getInstanceStore().put(id, instance);
        }
        return instance;
    }

    public Class<? extends Annotation> getScope() {
        return TransactionScoped.class;
    }

    public boolean isActive() {
        lazyInitialization();
        try {
            return userTransaction.isActive();
        } catch (SystemException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * the transaction is done, destory the beans
     */
    public void afterCompletion(int status) {
        TransactionScopeData data = contextData.get();
        for (Entry<String, Object> e : data.getInstanceStore().entrySet()) {
            Contextual contextual = identifierStore.getContextual(e.getKey());
            CreationalContext<?> ctx = data.getCreationalContexts().get(e.getKey());
            contextual.destroy(e.getValue(), ctx);
            ctx.release();
        }
        contextData.remove();
    }

    public void beforeCompletion() {

    }

    private class TransactionScopeData {
        private final Map<String, Object> instanceStore = new HashMap<String, Object>();
        private final Map<String, CreationalContext<?>> creationalContexts = new HashMap<String, CreationalContext<?>>();
        private boolean syncronisationRegistered;

        public boolean isSyncronisationRegistered() {
            return syncronisationRegistered;
        }

        public void setSyncronisationRegistered(boolean syncronisationRegistered) {
            this.syncronisationRegistered = syncronisationRegistered;
        }

        public Map<String, Object> getInstanceStore() {
            return instanceStore;
        }

        public Map<String, CreationalContext<?>> getCreationalContexts() {
            return creationalContexts;
        }

    }

}
