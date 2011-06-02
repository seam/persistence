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
package org.jboss.seam.transaction;

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

import org.jboss.logging.Logger;
import org.jboss.seam.solder.bean.defaultbean.DefaultBean;

/**
 * Receives JTA transaction completion notifications from the EJB container, and passes them on to the registered
 * Synchronizations. This implementation is fully aware of container managed transactions and is able to register
 * Synchronizations for the container transaction.
 * 
 * @author Gavin King
 * @author <a href="mailto:lincolnbaxter@gmail.com">Lincoln Baxter, III</a>
 * 
 */
@Stateful
@RequestScoped
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@DefaultBean(Synchronizations.class)
public class EjbSynchronizations implements LocalEjbSynchronizations, SessionSynchronization
{
   private static final Logger log = Logger.getLogger(TransactionManagerSynchronizations.class);

   @Inject
   private BeanManager beanManager;

   // maintain two lists to work around a bug in JBoss EJB3 where a new
   // SessionSynchronization
   // gets registered each time the bean is called
   protected LinkedList<SynchronizationRegistry> synchronizations = new LinkedList<SynchronizationRegistry>();
   protected LinkedList<SynchronizationRegistry> committing = new LinkedList<SynchronizationRegistry>();

   @Override
   public void afterBegin()
   {
      log.debug("afterBegin");
      synchronizations.addLast(new SynchronizationRegistry(beanManager));
   }

   @Override
   public void beforeCompletion() throws EJBException, RemoteException
   {
      log.debug("beforeCompletion");
      SynchronizationRegistry sync = synchronizations.removeLast();
      sync.beforeTransactionCompletion();
      committing.addLast(sync);
   }

   @Override
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
      }
   }

   @Override
   public boolean isAwareOfContainerTransactions()
   {
      return true;
   }

   @Override
   public void afterTransactionBegin()
   {
      // noop, let JTA notify us
   }

   @Override
   public void afterTransactionCompletion(boolean success)
   {
      // noop, let JTA notify us
   }

   @Override
   public void beforeTransactionCommit()
   {
      // noop, let JTA notify us
   }

   @Override
   public void registerSynchronization(Synchronization sync)
   {
      synchronizations.getLast().registerSynchronization(sync);
   }

   @Override
   @Remove
   public void destroy()
   {
   }

}
