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
package org.jboss.seam.transaction;

import javax.transaction.Status;

import org.jboss.seam.persistence.util.ExceptionUtil;
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

   public final T workInTransaction(org.jboss.seam.transaction.SeamTransaction transaction) throws Exception
   {
      boolean transactionActive = transaction.isActiveOrMarkedRollback() || transaction.isRolledBack();
      // TODO: temp workaround, what should we really do in this case??
      boolean newTransactionRequired = isNewTransactionRequired(transactionActive);

      try
      {
         if (newTransactionRequired)
         {
            log.debug("beginning transaction");
            transaction.begin();
         }

         T result = work();
         if (newTransactionRequired)
         {
            if (transaction.isMarkedRollback())
            {
               log.debug("rolling back transaction");
               transaction.rollback();
            }
            else
            {
               log.debug("committing transaction");
               transaction.commit();
            }
         }
         return result;
      }
      catch (Exception e)
      {
         if (newTransactionRequired && transaction.getStatus() != Status.STATUS_NO_TRANSACTION)
         {
            if (ExceptionUtil.exceptionCausesRollback(e))
            {
               log.debug("rolling back transaction");
               transaction.rollback();
            }
            else
            {
               log.debug("committing transaction after ApplicationException(rollback=false):" + e.getMessage());
               transaction.commit();
            }
         }
         else if (transaction.getStatus() != Status.STATUS_NO_TRANSACTION && ExceptionUtil.exceptionCausesRollback(e))
         {
            transaction.setRollbackOnly();
         }
         throw e;
      }
   }

}
