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

import static javax.transaction.Status.STATUS_ACTIVE;
import static javax.transaction.Status.STATUS_COMMITTED;
import static javax.transaction.Status.STATUS_MARKED_ROLLBACK;
import static javax.transaction.Status.STATUS_NO_TRANSACTION;
import static javax.transaction.Status.STATUS_ROLLEDBACK;

import javax.persistence.EntityManager;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

/**
 * Base implementation of UserTransaction
 * 
 * @author Gavin King
 * 
 */
public abstract class AbstractUserTransaction implements SeamTransaction
{

   private Synchronizations synchronizations;

   public boolean isActive() throws SystemException
   {
      return getStatus() == STATUS_ACTIVE;
   }

   public boolean isActiveOrMarkedRollback() throws SystemException
   {
      int status = getStatus();
      return status == STATUS_ACTIVE || status == STATUS_MARKED_ROLLBACK;
   }

   public boolean isRolledBackOrMarkedRollback() throws SystemException
   {
      int status = getStatus();
      return status == STATUS_ROLLEDBACK || status == STATUS_MARKED_ROLLBACK;
   }

   public boolean isMarkedRollback() throws SystemException
   {
      return getStatus() == STATUS_MARKED_ROLLBACK;
   }

   public boolean isNoTransaction() throws SystemException
   {
      return getStatus() == STATUS_NO_TRANSACTION;
   }

   public boolean isRolledBack() throws SystemException
   {
      return getStatus() == STATUS_ROLLEDBACK;
   }

   public boolean isCommitted() throws SystemException
   {
      return getStatus() == STATUS_COMMITTED;
   }

   public boolean isConversationContextRequired()
   {
      return false;
   }

   public abstract void registerSynchronization(Synchronization sync);

   public void enlist(EntityManager entityManager) throws SystemException
   {
      if (isActiveOrMarkedRollback())
      {
         entityManager.joinTransaction();
      }
   }

   public Synchronizations getSynchronizations()
   {
      return synchronizations;
   }

   protected void setSynchronizations(Synchronizations synchronizations)
   {
      this.synchronizations = synchronizations;
   }

}
