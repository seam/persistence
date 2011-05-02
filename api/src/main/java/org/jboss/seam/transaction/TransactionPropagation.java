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

/**
 * @author Dan Allen
 */
public enum TransactionPropagation {
    /**
     * A transaction will be started if one is not currently active.
     */
    REQUIRED,
    /**
     * A transaction will not be started if there is not one currently active,
     * however this method supports running inside an existing transaction
     */
    SUPPORTS,
    /**
     * Requires a transaction to be active. If no transaction is active an
     * {@link IllegalStateException} is thrown
     */
    MANDATORY,
    /**
     * Requires no transaction to be active. If a transaction is active an
     * {@link IllegalStateException} is thrown
     */
    NEVER;

    public boolean isNewTransactionRequired(boolean transactionActive) {
        switch (this) {
            case REQUIRED:
                return !transactionActive;
            case SUPPORTS:
                return false;
            case MANDATORY:
                if (!transactionActive) {
                    throw new IllegalStateException("No transaction active on call to MANDATORY method");
                } else {
                    return false;
                }
            case NEVER:
                if (transactionActive) {
                    throw new IllegalStateException("Transaction active on call to NEVER method");
                } else {
                    return false;
                }
            default:
                throw new IllegalArgumentException();
        }
    }
}