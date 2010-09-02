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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jboss.weld.extensions.annotated.AnnotatedTypeBuilder;
import org.jboss.weld.extensions.bean.BeanBuilder;
import org.jboss.weld.extensions.literal.AnyLiteral;
import org.jboss.weld.extensions.literal.ApplicationScopedLiteral;
import org.jboss.weld.extensions.literal.DefaultLiteral;
import org.jboss.weld.extensions.util.service.ServiceLoader;
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
public class ManagedPersistenceContextExtension implements Extension
{

   Set<Bean<?>> beans = new HashSet<Bean<?>>();

   List<SeamPersistenceProvider> persistenceProviders = new ArrayList<SeamPersistenceProvider>();

   private static final Logger log = LoggerFactory.getLogger(ManagedPersistenceContextExtension.class);

   public void beforeBeanDiscovery(@Observes BeforeBeanDiscovery event)
   {
      ServiceLoader<SeamPersistenceProvider> providers = ServiceLoader.load(SeamPersistenceProvider.class);
      for (SeamPersistenceProvider i : providers)
      {
         persistenceProviders.add(i);
      }
      // this is always the last one considered
      persistenceProviders.add(new DefaultPersistenceProvider());
   }

   /**
    * loops through the fields on an AnnotatedType looking for a @PersistnceUnit
    * producer field that is annotated {@link SeamManaged}. Then a corresponding
    * smpc bean is created and registered. Any scope declaration on the producer
    * are removed as this is not supported by the spec
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
            registerManagedPersistenceContext(qualifiers, scope, f.isAnnotationPresent(Alternative.class), manager, event.getAnnotatedType().getJavaClass().getClassLoader(), f);
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
            registerManagedPersistenceContext(qualifiers, scope, m.isAnnotationPresent(Alternative.class), manager, event.getAnnotatedType().getJavaClass().getClassLoader(), m);
         }
      }

      if (modifiedType != null)
      {
         event.setAnnotatedType(modifiedType.create());
      }
   }

   private void registerManagedPersistenceContext(Set<Annotation> qualifiers, Class<? extends Annotation> scope, boolean alternative, BeanManager manager, ClassLoader loader, AnnotatedMember<?> member)
   {
      // we need to add all additional interfaces from our
      // SeamPersistenceProvider to the bean as at this stage we have no way of
      // knowing which persistence provider is actually in use. The only time
      // that this may cause slightly odd behaviour is if two providers are on
      // the class path, in which case the entity manager may be assignable to
      // additional interfaces that it does not support.
      Set<Class<?>> additionalInterfaces = new HashSet<Class<?>>();
      for (SeamPersistenceProvider i : persistenceProviders)
      {
         additionalInterfaces.addAll(i.getAdditionalEntityManagerInterfaces());
      }
      // create the new bean to be registered later
      ManagedPersistenceContextBeanLifecycle lifecycle = new ManagedPersistenceContextBeanLifecycle(qualifiers, loader, manager, additionalInterfaces, persistenceProviders);
      AnnotatedTypeBuilder<EntityManager> typeBuilder = new AnnotatedTypeBuilder().setJavaClass(EntityManager.class);
      BeanBuilder<EntityManager> builder = new BeanBuilder<EntityManager>(manager).defineBeanFromAnnotatedType(typeBuilder.create());
      builder.setQualifiers(qualifiers);
      builder.setScope(scope);
      builder.setBeanClass(member.getDeclaringType().getJavaClass());
      builder.getTypes().add(ManagedPersistenceContext.class);
      builder.getTypes().addAll(additionalInterfaces);
      builder.getTypes().add(Object.class);
      builder.setBeanLifecycle(lifecycle);
      builder.setAlternative(alternative);
      builder.setToString("Seam Managed Persistence Context with qualifiers [" + qualifiers + "] with configured by [" + member + "]");
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
