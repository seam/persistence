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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.ConversationScoped;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

import org.jboss.solder.logging.Logger;

/**
 * Maintains the set of persistence contexts that have been touched in a
 * conversation. Also controls the flush mode used by the persistence contexts
 * during the render phase.
 *
 * @author Gavin King
 * @author Stuart Douglas
 */
@ConversationScoped
public class PersistenceContextsImpl implements Serializable, PersistenceContexts {
    private static final long serialVersionUID = -4897350516435283182L;

    private static final Logger log = Logger.getLogger(PersistenceContextsImpl.class);

    /**
     * persistences contexts are referenced by their qualifiers
     */
    private final Set<PersistenceContextDefintition> set = new HashSet<PersistenceContextDefintition>();

    private FlushModeType currentFlushMode;

    // the real flush mode is a backup of the flush mode when doing a temporary
    // switch (such as during render)
    private FlushModeType defaultFlushMode;

    @Inject
    @Any
    private Instance<ManagedPersistenceContext> persistenceContexts;

    @Inject
    public void create(FlushModeManager manager) {
        FlushModeType defaultFlushMode = manager.getFlushModeType();
        if (defaultFlushMode != null) {
            currentFlushMode = defaultFlushMode;
        } else {
            currentFlushMode = FlushModeType.AUTO;
        }

        this.defaultFlushMode = currentFlushMode;
    }

    public FlushModeType getFlushMode() {
        return currentFlushMode;
    }

    public Set<PersistenceContextDefintition> getTouchedContexts() {
        return Collections.unmodifiableSet(set);
    }

    public void touch(ManagedPersistenceContext context) {
        set.add(new PersistenceContextDefintition(context.getQualifiers(), context.getBeanType()));
    }

    public void untouch(ManagedPersistenceContext context) {
        set.remove(new PersistenceContextDefintition(context.getQualifiers(), context.getBeanType()));
    }

    public void changeFlushMode(FlushModeType flushMode) {
        this.currentFlushMode = flushMode;
        changeFlushModes();
    }

    public void restoreFlushMode() {
        if (defaultFlushMode != null && defaultFlushMode != currentFlushMode) {
            currentFlushMode = defaultFlushMode;
//            defaultFlushMode = null;
            changeFlushModes();
        }
    }

    private void changeFlushModes() {
        for (ManagedPersistenceContext context : persistenceContexts) {
            if (set.contains(new PersistenceContextDefintition(context.getQualifiers(), context.getBeanType()))) {
                try {
                    context.changeFlushMode(currentFlushMode);
                } catch (UnsupportedOperationException uoe) {
                    // we won't be nasty and throw and exception, but we'll log a
                    // warning to the developer
                    log.warn(uoe.getMessage());
                }
            }
        }
    }

    public void beforeRender() {
        for (ManagedPersistenceContext context : persistenceContexts) {
            if (set.contains(new PersistenceContextDefintition(context.getQualifiers(), context.getBeanType()))) {
                try {
                    currentFlushMode = context.getProvider().getRenderFlushMode();
                    context.changeFlushMode(currentFlushMode);
                } catch (UnsupportedOperationException uoe) {
                    // we won't be nasty and throw and exception, but we'll log a
                    // warning to the developer
                    log.warn(uoe.getMessage());
                }
            }
        }
    }

    public void afterRender() {
        restoreFlushMode();
    }

    public static class PersistenceContextDefintition {
        private final Set<Annotation> qualifiers;
        private final Class<?> type;

        public PersistenceContextDefintition(Set<Annotation> qualifiers, Class<?> type) {
            this.qualifiers = new HashSet<Annotation>(qualifiers);
            this.type = type;
        }

        public Set<Annotation> getQualifiers() {
            return qualifiers;
        }

        public Class<?> getType() {
            return type;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((qualifiers == null) ? 0 : qualifiers.hashCode());
            result = prime * result + ((type == null) ? 0 : type.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            PersistenceContextDefintition other = (PersistenceContextDefintition) obj;
            if (qualifiers == null) {
                if (other.qualifiers != null)
                    return false;
            } else if (!qualifiers.equals(other.qualifiers))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }

    }

}
