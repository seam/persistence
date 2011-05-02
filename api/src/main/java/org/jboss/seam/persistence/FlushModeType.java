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

/**
 * A full set of flush modes, including MANUAL,
 * which is a glaring missing feature of the JPA
 * spec.
 *
 * @author Gavin King
 */
public enum FlushModeType {

    /**
     * Flushing never occurs automatically, all changes are queued
     * until the application calls flush() explicitly.
     */
    MANUAL,

    /**
     * Flushing occurs automatically at commit time and when necessary
     * before query executions.
     */
    AUTO,

    /**
     * Flushing occurs automatically at transaction commit time.
     */
    COMMIT;

    /**
     * Does this flush mode keep unflushed changes past a
     * transaction commit?
     *
     * @return false for all flush modes except for MANUAL
     */
    public boolean dirtyBetweenTransactions() {
        return this == MANUAL;
    }

}
