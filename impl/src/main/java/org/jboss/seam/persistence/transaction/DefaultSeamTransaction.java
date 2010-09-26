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

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.util.EJBContextUtils;
import org.jboss.seam.persistence.util.NamingUtils;
import org.jboss.weld.extensions.defaultbean.DefaultBean;

/**
 * 
 * Supports injection of a Seam UserTransaction object that wraps the current
 * JTA transaction or EJB container managed transaction.
 * 
 * @author Stuart Douglas
 * 
 */
@DefaultBean(SeamTransaction.class)
@DefaultTransaction
public class DefaultSeamTransaction implements SeamTransaction
{
   @Inject
   private Synchronizations synchronizations;

   public void enlist(EntityManager entityManager) throws SystemException
   {
      getSeamTransaction().enlist(entityManager);
   }

   public boolean isActive() throws SystemException
   {
      return getSeamTransaction().isActive();
   }

   public boolean isActiveOrMarkedRollback() throws SystemException
   {
      return getSeamTransaction().isActiveOrMarkedRollback();
   }

   public boolean isCommitted() throws SystemException
   {
      return getSeamTransaction().isCommitted();
   }

   public boolean isConversationContextRequired()
   {
      return getSeamTransaction().isConversationContextRequired();
   }

   public boolean isMarkedRollback() throws SystemException
   {
      return getSeamTransaction().isMarkedRollback();
   }

   public boolean isNoTransaction() throws SystemException
   {
      return getSeamTransaction().isNoTransaction();
   }

   public boolean isRolledBack() throws SystemException
   {
      return getSeamTransaction().isRolledBack();
   }

   public boolean isRolledBackOrMarkedRollback() throws SystemException
   {
      return getSeamTransaction().isRolledBackOrMarkedRollback();
   }

   public void registerSynchronization(Synchronization sync)
   {
      getSeamTransaction().registerSynchronization(sync);
   }

   public void begin() throws NotSupportedException, SystemException
   {
      getSeamTransaction().begin();
   }

   public void commit() throws RollbackException, HeuristicMixedException, HeuristicRollbackException, SecurityException, IllegalStateException, SystemException
   {
      getSeamTransaction().commit();
   }

   public int getStatus() throws SystemException
   {
      return getSeamTransaction().getStatus();
   }

   public void rollback() throws IllegalStateException, SecurityException, SystemException
   {
      getSeamTransaction().rollback();
   }

   public void setRollbackOnly() throws IllegalStateException, SystemException
   {
      getSeamTransaction().setRollbackOnly();
   }

   public void setTransactionTimeout(int seconds) throws SystemException
   {
      getSeamTransaction().setTransactionTimeout(seconds);
   }

   protected SeamTransaction getSeamTransaction()
   {
      try
      {
         return createUTTransaction();
      }
      catch (NameNotFoundException nnfe)
      {
         try
         {
            return createCMTTransaction();
         }
         catch (NameNotFoundException nnfe2)
         {
            return createNoTransaction();
         }
         catch (NamingException e)
         {
            throw new RuntimeException(e);
         }
      }
      catch (NamingException e)
      {
         throw new RuntimeException(e);
      }
   }

   protected SeamTransaction createNoTransaction()
   {
      return new NoTransaction();
   }

   protected SeamTransaction createCMTTransaction() throws NamingException
   {
      return new CMTTransaction(EJBContextUtils.getEJBContext(), synchronizations);
   }

   protected SeamTransaction createUTTransaction() throws NamingException
   {
      return new UTTransaction(getUserTransaction(), synchronizations);
   }

   protected javax.transaction.UserTransaction getUserTransaction() throws NamingException
   {
      InitialContext context = NamingUtils.getInitialContext();
      try
      {
         return (javax.transaction.UserTransaction) context.lookup("java:comp/UserTransaction");
      }
      catch (NameNotFoundException nnfe)
      {
         try
         {
            // Embedded JBoss has no java:comp/UserTransaction
            javax.transaction.UserTransaction ut = (javax.transaction.UserTransaction) context.lookup("UserTransaction");
            ut.getStatus(); // for glassfish, which can return an unusable UT
            return ut;
         }
         catch (Exception e)
         {
            throw nnfe;
         }
      }
   }
}
