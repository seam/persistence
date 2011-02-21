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
package org.jboss.seam.persistence.transactions.test;

import javax.inject.Inject;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.junit.Test;

public class TransactionScopedTestBase
{

   public static Class<?>[] getTestClasses()
   {
      return new Class[] { TransactionScopedTestBase.class, Hotel.class, HelloService.class, TransactionScopedObject.class };
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @PersistenceUnit
   EntityManagerFactory emf;

   @Inject
   TransactionScopedObject transactionScopedObject;

   @Test
   public void transactionScopeTest() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      transaction.begin();
      transactionScopedObject.setValue(10);
      Assert.assertTrue(transactionScopedObject.getValue() == 10);
      transaction.commit();

      transaction.begin();
      Assert.assertTrue(transactionScopedObject.getValue() == 0);
      transactionScopedObject.setValue(20);
      Assert.assertTrue(transactionScopedObject.getValue() == 20);
      transaction.rollback();

      transaction.begin();
      Assert.assertTrue(transactionScopedObject.getValue() == 0);
      transaction.rollback();
   }

}
