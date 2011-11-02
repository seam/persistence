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

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.hibernate.Session;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.test.util.HotelNameProducer;
import org.jboss.seam.persistence.test.util.ManagedHibernateSessionProvider;
import org.jboss.seam.transaction.DefaultTransaction;
import org.jboss.seam.transaction.SeamTransaction;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class ManagedHibernateSessionELTestBase {

    public static Class<?>[] getTestClasses() {
        return new Class[]{ManagedHibernateSessionELTestBase.class, Hotel.class, ManagedHibernateSessionProvider.class, HotelNameProducer.class, HelloService.class};
    }

    @Inject
    @DefaultTransaction
    SeamTransaction transaction;

    @Inject
    Session session;

    @Test
    public void testELInInquery() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        transaction.begin();
        Hotel h = new Hotel("Hilton", "Fake St", "Wollongong", "NSW", "2518", "Australia");
        session.persist(h);
        session.flush();
        transaction.commit();

        transaction.begin();
        h = new Hotel("Other Hotel", "Real St ", "Wollongong", "NSW", "2518", "Australia");
        session.persist(h);
        session.flush();
        transaction.commit();

        transaction.begin();
        Hotel hilton = (Hotel) session.createQuery("select h from Hotel h where h.name=#{hotelName}").uniqueResult();
        Assert.assertTrue(hilton.getName().equals("Hilton"));
        Assert.assertTrue(hilton.getAddress().equals("Fake St"));
        transaction.commit();

    }

}
