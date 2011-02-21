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
package org.jboss.seam.persistence.transaction;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.solder.core.Veto;

/**
 * When no kind of transaction management exists.
 * 
 * @author Mike Youngstrom
 * @author Gavin King
 * 
 */
@Veto
public class NoTransaction extends AbstractUserTransaction
{

   public NoTransaction()
   {
   }

   public void begin() throws NotSupportedException, SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   public int getStatus() throws SystemException
   {
      return Status.STATUS_NO_TRANSACTION;
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   public void setTransactionTimeout(int timeout) throws SystemException
   {
      throw new UnsupportedOperationException("no transaction");
   }

   @Override
   public void registerSynchronization(Synchronization sync)
   {
      throw new UnsupportedOperationException("no transaction");
   }

   @Override
   public void enlist(EntityManager entityManager) throws SystemException
   {
      // no-op
   }

}
