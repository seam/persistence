/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

import org.jboss.weld.extensions.core.Veto;

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
