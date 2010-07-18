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

/**
 * @author Dan Allen
 */
public enum TransactionPropagation
{
   REQUIRED, SUPPORTS, MANDATORY, NEVER;

   public boolean isNewTransactionRequired(boolean transactionActive)
   {
      switch (this)
      {
      case REQUIRED:
         return !transactionActive;
      case SUPPORTS:
         return false;
      case MANDATORY:
         if (!transactionActive)
         {
            throw new IllegalStateException("No transaction active on call to MANDATORY method");
         }
         else
         {
            return false;
         }
      case NEVER:
         if (transactionActive)
         {
            throw new IllegalStateException("Transaction active on call to NEVER method");
         }
         else
         {
            return false;
         }
      default:
         throw new IllegalArgumentException();
      }
   }
}