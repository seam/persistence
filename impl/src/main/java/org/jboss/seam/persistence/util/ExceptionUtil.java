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
package org.jboss.seam.persistence.util;

import java.lang.reflect.Method;

import org.jboss.seam.persistence.transaction.SeamApplicationException;

/**
 * Utility class for dealing with application exceptions
 * 
 * @author Stuart Douglas
 * 
 */
public class ExceptionUtil
{

   private ExceptionUtil()
   {

   }

   public static boolean exceptionCausesRollback(Exception e)
   {
      boolean defaultRollback = false;
      if (e instanceof RuntimeException)
      {
         defaultRollback = true;
      }
      Class<?> exClass = e.getClass();
      if (exClass.isAnnotationPresent(SeamApplicationException.class))
      {
         SeamApplicationException sae = exClass.getAnnotation(SeamApplicationException.class);
         return sae.rollback();
      }
      else if (exClass.isAnnotationPresent(EjbApi.APPLICATION_EXCEPTION))
      {
         Object ae = exClass.getAnnotation(EjbApi.APPLICATION_EXCEPTION);
         try
         {
            Method rollback = EjbApi.APPLICATION_EXCEPTION.getMethod("rollback");
            return (Boolean) rollback.invoke(ae);
         }
         catch (Exception ex)
         {
            throw new RuntimeException(ex);
         }
      }
      return defaultRollback;
   }
}
