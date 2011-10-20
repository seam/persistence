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
import javax.persistence.EntityManagerFactory;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.test.util.ManagedPersistenceContextProvider;
import org.jboss.seam.transaction.DefaultTransaction;
import org.jboss.seam.transaction.SeamTransaction;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that injection is working for JPA entities
 *
 * @author Stuart Douglas
 */

public class EntityInjectionTestBase {

    public static Class<?>[] getTestClasses() {
        return new Class[]{HibernateSearchTestBase.class, Hotel.class, ManagedPersistenceContextProvider.class, HelloService.class, EntityInjectionTestBase.class};
    }

    @Inject
    @DefaultTransaction
    SeamTransaction transaction;

    @Inject
    EntityManagerFactory emf;

    @Test
    public void testInjectionIntoEntity() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        EntityManager em = null;
        try {
            em = emf.createEntityManager();
            transaction.begin();
            em.joinTransaction();
            Hotel h = new Hotel("Hilton", "Fake St", "Wollongong", "NSW", "2518", "Australia");
            em.persist(h);
            em.flush();
            transaction.commit();
            em.close();
            transaction.begin();
            em = emf.createEntityManager();
            em.joinTransaction();

            h = (Hotel) em.createQuery("select h from Hotel h where h.name='Hilton'").getSingleResult();
            Assert.assertTrue(h.isInitalizerCalled());
            Assert.assertEquals(h.sayHello(), "Hello");
        } finally {
            em.close();
            transaction.rollback();
        }

    }

}
