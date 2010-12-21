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

import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.solder.core.Veto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps JTA transaction management in a Seam UserTransaction interface.
 * 
 * @author Mike Youngstrom
 * @author Gavin King
 * 
 */
@Veto
public class UTTransaction extends AbstractUserTransaction
{
   private static final Logger log = LoggerFactory.getLogger(UTTransaction.class);

   private final javax.transaction.UserTransaction delegate;

   UTTransaction(javax.transaction.UserTransaction delegate, Synchronizations sync)
   {
      this.setSynchronizations(sync);
      this.delegate = delegate;
      if (delegate == null)
      {
         throw new IllegalArgumentException("null UserTransaction");
      }
   }

   public void begin() throws NotSupportedException, SystemException
   {
      log.debug("beginning JTA transaction");
      delegate.begin();
      getSynchronizations().afterTransactionBegin();
   }

   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      log.debug("committing JTA transaction");
      boolean success = false;
      Synchronizations synchronizations = getSynchronizations();
      synchronizations.beforeTransactionCommit();
      try
      {
         delegate.commit();
         success = true;
      }
      finally
      {
         synchronizations.afterTransactionCompletion(success);
      }
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      log.debug("rolling back JTA transaction");
      try
      {
         delegate.rollback();
      }
      finally
      {
         getSynchronizations().afterTransactionCompletion(false);
      }
   }

   public int getStatus() throws SystemException
   {
      return delegate.getStatus();
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      delegate.setRollbackOnly();
   }

   public void setTransactionTimeout(int timeout) throws SystemException
   {
      delegate.setTransactionTimeout(timeout);
   }

   @Override
   public void registerSynchronization(Synchronization sync)
   {
      getSynchronizations().registerSynchronization(sync);
   }

}
