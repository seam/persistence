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
package org.jboss.seam.persistence.test;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import junit.framework.Assert;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.jboss.seam.persistence.FlushModeManager;
import org.jboss.seam.persistence.FlushModeType;
import org.jboss.seam.persistence.PersistenceContexts;
import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.test.util.ManagedPersistenceContextProvider;
import org.junit.Test;

public class ManagedPersistenceContextFlushModeTestBase {
    public static Class<?>[] getTestClasses() {
        return new Class[]{ManagedPersistenceContextFlushModeTestBase.class, Hotel.class, ManagedPersistenceContextProvider.class, HelloService.class};
    }

    @Inject
    private FlushModeManager manager;

    @Inject
    private EntityManager em;

    @Inject
    private PersistenceContexts pc;

    @Test
    public void testChangedTouchedPersistenceContextFlushMode() {
        manager.setFlushModeType(FlushModeType.MANUAL);
        // test default flush mode
        Assert.assertEquals(FlushMode.MANUAL, ((Session) em.getDelegate()).getFlushMode());
        try {
            em.setFlushMode(javax.persistence.FlushModeType.AUTO);
            pc.changeFlushMode(FlushModeType.MANUAL);
            Assert.assertEquals(FlushMode.MANUAL, ((Session) em.getDelegate()).getFlushMode());
        } finally {
            em.setFlushMode(javax.persistence.FlushModeType.AUTO);
        }
    }
}
