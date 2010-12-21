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
package org.jboss.seam.persistence;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;

import org.jboss.seam.solder.beanManager.BeanManagerAware;
import org.jboss.seam.solder.reflection.Reflections;
import org.jboss.seam.solder.reflection.annotated.AnnotatedTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Event listener that enables injection and initalizer methods for JPA entities
 * 
 * Other CDI featues such as interceptors, observer methods and decorators are
 * not supported
 * 
 * TODO: should we check for the presence of invalid annotations such as @Observes
 * and log a warning?
 * 
 * This listener must be enabled in orm.xml
 * 
 * @author Stuart Douglas
 * 
 */
public class InjectionEventListener extends BeanManagerAware
{

   private final static Logger log = LoggerFactory.getLogger(InjectionEventListener.class);

   private final Map<Class<?>, InjectionTarget<?>> injectionTargets = new ConcurrentHashMap<Class<?>, InjectionTarget<?>>();

   public void load(Object entity)
   {
      if (!injectionTargets.containsKey(entity.getClass()))
      {
         if (!injectionRequired(entity.getClass()))
         {
            injectionTargets.put(entity.getClass(), NULL_INJECTION_TARGET);
            log.debug("Entity {} has no injection points so injection will not be enabled", entity.getClass());
         }
         else
         {
            // it is ok for this code to run twice, so we don't really need to
            // lock
            AnnotatedTypeBuilder<?> builder = new AnnotatedTypeBuilder().readFromType(entity.getClass());
            InjectionTarget<?> injectionTarget = getBeanManager().createInjectionTarget(builder.create());
            injectionTargets.put(entity.getClass(), injectionTarget);
            log.info("Enabling injection into entity {}", entity.getClass());
         }
      }
      InjectionTarget it = injectionTargets.get(entity.getClass());
      if (it != NULL_INJECTION_TARGET)
      {
         log.debug("Running CDI injection for {}", entity.getClass());
         it.inject(entity, new CreationalContextImpl());
      }

   }

   /**
    * 
    * returns true if the class has injection points or initalizer methods
    */
   private boolean injectionRequired(Class<?> entityClass)
   {
      for (Field f : Reflections.getAllDeclaredFields(entityClass))
      {
         if (f.isAnnotationPresent(Inject.class))
         {
            return true;
         }
      }

      for (Method m : Reflections.getAllDeclaredMethods(entityClass))
      {
         if (m.isAnnotationPresent(Inject.class))
         {
            return true;
         }
      }

      for (Constructor<?> c : Reflections.getAllDeclaredConstructors(entityClass))
      {
         if (c.isAnnotationPresent(Inject.class))
         {
            return true;
         }
      }
      return false;
   }

   /**
    * marker used for the null value, as a ConcurrentHashMap does not support
    * null values
    */
   private static final InjectionTarget NULL_INJECTION_TARGET = new InjectionTarget()
   {

      public void inject(Object instance, CreationalContext ctx)
      {
      }

      public void postConstruct(Object instance)
      {
      }

      public void preDestroy(Object instance)
      {
      }

      public void dispose(Object instance)
      {
      }

      public Set getInjectionPoints()
      {
         return null;
      }

      public Object produce(CreationalContext ctx)
      {
         return null;
      }
   };

   // no-op creational context
   private static class CreationalContextImpl implements CreationalContext
   {

      public void push(Object incompleteInstance)
      {

      }

      public void release()
      {

      }

   }
}
