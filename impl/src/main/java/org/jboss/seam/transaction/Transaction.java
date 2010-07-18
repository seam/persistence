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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

import org.jboss.seam.persistence.transaction.UserTransaction;
import org.jboss.weld.extensions.managedproducer.ManagedProducer;

/**
 * Supports injection of a Seam UserTransaction object that wraps the current
 * JTA transaction or EJB container managed transaction.
 * 
 * @author Mike Youngstrom
 * @author Gavin King
 * @author Stuart Douglas
 * 
 */
@ApplicationScoped
public class Transaction
{

   @Inject
   Synchronizations synchronizations;

   @ManagedProducer
   @TransactionQualifier
   public UserTransaction getTransaction() throws NamingException
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
      }
   }

   protected UserTransaction createNoTransaction()
   {
      return new NoTransaction();
   }

   protected UserTransaction createCMTTransaction() throws NamingException
   {
      return new CMTTransaction(EJB.getEJBContext(), synchronizations);
   }

   protected UserTransaction createUTTransaction() throws NamingException
   {
      return new UTTransaction(getUserTransaction(), synchronizations);
   }

   protected javax.transaction.UserTransaction getUserTransaction() throws NamingException
   {
      InitialContext context = Naming.getInitialContext();
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


