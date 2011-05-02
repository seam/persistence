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

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.logging.Logger;
import org.jboss.seam.solder.core.Veto;

/**
 * Support for the Hibernate transaction API.
 *
 * @author Stuart Douglas
 */
@RequestScoped
@DefaultTransaction
@Veto
public class HibernateTransaction extends AbstractUserTransaction implements Synchronization {
    private static final Logger log = Logger.getLogger(HibernateTransaction.class);

    @Inject
    private Session session;

    private boolean rollbackOnly; // Hibernate Transaction doesn't have a
    // "rollback only" state

    private boolean synchronizationRegistered = false;

    @Inject
    public void init(Synchronizations sync) {
        setSynchronizations(sync);
    }

    public HibernateTransaction() {
    }

    private Transaction getDelegate() {
        return session.getTransaction();
    }

    public void begin() throws NotSupportedException, SystemException {
        log.debug("beginning JPA resource-local transaction");
        // TODO: translate exceptions that occur into the correct JTA exception
        try {
            getDelegate().begin();
            getSynchronizations().afterTransactionBegin();
            // use hibernate to manage the synchronizations
            // that way even if the user commits the transaction
            // themselves they will still be handled
            getDelegate().registerSynchronization(this);
            synchronizationRegistered = true;
        } catch (RuntimeException re) {
            throw re;
        }
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        log.debug("committing JPA resource-local transaction");
        Transaction delegate = getDelegate();
        boolean success = false;
        boolean tempSynchronizationRegistered = synchronizationRegistered;
        try {
            if (delegate.isActive()) {
                if (!rollbackOnly) {
                    if (!tempSynchronizationRegistered) {
                        // should only occur if the user started the transaction
                        // directly through the session
                        getSynchronizations().beforeTransactionCommit();
                    }
                    delegate.commit();
                    success = true;
                } else {
                    rollback();
                }
            }
        } finally {
            if (!tempSynchronizationRegistered) {
                getSynchronizations().afterTransactionCompletion(success);
            }
        }
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        log.debug("rolling back JPA resource-local transaction");
        // TODO: translate exceptions that occur into the correct JTA exception
        Transaction delegate = getDelegate();
        rollbackOnly = false;
        boolean tempSynchronizationRegistered = synchronizationRegistered;
        delegate.rollback();
        if (!tempSynchronizationRegistered) {
            getSynchronizations().afterTransactionCompletion(false);
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        log.debug("marking JPA resource-local transaction for rollback");
        rollbackOnly = true;
    }

    public int getStatus() throws SystemException {
        if (getDelegate().isActive()) {
            if (rollbackOnly) {
                return Status.STATUS_MARKED_ROLLBACK;
            }
            return Status.STATUS_ACTIVE;
        } else {
            return Status.STATUS_NO_TRANSACTION;
        }
    }

    public void setTransactionTimeout(int timeout) throws SystemException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void registerSynchronization(Synchronization sync) {
        if (log.isDebugEnabled()) {
            log.debug("registering synchronization: " + sync);
        }
        getDelegate().registerSynchronization(sync);
    }

    @Override
    public boolean isConversationContextRequired() {
        return true;
    }

    @Override
    public void enlist(EntityManager entityManager) {
        throw new RuntimeException("You should not try and enlist an EntityManager in a HibernateTransaction, use EntityTransaction or JTA instead");
    }

    public void afterCompletion(int status) {
        boolean success = Status.STATUS_COMMITTED == status;
        getSynchronizations().afterTransactionCompletion(success);
        rollbackOnly = false;
        synchronizationRegistered = false;
    }

    public void beforeCompletion() {
        getSynchronizations().beforeTransactionCommit();
    }

}
