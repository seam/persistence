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
package org.jboss.seam.transaction.test;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TransactionRequiredException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.jboss.seam.persistence.test.util.DontRollBackException;
import org.jboss.seam.persistence.test.util.EntityManagerProvider;
import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.transaction.DefaultTransaction;
import org.jboss.seam.transaction.SeamTransaction;
import org.junit.Test;

/**
 * Tests the @Transactional interceptor
 * <p/>
 * TODO: refactor the tests to share a common superclass
 *
 * @author stuart
 */
public class TransactionInterceptorTestBase {

    public static Class<?>[] getTestClasses() {
        return new Class[]{TransactionInterceptorTestBase.class, TransactionManagedBean.class, HelloService.class, Hotel.class, EntityManagerProvider.class, DontRollBackException.class};
    }

    @Inject
    TransactionManagedBean bean;

    @Inject
    @DefaultTransaction
    SeamTransaction transaction;

    @PersistenceContext
    EntityManager em;

    @Test
    public void testTransactionInterceptor() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        bean.addHotel();
        assertHotels(1);
        try {
            bean.failToAddHotel();
        } catch (Exception e) {
        }
        assertHotels(1);
        try {
            bean.addHotelWithApplicationException();
        } catch (DontRollBackException e) {
        }
        assertHotels(2);
    }

    @Test(expected = TransactionRequiredException.class)
    public void testTransactionInterceptorMethodOverrides() {
        bean.tryAndAddHotelWithNoTransaction();
    }

    public void assertHotels(int count) throws NotSupportedException, SystemException {
        transaction.begin();
        em.joinTransaction();
        List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
        Assert.assertTrue("Wrong number of hotels: " + hotels.size(), hotels.size() == count);
        transaction.rollback();
    }
}
