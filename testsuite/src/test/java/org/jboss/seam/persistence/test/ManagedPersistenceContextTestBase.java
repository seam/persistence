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

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;
import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.test.util.ManagedPersistenceContextProvider;
import org.jboss.seam.transaction.DefaultTransaction;
import org.jboss.seam.transaction.SeamTransaction;
import org.junit.Test;

public class ManagedPersistenceContextTestBase {

    public static Class<?>[] getTestClasses() {
        return new Class[]{ManagedPersistenceContextTestBase.class, Hotel.class, ManagedPersistenceContextProvider.class, HelloService.class};
    }

    @Inject
    @DefaultTransaction
    SeamTransaction transaction;

    @Inject
    EntityManager em;

    @Test
    public void testManagedPersistenceContext() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        transaction.begin();
        Hotel h = new Hotel("test", "Fake St", "Wollongong", "NSW", "2518", "Australia");
        em.persist(h);
        em.flush();
        transaction.commit();

        transaction.begin();
        h = new Hotel("test2", "Fake St", "Wollongong", "NSW", "2518", "Australia");
        em.persist(h);
        em.flush();
        transaction.rollback();

        transaction.begin();
        List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
        Assert.assertEquals(1, hotels.size());
        transaction.rollback();
    }
    
    @Test(expected = EntityNotFoundException.class)
    public void testManagedPersistenceContextException() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        transaction.begin();
        Hotel h = new Hotel("test3", "Purkynova", "Brno", "CZ", "63100", "Czech Republic");
        em.persist(h);
        em.flush();
        transaction.commit();

        transaction.begin();
        int affected = em.createQuery("delete from Hotel h where h.name = :name").setParameter("name", h.getName()).executeUpdate();
        Assert.assertEquals(1, affected);
        transaction.commit();
        
        try {
            transaction.begin();
            em.refresh(h);
        }
        finally {
            transaction.rollback();
        }
    }
}
