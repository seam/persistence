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
package org.jboss.seam.persistence.hibernate.test;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;

import junit.framework.Assert;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.jboss.seam.persistence.FlushModeManager;
import org.jboss.seam.persistence.FlushModeType;
import org.jboss.seam.persistence.ManagedPersistenceContext;
import org.jboss.seam.persistence.PersistenceContexts;
import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.test.util.ManagedHibernateSessionProvider;
import org.junit.Test;

public class ManagedHibernateSessionFlushModeTestBase {
    public static Class<?>[] getTestClasses() {
        return new Class[]{ManagedHibernateSessionFlushModeTestBase.class, Hotel.class, ManagedHibernateSessionProvider.class, HelloService.class};
    }

    @Inject
    FlushModeManager manager;

    @Inject
    Session session;

    @Inject
    ManagedPersistenceContext context;

    @Inject
    PersistenceContexts pc;

    @Inject
    BeanManager bm;

    @Test
    public void testChangedTouchedSessionFlushMode() {
        manager.setFlushModeType(FlushModeType.MANUAL);
        Assert.assertEquals(FlushMode.MANUAL, session.getFlushMode());
        try {
            session.setFlushMode(FlushMode.AUTO);
            pc.changeFlushMode(FlushModeType.MANUAL);
            Assert.assertEquals(FlushMode.MANUAL, session.getFlushMode());
        } finally {
            session.setFlushMode(FlushMode.AUTO);
        }
    }
}
