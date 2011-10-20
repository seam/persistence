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
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.MultiFieldQueryParser;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.util.Version;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.IndexedHotel;
import org.jboss.seam.persistence.test.util.ManagedPersistenceContextProvider;
import org.jboss.seam.transaction.DefaultTransaction;
import org.jboss.seam.transaction.SeamTransaction;
import org.junit.Assert;
import org.junit.Test;

public class HibernateSearchTestBase {
    public static Class<?>[] getTestClasses() {
        return new Class[]{IndexedHotel.class, ManagedPersistenceContextProvider.class, HelloService.class, HibernateSearchTestBase.class};
    }

    @Inject
    @DefaultTransaction
    SeamTransaction transaction;

    @Inject
    FullTextEntityManager em;

    @Test
    public void testFullTextEntityManager() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException {
        Assert.assertTrue(em instanceof FullTextEntityManager);
    }

    @Test
    public void testSearchingForHotel() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException, ParseException {
        transaction.begin();
        IndexedHotel h = new IndexedHotel("Hilton", "Fake St", "Wollongong", "NSW", "2518", "Australia");
        em.persist(h);
        em.flush();
        transaction.commit();

        transaction.begin();
        h = new IndexedHotel("Other Hotel", "Real St ", "Wollongong", "NSW", "2518", "Australia");
        em.persist(h);
        em.flush();
        transaction.commit();

        transaction.begin();
        String[] fields = new String[]{"name"};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(Version.LUCENE_30, fields, new StandardAnalyzer(Version.LUCENE_30));
        org.apache.lucene.search.Query query = parser.parse("Other");

        // wrap Lucene query in a javax.persistence.Query
        javax.persistence.Query persistenceQuery = em.createFullTextQuery(query, IndexedHotel.class);
        IndexedHotel hotel = (IndexedHotel) persistenceQuery.getSingleResult();
        Assert.assertTrue(hotel.getName().equals("Other Hotel"));
        transaction.commit();

    }
}
