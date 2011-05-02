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

import javax.ejb.EJBContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.jboss.seam.solder.core.Veto;

/**
 * Wraps EJBContext transaction management in a UserTransaction interface. Note
 * that container managed transactions cannot be controlled by the application,
 * so begin(), commit() and rollback() are disallowed in a CMT.
 *
 * @author Mike Youngstrom
 * @author Gavin King
 * @author Stuart Douglas
 */
@Veto
public class CMTTransaction extends AbstractUserTransaction {

    private final EJBContext ejbContext;

    public CMTTransaction(EJBContext ejbContext, Synchronizations sync) {
        setSynchronizations(sync);
        this.ejbContext = ejbContext;
        if (ejbContext == null) {
            throw new IllegalArgumentException("null EJBContext");
        }
    }

    public void begin() throws NotSupportedException, SystemException {
        ejbContext.getUserTransaction().begin();
        getSynchronizations().afterTransactionBegin();
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        UserTransaction userTransaction = ejbContext.getUserTransaction();
        boolean success = false;
        Synchronizations synchronizations = getSynchronizations();
        synchronizations.beforeTransactionCommit();
        try {
            userTransaction.commit();
            success = true;
        } finally {
            synchronizations.afterTransactionCompletion(success);
        }
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        UserTransaction userTransaction = ejbContext.getUserTransaction();
        try {
            userTransaction.rollback();
        } finally {
            getSynchronizations().afterTransactionCompletion(false);
        }
    }

    public int getStatus() throws SystemException {
        try {
            // TODO: not correct for SUPPORTS or NEVER!
            if (!ejbContext.getRollbackOnly()) {
                return Status.STATUS_ACTIVE;
            } else {
                return Status.STATUS_MARKED_ROLLBACK;
            }
        } catch (IllegalStateException ise) {
            try {
                return ejbContext.getUserTransaction().getStatus();
            } catch (IllegalStateException is) {
                return Status.STATUS_NO_TRANSACTION;
            }
        }
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        ejbContext.setRollbackOnly();
    }

    public void setTransactionTimeout(int timeout) throws SystemException {
        ejbContext.getUserTransaction().setTransactionTimeout(timeout);
    }

    @Override
    public void registerSynchronization(Synchronization sync) {
        Synchronizations synchronizations = getSynchronizations();
        if (synchronizations.isAwareOfContainerTransactions()) {
            synchronizations.registerSynchronization(sync);
        } else {
            throw new UnsupportedOperationException("cannot register synchronization with container transaction, use <transaction:ejb-transaction/>");
        }
    }

}
