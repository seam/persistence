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

import javax.ejb.ApplicationException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs work in a JTA transaction.
 * 
 * @author Gavin King
 */
public abstract class Work<T>
{
   private static final Logger log = LoggerFactory.getLogger(Work.class);

   protected abstract T work() throws Exception;

   protected boolean isNewTransactionRequired(boolean transactionActive)
   {
      return !transactionActive;
   }

   public final T workInTransaction(org.jboss.seam.persistence.transaction.UserTransaction transaction) throws Exception
   {
      boolean transactionActive = transaction.isActiveOrMarkedRollback() || transaction.isRolledBack();
      // TODO: temp workaround, what should we really do in this case??
      boolean newTransactionRequired = isNewTransactionRequired(transactionActive);
      UserTransaction userTransaction = newTransactionRequired ? transaction : null;

      try
      {
         if (newTransactionRequired)
         {
            log.debug("beginning transaction");
            userTransaction.begin();
         }

         T result = work();
         if (newTransactionRequired)
         {
            if (transaction.isMarkedRollback())
            {
               log.debug("rolling back transaction");
               userTransaction.rollback();
            }
            else
            {
               log.debug("committing transaction");
               userTransaction.commit();
            }
         }
         return result;
      }
      catch (Exception e)
      {
         if (newTransactionRequired && userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION )
         {
            if(isRollbackRequired(e, true))
            {
               log.debug("rolling back transaction");
               userTransaction.rollback();
            }
            else
            {
               log.debug("committing transaction after ApplicationException(rollback=false):" + e.getMessage());
               userTransaction.commit();
            }
         }
         else if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION && isRollbackRequired(e, true))
         {
            userTransaction.setRollbackOnly();
         }

         throw e;
      }

   }

   public static boolean isRollbackRequired(Exception e, boolean isJavaBean)
   {
      Class<? extends Exception> clazz = e.getClass();
      return (isSystemException(e, isJavaBean, clazz)) || (clazz.isAnnotationPresent(ApplicationException.class) && clazz.getAnnotation(ApplicationException.class).rollback());
   }

   private static boolean isSystemException(Exception e, boolean isJavaBean, Class<? extends Exception> clazz)
   {
      return isJavaBean && (e instanceof RuntimeException) && !clazz.isAnnotationPresent(ApplicationException.class);
      // &&
      // TODO: this is hackish, maybe just turn off RollackInterceptor for
      // @Converter/@Validator components
      // !JSF.VALIDATOR_EXCEPTION.isInstance(e) &&
      // !JSF.CONVERTER_EXCEPTION.isInstance(e);
   }
}
