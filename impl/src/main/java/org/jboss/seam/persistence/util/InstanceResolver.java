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
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.weld.extensions.literal.DefaultLiteral;

/**
 * Utillity class that can get an Instance<T> from the bean manager
 * 
 * @author stuart
 * 
 */
public class InstanceResolver
{
   private InstanceResolver()
   {

   }

   public static <T> Instance<T> getInstance(Class<T> type, BeanManager manager)
   {
      return getInstance(type, manager, DefaultLiteral.INSTANCE);
   }

   public static <T> Instance<T> getInstance(Class<T> type, BeanManager manager, Annotation... qualifiers)
   {
      Type instanceType = new InstanceParamatizedTypeImpl<T>(type);
      Bean<?> bean = manager.resolve(manager.getBeans(instanceType, qualifiers));
      CreationalContext ctx = manager.createCreationalContext(bean);
      return (Instance<T>) manager.getInjectableReference(new InstanceInjectionPoint<T>(type, qualifiers), ctx);
   }

   private static class InstanceParamatizedTypeImpl<T> implements ParameterizedType
   {
      private final Class<T> type;

      public InstanceParamatizedTypeImpl(Class<T> type)
      {
         this.type = type;
      }

      public Type[] getActualTypeArguments()
      {
         Type[] ret = new Type[1];
         ret[0] = type;
         return ret;
      }

      public Type getOwnerType()
      {
         return null;
      }

      public Type getRawType()
      {
         return Instance.class;
      }

   }

   /**
    * TODO: this is not portable, needs to be a proper implementation as this
    * could cause a NPE due to some methods returning null
    */
   private static class InstanceInjectionPoint<T> implements InjectionPoint
   {

      private final Class<T> type;
      private final Set<Annotation> qualifiers;

      public InstanceInjectionPoint(Class<T> type, Annotation... quals)
      {
         this.type = type;
         qualifiers = new HashSet<Annotation>();
         for (Annotation a : quals)
         {
            qualifiers.add(a);
         }
      }

      public Annotated getAnnotated()
      {
         return null;
      }

      public Bean<?> getBean()
      {
         return null;
      }

      public Member getMember()
      {
         return null;
      }

      public Set<Annotation> getQualifiers()
      {

         return qualifiers;
      }

      public Type getType()
      {
         return new InstanceParamatizedTypeImpl<T>(type);
      }

      public boolean isDelegate()
      {
         return false;
      }

      public boolean isTransient()
      {
         return false;
      }

   }
}
