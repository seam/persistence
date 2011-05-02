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

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.util.EJBContextUtils;
import org.jboss.seam.persistence.util.NamingUtils;
import org.jboss.seam.solder.bean.defaultbean.DefaultBean;

/**
 * Supports injection of a Seam UserTransaction object that wraps the current
 * JTA transaction or EJB container managed transaction.
 *
 * @author Stuart Douglas
 */
@DefaultBean(SeamTransaction.class)
@DefaultTransaction
public class DefaultSeamTransaction implements SeamTransaction {
    @Inject
    private Synchronizations synchronizations;

    public void enlist(EntityManager entityManager) throws SystemException {
        getSeamTransaction().enlist(entityManager);
    }

    public boolean isActive() throws SystemException {
        return getSeamTransaction().isActive();
    }

    public boolean isActiveOrMarkedRollback() throws SystemException {
        return getSeamTransaction().isActiveOrMarkedRollback();
    }

    public boolean isCommitted() throws SystemException {
        return getSeamTransaction().isCommitted();
    }

    public boolean isConversationContextRequired() {
        return getSeamTransaction().isConversationContextRequired();
    }

    public boolean isMarkedRollback() throws SystemException {
        return getSeamTransaction().isMarkedRollback();
    }

    public boolean isNoTransaction() throws SystemException {
        return getSeamTransaction().isNoTransaction();
    }

    public boolean isRolledBack() throws SystemException {
        return getSeamTransaction().isRolledBack();
    }

    public boolean isRolledBackOrMarkedRollback() throws SystemException {
        return getSeamTransaction().isRolledBackOrMarkedRollback();
    }

    public void registerSynchronization(Synchronization sync) {
        getSeamTransaction().registerSynchronization(sync);
    }

    public void begin() throws NotSupportedException, SystemException {
        getSeamTransaction().begin();
    }

    public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException {
        getSeamTransaction().commit();
    }

    public int getStatus() throws SystemException {
        return getSeamTransaction().getStatus();
    }

    public void rollback() throws IllegalStateException, SecurityException, SystemException {
        getSeamTransaction().rollback();
    }

    public void setRollbackOnly() throws IllegalStateException, SystemException {
        getSeamTransaction().setRollbackOnly();
    }

    public void setTransactionTimeout(int seconds) throws SystemException {
        getSeamTransaction().setTransactionTimeout(seconds);
    }

    protected SeamTransaction getSeamTransaction() {
        try {
            return createUTTransaction();
        } catch (NameNotFoundException nnfe) {
            try {
                return createCMTTransaction();
            } catch (NameNotFoundException nnfe2) {
                return createNoTransaction();
            } catch (NamingException e) {
                throw new RuntimeException(e);
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
    }

    protected SeamTransaction createNoTransaction() {
        return new NoTransaction();
    }

    protected SeamTransaction createCMTTransaction() throws NamingException {
        return new CMTTransaction(EJBContextUtils.getEJBContext(), synchronizations);
    }

    protected SeamTransaction createUTTransaction() throws NamingException {
        return new UTTransaction(getUserTransaction(), synchronizations);
    }

    protected javax.transaction.UserTransaction getUserTransaction() throws NamingException {
        InitialContext context = NamingUtils.getInitialContext();
        try {
            return (javax.transaction.UserTransaction) context.lookup("java:comp/UserTransaction");
        } catch (NameNotFoundException nnfe) {
            try {
                // Embedded JBoss has no java:comp/UserTransaction
                javax.transaction.UserTransaction ut = (javax.transaction.UserTransaction) context.lookup("UserTransaction");
                ut.getStatus(); // for glassfish, which can return an unusable UT
                return ut;
            } catch (Exception e) {
                throw nnfe;
            }
        }
    }
}
