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

import javax.ejb.Remove;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.logging.Logger;
import org.jboss.seam.solder.bean.defaultbean.DefaultBean;

/**
 * Synchronizations implementation that registers synchronizations with a JTA
 * {@link TransactionManager}
 */
@ApplicationScoped
@DefaultBean(Synchronizations.class)
public class TransactionManagerSynchronizations implements Synchronization, Synchronizations {
    private static final Logger log = Logger.getLogger(TransactionManagerSynchronizations.class);

    private final String[] JNDI_LOCATIONS = {"java:/TransactionManager", "java:appserver/TransactionManager", "java:comp/TransactionManager", "java:pm/TransactionManager"};

    /**
     * The location that the TM was found under JNDI. This is static, as it will
     * not change between deployed apps on the same JVM
     */
    private static volatile String foundJndiLocation;

    @Inject
    private BeanManager beanManager;


    protected ThreadLocalStack<SynchronizationRegistry> synchronizations = new ThreadLocalStack<SynchronizationRegistry>();

    protected ThreadLocalStack<Transaction> transactions = new ThreadLocalStack<Transaction>();

    public void beforeCompletion() {
        log.debug("beforeCompletion");
        SynchronizationRegistry sync = synchronizations.peek();
        sync.beforeTransactionCompletion();
    }

    public void afterCompletion(int status) {
        transactions.pop();
        log.debug("afterCompletion");
        synchronizations.pop().afterTransactionCompletion((Status.STATUS_COMMITTED & status) == 0);
    }

    public boolean isAwareOfContainerTransactions() {
        return true;
    }

    public void registerSynchronization(Synchronization sync) {
        try {
            TransactionManager manager = getTransactionManager();
            Transaction transaction = manager.getTransaction();
            if (transactions.isEmpty() || transactions.peek().equals(transaction)) {
                transactions.push(transaction);
                synchronizations.push(new SynchronizationRegistry(beanManager));
                transaction.registerSynchronization(this);
            }
            synchronizations.peek().registerSynchronization(sync);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Remove
    public void destroy() {
    }

    public TransactionManager getTransactionManager() {
        if (foundJndiLocation != null) {
            try {
                return (TransactionManager) new InitialContext().lookup(foundJndiLocation);
            } catch (NamingException e) {
                log.trace("Could not find transaction manager under" + foundJndiLocation);
            }
        }
        for (String location : JNDI_LOCATIONS) {
            try {
                TransactionManager manager = (TransactionManager) new InitialContext().lookup(location);
                foundJndiLocation = location;
                return manager;
            } catch (NamingException e) {
                log.trace("Could not find transaction manager under" + location);
            }
        }
        throw new RuntimeException("Could not find TransactionManager in JNDI");
    }

    @Override
    public void afterTransactionBegin() {

    }

    @Override
    public void afterTransactionCompletion(boolean success) {

    }

    @Override
    public void beforeTransactionCommit() {

    }
}
