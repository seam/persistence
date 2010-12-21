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

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jboss.seam.solder.core.Veto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Support for the Hibernate transaction API.
 * 
 * 
 * @author Stuart Douglas
 * 
 */
@RequestScoped
@DefaultTransaction
@Veto
public class HibernateTransaction extends AbstractUserTransaction implements Synchronization
{
   private static final Logger log = LoggerFactory.getLogger(HibernateTransaction.class);

   @Inject
   private Session session;

   private boolean rollbackOnly; // Hibernate Transaction doesn't have a
                                 // "rollback only" state

   private boolean synchronizationRegistered = false;

   @Inject
   public void init(Synchronizations sync)
   {
      setSynchronizations(sync);
   }

   public HibernateTransaction()
   {
   }

   private Transaction getDelegate()
   {
      return session.getTransaction();
   }

   public void begin() throws NotSupportedException, SystemException
   {
      log.debug("beginning JPA resource-local transaction");
      // TODO: translate exceptions that occur into the correct JTA exception
      try
      {
         getDelegate().begin();
         getSynchronizations().afterTransactionBegin();
         // use hibernate to manage the synchronizations
         // that way even if the user commits the transaction
         // themselves they will still be handled
         getDelegate().registerSynchronization(this);
         synchronizationRegistered = true;
      }
      catch (RuntimeException re)
      {
         throw re;
      }
   }

   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      log.debug("committing JPA resource-local transaction");
      Transaction delegate = getDelegate();
      boolean success = false;
      boolean tempSynchronizationRegistered = synchronizationRegistered;
      try
      {
         if (delegate.isActive())
         {
            if (!rollbackOnly)
            {
               if (!tempSynchronizationRegistered)
               {
                  // should only occur if the user started the transaction
                  // directly through the session
                  getSynchronizations().beforeTransactionCommit();
               }
               delegate.commit();
               success = true;
            }
            else
            {
               rollback();
            }
         }
      }
      finally
      {
         if (!tempSynchronizationRegistered)
         {
            getSynchronizations().afterTransactionCompletion(success);
         }
      }
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      log.debug("rolling back JPA resource-local transaction");
      // TODO: translate exceptions that occur into the correct JTA exception
      Transaction delegate = getDelegate();
      rollbackOnly = false;
      boolean tempSynchronizationRegistered = synchronizationRegistered;
      delegate.rollback();
      if (!tempSynchronizationRegistered)
      {
         getSynchronizations().afterTransactionCompletion(false);
      }
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      log.debug("marking JPA resource-local transaction for rollback");
      rollbackOnly = true;
   }

   public int getStatus() throws SystemException
   {
      if (getDelegate().isActive())
      {
         if (rollbackOnly)
         {
            return Status.STATUS_MARKED_ROLLBACK;
         }
         return Status.STATUS_ACTIVE;
      }
      else
      {
         return Status.STATUS_NO_TRANSACTION;
      }
   }

   public void setTransactionTimeout(int timeout) throws SystemException
   {
      throw new UnsupportedOperationException();
   }

   @Override
   public void registerSynchronization(Synchronization sync)
   {
      if (log.isDebugEnabled())
      {
         log.debug("registering synchronization: " + sync);
      }
      getDelegate().registerSynchronization(sync);
   }

   @Override
   public boolean isConversationContextRequired()
   {
      return true;
   }

   @Override
   public void enlist(EntityManager entityManager)
   {
      throw new RuntimeException("You should not try and enlist an EntityManager in a HibernateTransaction, use EntityTransaction or JTA instead");
   }

   public void afterCompletion(int status)
   {
      boolean success = Status.STATUS_COMMITTED == status;
      getSynchronizations().afterTransactionCompletion(success);
      rollbackOnly = false;
      synchronizationRegistered = false;
   }

   public void beforeCompletion()
   {
      getSynchronizations().beforeTransactionCommit();
   }

}
