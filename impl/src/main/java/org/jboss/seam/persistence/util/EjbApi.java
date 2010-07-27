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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.enterprise.inject.spi.AnnotatedType;

import org.jboss.weld.extensions.util.Reflections;

/**
 * Utility class that provides access to some annotations from the Java
 * Enterprise Edition specs if they are present on the classpath
 * 
 * 
 */
public class EjbApi
{

   public @interface Dummy
   {
   }

   public static final Class<? extends Annotation> TRANSACTION_ATTRIBUTE;
   public static final Class<? extends Enum> TRANSACTION_ATTRIBUTE_TYPE;
   public static final Class<? extends Annotation> APPLICATION_EXCEPTION;

   public static final Class<? extends Annotation> STATEFUL;
   public static final Class<? extends Annotation> STATELESS;
   public static final Class<? extends Annotation> MESSAGE_DRIVEN;
   public static final Class<? extends Annotation> SINGLETON;

   public static final Object MANDATORY;

   public static final Object REQUIRED;

   public static final Object REQUIRES_NEW;

   public static final Object SUPPORTS;

   public static final Object NOT_SUPPORTED;

   public static final Object NEVER;

   public static final boolean INVOCATION_CONTEXT_AVAILABLE;

   private static Class classForName(String name)
   {
      try
      {
         return Reflections.classForName(name);
      }
      catch (ClassNotFoundException cnfe)
      {
         return Dummy.class;
      }
   }

   static
   {
      APPLICATION_EXCEPTION = classForName("javax.ejb.ApplicationException");
      TRANSACTION_ATTRIBUTE = classForName("javax.ejb.TransactionAttribute");

      TRANSACTION_ATTRIBUTE_TYPE = classForName("javax.ejb.TransactionAttributeType");
      if (TRANSACTION_ATTRIBUTE_TYPE.getName().equals("javax.ejb.TransactionAttributeType"))
      {
         MANDATORY = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "MANDATORY");
         REQUIRED = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "REQUIRED");
         NOT_SUPPORTED = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "NOT_SUPPORTED");
         REQUIRES_NEW = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "REQUIRES_NEW");
         NEVER = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "NEVER");
         SUPPORTS = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "SUPPORTS");
      }
      else
      {
         MANDATORY = Dummy.class;
         REQUIRED = Dummy.class;
         NOT_SUPPORTED = Dummy.class;
         REQUIRES_NEW = Dummy.class;
         NEVER = Dummy.class;
         SUPPORTS = Dummy.class;
      }
      INVOCATION_CONTEXT_AVAILABLE = !classForName("javax.interceptor.InvocationContext").equals(Dummy.class);

      STATEFUL = classForName("javax.ejb.Stateful");
      STATELESS = classForName("javax.ejb.Stateless");
      MESSAGE_DRIVEN = classForName("javax.ejb.MessageDriven");
      SINGLETON = classForName("javax.ejb.Singleton");

   }

   public static String name(Annotation annotation)
   {
      return (String) invokeAndWrap(Reflections.getMethod(annotation.annotationType(), "name"), annotation);
   }

   public static Class[] value(Annotation annotation)
   {
      return (Class[]) invokeAndWrap(Reflections.getMethod(annotation.annotationType(), "value"), annotation);
   }

   public static boolean rollback(Annotation annotation)
   {
      return (Boolean) invokeAndWrap(Reflections.getMethod(annotation.annotationType(), "rollback"), annotation);
   }

   private static Object invokeAndWrap(Method method, Object instance, Object... parameters)
   {
      try
      {
         return method.invoke(instance, parameters);
      }
      catch (Exception e)
      {
         throw new RuntimeException(e);
      }
   }

   public static <X> boolean isEjb(AnnotatedType<X> type)
   {
      return type.isAnnotationPresent(STATEFUL) || type.isAnnotationPresent(STATELESS) || type.isAnnotationPresent(MESSAGE_DRIVEN) || type.isAnnotationPresent(SINGLETON);
   }
}
