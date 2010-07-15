package org.jboss.seam.transaction;

import javax.ejb.EJBContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;

/**
 * utility class to look up the EJBContext
 * 
 * 
 */
public class EJB
{
   public static String ejbContextName = "java:comp.ejb3/EJBContext";
   public static final String STANDARD_EJB_CONTEXT_NAME = "java:comp/EJBContext";

   public static EJBContext getEJBContext() throws NamingException
   {
      try
      {
         return (EJBContext) Naming.getInitialContext().lookup(ejbContextName);
      }
      catch (NameNotFoundException nnfe)
      {
         return (EJBContext) Naming.getInitialContext().lookup(STANDARD_EJB_CONTEXT_NAME);
      }
   }

   protected static String getEjbContextName()
   {
      return ejbContextName;
   }

   protected static void setEjbContextName(String ejbContextName)
   {
      EJB.ejbContextName = ejbContextName;
   }

}
