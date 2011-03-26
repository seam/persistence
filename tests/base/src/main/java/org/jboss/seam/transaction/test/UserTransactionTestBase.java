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
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.transaction.DefaultTransaction;
import org.jboss.seam.transaction.SeamTransaction;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class UserTransactionTestBase
{

   public static Class<?>[] getTestClasses()
   {
      return new Class[] { UserTransactionTestBase.class, Hotel.class, HelloService.class };
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @PersistenceContext
   EntityManager em;

   @Test
   public void userTransactionTest() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      transaction.begin();
      em.joinTransaction();
      Hotel h = new Hotel("test", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.commit();
      em.clear();

      transaction.begin();
      em.joinTransaction();
      h = new Hotel("test2", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.rollback();
      em.clear();

      transaction.begin();
      em.joinTransaction();
      List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
      Assert.assertTrue(hotels.size() == 1);
      transaction.rollback();
      em.clear();

   }

   @Test
   public void synchronizationsTest() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      TransactionAware sync = new TransactionAware();
      transaction.begin();
      transaction.registerSynchronization(sync);
      transaction.commit();
      Assert.assertEquals(1, sync.getAfterCompletionCount());
      Assert.assertEquals(1, sync.getBeforeCompletionCount());

      transaction.begin();
      transaction.registerSynchronization(sync);
      transaction.commit();
      Assert.assertEquals(2, sync.getAfterCompletionCount());
      Assert.assertEquals(2, sync.getBeforeCompletionCount());

   }

   private static class TransactionAware implements Synchronization
   {
      int beforeCompletionCount = 0;
      int afterCompletionCount = 0;

      public void afterCompletion(int status)
      {
         afterCompletionCount++;
      }

      public void beforeCompletion()
      {
         beforeCompletionCount++;
      }

      public int getAfterCompletionCount()
      {
         return afterCompletionCount;
      }

      public int getBeforeCompletionCount()
      {
         return beforeCompletionCount;
      }

      public void clear()
      {
         beforeCompletionCount = 0;
         afterCompletionCount = 0;
      }

   }

}
