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
package org.jboss.seam.persistence.hibernate;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.hibernate.Session;
import org.jboss.seam.persistence.HibernatePersistenceProvider;
import org.jboss.seam.persistence.ManagedPersistenceContext;
import org.jboss.seam.persistence.SeamManaged;
import org.jboss.weld.extensions.bean.BeanBuilder;
import org.jboss.weld.extensions.literal.AnyLiteral;
import org.jboss.weld.extensions.literal.ApplicationScopedLiteral;
import org.jboss.weld.extensions.literal.DefaultLiteral;
import org.jboss.weld.extensions.reflection.annotated.AnnotatedTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension the wraps producer methods/fields that produce an entity manager to
 * turn them into Seam Managed Persistence Contexts.
 * 
 * 
 * @author Stuart Douglas
 * 
 */
public class HibernateManagedPersistenceContextExtension implements Extension
{

   Set<Bean<?>> beans = new HashSet<Bean<?>>();

   private static final Logger log = LoggerFactory.getLogger(HibernateManagedPersistenceContextExtension.class);

   private final HibernatePersistenceProvider provider = new HibernatePersistenceProvider();

   /**
    * loops through the fields on an AnnotatedType looking for a SessionFactory
    * producer that is annotated {@link SeamManaged}. Then a corresponding smpc
    * bean is created and registered. The producers scope will be changed to
    * ApplicationScoped (or @Dependent if it is created using resource
    * injection) to ensure that the SMPC is only created once
    * 
    */
   public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event, BeanManager manager)
   {
      AnnotatedTypeBuilder<T> modifiedType = null;
      for (AnnotatedField<? super T> f : event.getAnnotatedType().getFields())
      {
         // look for a seam managed persistence unit declaration on EE resource
         // producer fields
         if (f.isAnnotationPresent(SeamManaged.class) && f.isAnnotationPresent(PersistenceUnit.class) && f.isAnnotationPresent(Produces.class) && EntityManagerFactory.class.isAssignableFrom(f.getJavaMember().getType()))
         {
            if (modifiedType == null)
            {
               modifiedType = new AnnotatedTypeBuilder().readFromType(event.getAnnotatedType());
            }
            Set<Annotation> qualifiers = new HashSet<Annotation>();
            Class<? extends Annotation> scope = Dependent.class;
            // get the qualifier and scope for the new bean
            for (Annotation a : f.getAnnotations())
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
            qualifiers.add(AnyLiteral.INSTANCE);
            // we need to remove the scope, they are not nessesarily supported
            // on producer fields
            if (scope != Dependent.class)
            {
               modifiedType.removeFromField(f.getJavaMember(), scope);
            }
            registerManagedPersistenceContext(qualifiers, scope, manager, event.getAnnotatedType().getJavaClass().getClassLoader());
         }
         // now look for producer methods that produce an EntityManagerFactory.
         // This allows the user to manually configure an EntityManagerFactory
         // and return it from a producer method
      }
      // now look for SMPC's that are configured programatically via a producer
      // method. This looks for both EMF's and SessionFactories
      // The producer method has its scope changes to application scoped
      // this allows for programatic config of the SMPC
      for (AnnotatedMethod<? super T> m : event.getAnnotatedType().getMethods())
      {
         if (m.isAnnotationPresent(SeamManaged.class) && m.isAnnotationPresent(Produces.class) && EntityManagerFactory.class.isAssignableFrom(m.getJavaMember().getReturnType()))
         {
            if (modifiedType == null)
            {
               modifiedType = new AnnotatedTypeBuilder().readFromType(event.getAnnotatedType());
            }
            Set<Annotation> qualifiers = new HashSet<Annotation>();
            Class<? extends Annotation> scope = Dependent.class;
            // get the qualifier and scope for the new bean
            for (Annotation a : m.getAnnotations())
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
            qualifiers.add(AnyLiteral.INSTANCE);
            // we need to change the scope to application scoped
            modifiedType.removeFromMethod(m.getJavaMember(), scope);
            modifiedType.addToMethod(m.getJavaMember(), ApplicationScopedLiteral.INSTANCE);
            registerManagedPersistenceContext(qualifiers, scope, manager, event.getAnnotatedType().getJavaClass().getClassLoader());
         }
      }

      if (modifiedType != null)
      {
         event.setAnnotatedType(modifiedType.create());
      }
   }

   private void registerManagedPersistenceContext(Set<Annotation> qualifiers, Class<? extends Annotation> scope, BeanManager manager, ClassLoader loader)
   {
      // we need to add all additional interfaces from our
      // SeamPersistenceProvider to the bean as at this stage we have no way of
      // knowing which persistence provider is actually in use this only time
      // that this may cause slightly odd behavior is if two providers are on
      // the class path, in which case the entity manager may be assignable to
      // additional interfaces that it does not support.
      Set<Class<?>> additionalInterfaces = new HashSet<Class<?>>();

      additionalInterfaces.addAll(provider.getAdditionalEntityManagerInterfaces());

      // create the new bean to be registered later
      HibernateManagedPersistenceContextBeanLifecycle lifecycle = new HibernateManagedPersistenceContextBeanLifecycle(qualifiers, loader, manager);
      AnnotatedTypeBuilder<Session> typeBuilder = new AnnotatedTypeBuilder().setJavaClass(Session.class);
      BeanBuilder<Session> builder = new BeanBuilder<Session>(manager).readFromType(typeBuilder.create());
      builder.qualifiers(qualifiers);
      builder.scope(scope);
      builder.getTypes().add(ManagedPersistenceContext.class);
      builder.getTypes().addAll(additionalInterfaces);
      builder.getTypes().add(Object.class);
      builder.beanLifecycle(lifecycle);
      beans.add(builder.create());
   }

   public void afterBeanDiscovery(@Observes AfterBeanDiscovery event)
   {
      for (Bean<?> i : beans)
      {
         event.addBean(i);
      }
   }

}
