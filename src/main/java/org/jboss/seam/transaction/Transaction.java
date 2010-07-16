package org.jboss.seam.transaction;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

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


