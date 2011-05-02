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

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.inject.spi.BeanManager;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.jboss.logging.Logger;

/**
 * A list of Synchronizations to be invoked before and after transaction
 * completion. This class is used when we can't register a synchronization
 * directly with JTA.
 *
 * @author Gavin King
 */
class SynchronizationRegistry {

    private final BeanManager beanManager;

    public SynchronizationRegistry(BeanManager beanManager) {
        this.beanManager = beanManager;
    }

    private static final Logger log = Logger.getLogger(SynchronizationRegistry.class);

    private List<Synchronization> synchronizations = new ArrayList<Synchronization>();

    void registerSynchronization(Synchronization sync) {
        synchronizations.add(sync);
    }

    void afterTransactionCompletion(boolean success) {
        for (Synchronization sync : synchronizations) {
            try {
                sync.afterCompletion(success ? Status.STATUS_COMMITTED : Status.STATUS_ROLLEDBACK);
            } catch (Exception e) {
                log.error("Exception processing transaction Synchronization after completion", e);
            }
        }
        synchronizations.clear();
    }

    void beforeTransactionCompletion() {
        for (Synchronization sync : synchronizations) {
            try {
                sync.beforeCompletion();
            } catch (Exception e) {
                log.error("Exception processing transaction Synchronization before completion", e);
            }
        }
    }

}
