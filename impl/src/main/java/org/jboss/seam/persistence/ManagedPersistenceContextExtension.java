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
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.PersistenceUnit;

import org.jboss.seam.persistence.util.EnvironmentUtils;
import org.jboss.weld.extensions.bean.BeanBuilder;
import org.jboss.weld.extensions.bean.Beans;
import org.jboss.weld.extensions.literal.AnyLiteral;
import org.jboss.weld.extensions.literal.ApplicationScopedLiteral;
import org.jboss.weld.extensions.literal.DefaultLiteral;
import org.jboss.weld.extensions.reflection.annotated.AnnotatedTypeBuilder;
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
    * For non-ee environments this extension also bootstraps @PersistenceUnit
    * producer fields
    * 
    */
   public <T> void processAnnotatedType(@Observes final ProcessAnnotatedType<T> event, BeanManager manager)
   {
      AnnotatedTypeBuilder<T> modifiedType = null;
      boolean bootstrapped = false;
      for (AnnotatedField<? super T> f : event.getAnnotatedType().getFields())
      {
         if (f.isAnnotationPresent(PersistenceUnit.class) && f.isAnnotationPresent(Produces.class) && !EnvironmentUtils.isEEEnvironment())
         {
            bootstrapped = true;
            final String unitName = f.getAnnotation(PersistenceUnit.class).unitName();
            final Set<Annotation> qualifiers = Beans.getQualifiers(manager, f.getAnnotations());
            if (qualifiers.isEmpty())
            {
               qualifiers.add(DefaultLiteral.INSTANCE);
            }
            qualifiers.add(AnyLiteral.INSTANCE);
            beans.add(createEMFBean(unitName, qualifiers, event.getAnnotatedType()));
         }
         // look for a seam managed persistence unit declaration on EE resource
         // producer fields
         if (f.isAnnotationPresent(SeamManaged.class) && (f.isAnnotationPresent(PersistenceUnit.class) || f.isAnnotationPresent(Resource.class)) && f.isAnnotationPresent(Produces.class) && EntityManagerFactory.class.isAssignableFrom(f.getJavaMember().getType()))
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
            if (bootstrapped)
            {
               modifiedType.removeFromField(f.getJavaMember(), Produces.class);
            }
            registerManagedPersistenceContext(qualifiers, scope, f.isAnnotationPresent(Alternative.class), manager, event.getAnnotatedType().getJavaClass().getClassLoader(), f);
            log.info("Configuring Seam Managed Persistence Context from producer field " + f.getDeclaringType().getJavaClass().getName() + "." + f.getJavaMember().getName() + " with qualifiers " + qualifiers);
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
            log.info("Configuring Seam Managed Persistence Context from producer method " + m.getDeclaringType().getJavaClass().getName() + "." + m.getJavaMember().getName() + " with qualifiers " + qualifiers);
         }
      }

      if (modifiedType != null)
      {
         event.setAnnotatedType(modifiedType.create());
      }
   }

   private Bean<?> createEMFBean(final String unitName, final Set<Annotation> qualifiers, final AnnotatedType<?> type)
   {
      return new Bean<EntityManagerFactory>()
      {
         public Set<Type> getTypes()
         {
            Set<Type> types = new HashSet<Type>();
            types.add(Object.class);
            types.add(EntityManagerFactory.class);
            return types;
         }

         public Class<? extends Annotation> getScope()
         {
            return ApplicationScoped.class;
         }

         public EntityManagerFactory create(CreationalContext<EntityManagerFactory> ctx)
         {
            return Persistence.createEntityManagerFactory(unitName);
         }

         public void destroy(EntityManagerFactory emf, CreationalContext<EntityManagerFactory> ctx)
         {
            emf.close();
            ctx.release();
         }

         public Class<?> getBeanClass()
         {
            return type.getJavaClass();
         }

         public Set<InjectionPoint> getInjectionPoints()
         {
            return Collections.emptySet();
         }

         public String getName()
         {
            return null;
         }

         public Set<Annotation> getQualifiers()
         {
            return qualifiers;
         }

         public Set<Class<? extends Annotation>> getStereotypes()
         {
            return Collections.emptySet();

         }

         public boolean isAlternative()
         {
            return false;
         }

         public boolean isNullable()
         {
            return false;
         }
      };
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
