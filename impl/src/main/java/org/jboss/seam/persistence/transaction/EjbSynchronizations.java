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

import java.rmi.RemoteException;
import java.util.LinkedList;

import javax.ejb.EJBException;
import javax.ejb.Remove;
import javax.ejb.SessionSynchronization;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.transaction.Synchronization;

import org.jboss.weld.extensions.defaultbean.DefaultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Receives JTA transaction completion notifications from the EJB container, and
 * passes them on to the registered Synchronizations. This implementation is
 * fully aware of container managed transactions and is able to register
 * Synchronizations for the container transaction.
 * 
 * @author Gavin King
 * 
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@DefaultBean(Synchronizations.class)
public class EjbSynchronizations implements LocalEjbSynchronizations, SessionSynchronization
{
   private static final Logger log = LoggerFactory.getLogger(EjbSynchronizations.class);

   @Inject
   private BeanManager beanManager;

   // maintain two lists to work around a bug in JBoss EJB3 where a new
   // SessionSynchronization
   // gets registered each time the bean is called
   protected LinkedList<SynchronizationRegistry> synchronizations = new LinkedList<SynchronizationRegistry>();
   protected LinkedList<SynchronizationRegistry> committing = new LinkedList<SynchronizationRegistry>();

   public void afterBegin()
   {
      log.debug("afterBegin");
      synchronizations.addLast(new SynchronizationRegistry(beanManager));
   }

   public void beforeCompletion() throws EJBException, RemoteException
   {
      log.debug("beforeCompletion");
      SynchronizationRegistry sync = synchronizations.removeLast();
      sync.beforeTransactionCompletion();
      committing.addLast(sync);
   }

   public void afterCompletion(boolean success) throws EJBException, RemoteException
   {
      log.debug("afterCompletion");
      if (committing.isEmpty())
      {
         if (success)
         {
            throw new IllegalStateException("beforeCompletion was never called");
         }
         else
         {
            synchronizations.removeLast().afterTransactionCompletion(false);
         }
      }
      else
      {
         committing.removeFirst().afterTransactionCompletion(success);
      }
   }

   public boolean isAwareOfContainerTransactions()
   {
      return true;
   }

   public void afterTransactionBegin()
   {
      // noop, let JTA notify us
   }

   public void afterTransactionCommit(boolean success)
   {
      // noop, let JTA notify us
   }

   public void afterTransactionRollback()
   {
      // noop, let JTA notify us
   }

   public void beforeTransactionCommit()
   {
      // noop, let JTA notify us
   }

   public void registerSynchronization(Synchronization sync)
   {
      synchronizations.getLast().registerSynchronization(sync);
   }

   @Remove
   public void destroy()
   {
   }

}
