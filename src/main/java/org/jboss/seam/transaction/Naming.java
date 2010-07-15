/*
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package org.jboss.seam.transaction;

import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * this has been ported to seam and hacked to make it work
 * 
 * we need to figure out what we are doing with JNDI in seam 3 and make this go
 * away
 * 
 * @author stuart
 * 
 */
public final class Naming
{
   private static final Logger log = LoggerFactory.getLogger(Naming.class);
   private static Hashtable initialContextProperties;

   private static InitialContext initialContext;

   public static InitialContext getInitialContext(Hashtable<String, String> props) throws NamingException
   {
      if (props == null)
      {
         // throw new
         // IllegalStateException("JNDI properties not initialized, Seam was not started correctly");
      }
      props = new Hashtable<String, String>();

      if (log.isDebugEnabled())
      {
         log.debug("JNDI InitialContext properties:" + props);
      }

      try
      {
         return props.size() == 0 ? new InitialContext() : new InitialContext(props);
      }
      catch (NamingException e)
      {
         log.debug("Could not obtain initial context");
         throw e;
      }

   }

   public static InitialContext getInitialContext() throws NamingException
   {
      if (initialContext == null)
         initInitialContext();

      return initialContext;
   }

   private static synchronized void initInitialContext() throws NamingException
   {
      if (initialContext == null)
      {
         initialContext = getInitialContext(initialContextProperties);
      }
   }

   private Naming()
   {
   }

   public static void setInitialContextProperties(Hashtable initialContextProperties)
   {
      Naming.initialContextProperties = initialContextProperties;
      initialContext = null;
   }

   public static Hashtable getInitialContextProperties()
   {
      return initialContextProperties;
   }
}
