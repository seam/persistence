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
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.logging.Logger;
import org.jboss.seam.persistence.DefaultPersistenceProvider;
import org.jboss.seam.solder.core.Veto;

/**
 * Support for the JPA EntityTransaction API.
 * <p/>
 * Adapts JPA transaction management to a Seam UserTransaction interface.For use
 * in non-JTA-capable environments.
 *
 * @author Gavin King
 */
@RequestScoped
@DefaultTransaction
@Veto
public class EntityTransaction extends AbstractUserTransaction {
    private static final Logger log = Logger.getLogger(EntityTransaction.class);

    @Inject
    private EntityManager entityManager;

    @Inject
    private DefaultPersistenceProvider persistenceProvider;

    @Inject
    public void init(Synchronizations sync) {
        setSynchronizations(sync);
    }

    public EntityTransaction() {
    }

    private javax.persistence.EntityTransaction getDelegate() {
        return entityManager.getTransaction();
    }

    public void begin() throws NotSupportedException, SystemException {
        log.debug("beginning JPA resource-local transaction");
        // TODO: translate exceptions that occur into the correct JTA exception
        try {
            getDelegate().begin();
            getSynchronizations().afterTransactionBegin();
        } catch (RuntimeException re) {
            throw re;
        }
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        log.debug("committing JPA resource-local transaction");
        javax.persistence.EntityTransaction delegate = getDelegate();
        boolean success = false;
        try {
            if (delegate.getRollbackOnly()) {
                delegate.rollback();
                throw new RollbackException();
            } else {
                getSynchronizations().beforeTransactionCommit();
                delegate.commit();
                success = true;
            }
        } finally {
            getSynchronizations().afterTransactionCompletion(success);
        }
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        log.debug("rolling back JPA resource-local transaction");
        // TODO: translate exceptions that occur into the correct JTA exception
        javax.persistence.EntityTransaction delegate = getDelegate();
        try {
            delegate.rollback();
        } finally {
            getSynchronizations().afterTransactionCompletion(false);
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        log.debug("marking JPA resource-local transaction for rollback");
        getDelegate().setRollbackOnly();
    }

    public int getStatus() throws SystemException {
        if (getDelegate().isActive()) {
            if (getDelegate().getRollbackOnly()) {
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
        // try to register the synchronization directly with the
        // persistence provider, but if this fails, just hold
        // on to it myself
        if (!persistenceProvider.registerSynchronization(sync, entityManager)) {
            getSynchronizations().registerSynchronization(sync);
        }
    }

    @Override
    public boolean isConversationContextRequired() {
        return true;
    }

    @Override
    public void enlist(EntityManager entityManager) {
        // no-op
    }

}
