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

   public static final Class<Annotation> STATELESS;
   public static final Class<Annotation> STATEFUL;
   public static final Class<Annotation> MESSAGE_DRIVEN;
   public static final Class<Annotation> PRE_PASSIVATE;
   public static final Class<Annotation> POST_ACTIVATE;
   public static final Class<Annotation> PRE_DESTROY;
   public static final Class<Annotation> POST_CONSTRUCT;
   public static final Class<Annotation> REMOTE;
   public static final Class<Annotation> REMOVE;
   public static final Class<Annotation> LOCAL;
   public static final Class<Annotation> APPLICATION_EXCEPTION;
   public static final Class<Annotation> PERSISTENCE_CONTEXT;
   public static final Class<Annotation> PERSISTENCE_UNIT;
   public static final Class<Annotation> INTERCEPTORS;
   public static final Class<Annotation> AROUND_INVOKE;
   public static final Class<Annotation> EJB_EXCEPTION;
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
      STATELESS = classForName("javax.ejb.Stateless");
      STATEFUL = classForName("javax.ejb.Stateful");
      MESSAGE_DRIVEN = classForName("javax.ejb.MessageDriven");
      APPLICATION_EXCEPTION = classForName("javax.ejb.ApplicationException");
      PERSISTENCE_CONTEXT = classForName("javax.persistence.PersistenceContext");
      PERSISTENCE_UNIT = classForName("javax.persistence.PersistenceUnit");
      REMOVE = classForName("javax.ejb.Remove");
      REMOTE = classForName("javax.ejb.Remote");
      LOCAL = classForName("javax.ejb.Local");
      PRE_PASSIVATE = classForName("javax.ejb.PrePassivate");
      POST_ACTIVATE = classForName("javax.ejb.PostActivate");
      PRE_DESTROY = classForName("javax.annotation.PreDestroy");
      POST_CONSTRUCT = classForName("javax.annotation.PostConstruct");
      INTERCEPTORS = classForName("javax.interceptor.Interceptors");
      AROUND_INVOKE = classForName("javax.interceptor.AroundInvoke");
      EJB_EXCEPTION = classForName("javax.ejb.EJBException");
      INVOCATION_CONTEXT_AVAILABLE = !classForName("javax.interceptor.InvocationContext").equals(Dummy.class);
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

}
