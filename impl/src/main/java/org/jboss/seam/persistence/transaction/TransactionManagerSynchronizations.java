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

import javax.ejb.Remove;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.jboss.seam.solder.bean.defaultbean.DefaultBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronizations implementation that registers syncronizations with a JTA
 * {@link TransactionManager}
 * 
 */
@ApplicationScoped
@DefaultBean(Synchronizations.class)
public class TransactionManagerSynchronizations implements Synchronization, Synchronizations
{
   private static final Logger log = LoggerFactory.getLogger(TransactionManagerSynchronizations.class);

   private final String[] JNDI_LOCATIONS = { "java:/TransactionManager", "java:appserver/TransactionManager", "java:comp/TransactionManager", "java:pm/TransactionManager" };

   /**
    * The location that the TM was found under JNDI. This is static, as it will
    * not change between deployed apps on the same JVM
    */
   private static volatile String foundJndiLocation;

   @Inject
   private BeanManager beanManager;


   protected ThreadLocalStack<SynchronizationRegistry> synchronizations = new ThreadLocalStack<SynchronizationRegistry>();

   protected ThreadLocalStack<Transaction> transactions = new ThreadLocalStack<Transaction>();

   public void beforeCompletion()
   {
      log.debug("beforeCompletion");
      SynchronizationRegistry sync = synchronizations.peek();
      sync.beforeTransactionCompletion();
   }

   public void afterCompletion(int status)
   {
      transactions.pop();
      log.debug("afterCompletion");
      synchronizations.pop().afterTransactionCompletion((Status.STATUS_COMMITTED & status) == 0);
   }

   public boolean isAwareOfContainerTransactions()
   {
      return true;
   }

   public void registerSynchronization(Synchronization sync)
   {
      try
      {
         TransactionManager manager = getTransactionManager();
         Transaction transaction = manager.getTransaction();
         if (transactions.isEmpty() || transactions.peek().equals(transaction))
         {
            transactions.push(transaction);
            synchronizations.push(new SynchronizationRegistry(beanManager));
            transaction.registerSynchronization(this);
         }
         synchronizations.peek().registerSynchronization(sync);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   @Remove
   public void destroy()
   {
   }

   public TransactionManager getTransactionManager()
   {
      if (foundJndiLocation != null)
      {
         try
         {
            return (TransactionManager) new InitialContext().lookup(foundJndiLocation);
         }
         catch (NamingException e)
         {
            log.trace("Could not find transaction manager under" + foundJndiLocation);
         }
      }
      for (String location : JNDI_LOCATIONS)
      {
         try
         {
            TransactionManager manager = (TransactionManager) new InitialContext().lookup(location);
            foundJndiLocation = location;
            return manager;
         }
         catch (NamingException e)
         {
            log.trace("Could not find transaction manager under" + location);
         }
      }
      throw new RuntimeException("Could not find TransactionManager in JNDI");
   }

   @Override
   public void afterTransactionBegin()
   {

   }

   @Override
   public void afterTransactionCompletion(boolean success)
   {

   }

   @Override
   public void beforeTransactionCommit()
   {

   }
}
