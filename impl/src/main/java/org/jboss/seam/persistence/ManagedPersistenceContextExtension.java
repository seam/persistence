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

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessProducer;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.jboss.weld.extensions.annotated.AnnotatedTypeBuilder;
import org.jboss.weld.extensions.bean.BeanBuilder;
import org.jboss.weld.extensions.literal.DefaultLiteral;

/**
 * Extension the wraps producer methods/fields that produce an entity manager to
 * turn them into Seam Managed Persistence Contexts.
 * 
 * At present this happens automatically, in future we will need some way to
 * configure it
 * 
 * @author stuart
 * 
 */
public class ManagedPersistenceContextExtension implements Extension
{

   Set<Bean<?>> beans = new HashSet<Bean<?>>();

   public <T> void processProducer(@Observes ProcessProducer<T, EntityManagerFactory> event, BeanManager manager)
   {
      if (!event.getAnnotatedMember().isAnnotationPresent(SeamManaged.class))
      {
         return;
      }
      Set<Annotation> qualifiers = new HashSet<Annotation>();
      Class scope = Dependent.class;
      for (Annotation a : event.getAnnotatedMember().getAnnotations())
      {
         if (manager.isQualifier(a.annotationType()))
         {
            qualifiers.add(a);
         }
         else if (manager.isScope(a.annotationType()))
         {
            scope = a.annotationType();
         }
      }
      if (qualifiers.isEmpty())
      {
         qualifiers.add(new DefaultLiteral());
      }
      AnnotatedTypeBuilder<EntityManager> typeBuilder = AnnotatedTypeBuilder.newInstance(EntityManager.class);
      BeanBuilder<EntityManager> builder = new BeanBuilder<EntityManager>(typeBuilder.create(), manager);
      builder.defineBeanFromAnnotatedType();
      builder.setQualifiers(qualifiers);
      builder.setScope(scope);
      builder.setInjectionTarget(new NoOpInjectionTarget());
      builder.setBeanLifecycle(new ManagedPersistenceContextBeanLifecycle(qualifiers, manager));
      beans.add(builder.create());
   }

   public void afterBeanDiscovery(@Observes AfterBeanDiscovery event)
   {
      for (Bean<?> i : beans)
      {
         event.addBean(i);
      }
   }

   private static class NoOpInjectionTarget implements InjectionTarget<EntityManager>
   {

      public EntityManager produce(CreationalContext<EntityManager> ctx)
      {
         return null;
      }

      public Set<InjectionPoint> getInjectionPoints()
      {
         return Collections.emptySet();
      }

      public void dispose(EntityManager instance)
      {

      }

      public void preDestroy(EntityManager instance)
      {

      }

      public void postConstruct(EntityManager instance)
      {

      }

      public void inject(EntityManager instance, CreationalContext<EntityManager> ctx)
      {

      }

   }
}
