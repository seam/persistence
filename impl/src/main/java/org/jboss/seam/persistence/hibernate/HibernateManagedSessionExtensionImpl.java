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
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedField;
import javax.enterprise.inject.spi.AnnotatedMember;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.jboss.seam.persistence.HibernatePersistenceProvider;
import org.jboss.seam.persistence.ManagedPersistenceContext;
import org.jboss.seam.persistence.SeamManaged;
import org.jboss.seam.solder.bean.BeanBuilder;
import org.jboss.seam.solder.core.Veto;
import org.jboss.seam.solder.literal.AnyLiteral;
import org.jboss.seam.solder.literal.ApplicationScopedLiteral;
import org.jboss.seam.solder.literal.DefaultLiteral;
import org.jboss.seam.solder.reflection.annotated.AnnotatedTypeBuilder;
import org.jboss.seam.solder.reflection.annotated.Annotateds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class performs the actual work for the Hibernate managed session. As
 * some CDI implemtations cannot handle NDFE when loading an extension the
 * actual extension has no dependencies on the Hibernate classes
 * 
 * 
 * @author Stuart Douglas
 * 
 */
@Veto
public class HibernateManagedSessionExtensionImpl implements HibernateExtension
{
   Set<Bean<?>> beans = new HashSet<Bean<?>>();

   private static final Logger log = LoggerFactory.getLogger(HibernateManagedSessionExtensionImpl.class);

   private static final HibernatePersistenceProvider persistenceProvider = new HibernatePersistenceProvider();

   public <T> void processAnnotatedType(@Observes final ProcessAnnotatedType<T> event, BeanManager manager)
   {
      AnnotatedTypeBuilder<T> modifiedType = null;
      // look for Sessions that are configured programatically via a producer
      // method.
      for (AnnotatedMethod<? super T> method : event.getAnnotatedType().getMethods())
      {
         if (method.isAnnotationPresent(SeamManaged.class) && method.isAnnotationPresent(Produces.class) && SessionFactory.class.isAssignableFrom(method.getJavaMember().getReturnType()))
         {
            if (modifiedType == null)
            {
               modifiedType = new AnnotatedTypeBuilder().readFromType(event.getAnnotatedType());
            }
            Set<Annotation> qualifiers = new HashSet<Annotation>();
            Class<? extends Annotation> scope = Dependent.class;
            // get the qualifier and scope for the new bean
            for (Annotation annotation : method.getAnnotations())
            {
               if (manager.isQualifier(annotation.annotationType()))
               {
                  qualifiers.add(annotation);
               }
               else if (manager.isScope(annotation.annotationType()))
               {
                  scope = annotation.annotationType();
               }
            }
            if (qualifiers.isEmpty())
            {
               qualifiers.add(new DefaultLiteral());
            }
            qualifiers.add(AnyLiteral.INSTANCE);
            // we need to change the scope to application scoped
            modifiedType.removeFromMethod(method.getJavaMember(), scope);
            modifiedType.addToMethod(method.getJavaMember(), ApplicationScopedLiteral.INSTANCE);
            registerManagedSession(qualifiers, scope, method.isAnnotationPresent(Alternative.class), manager, event.getAnnotatedType().getJavaClass().getClassLoader(), method, event.getAnnotatedType().getJavaClass());
            log.info("Configuring Seam Managed Hibernate Session from producer method " + event.getAnnotatedType().getJavaClass().getName() + "." + method.getJavaMember().getName() + " with qualifiers " + qualifiers);
         }
      }

      if (modifiedType != null)
      {
         event.setAnnotatedType(modifiedType.create());
      }
   }

   private void registerManagedSession(Set<Annotation> qualifiers, Class<? extends Annotation> scope, boolean alternative, BeanManager manager, ClassLoader loader, AnnotatedMember<?> member, Class<?> declaringClass)
   {
      // create the new bean to be registered later
      HibernateManagedSessionBeanLifecycle lifecycle = new HibernateManagedSessionBeanLifecycle(qualifiers, loader, manager);
      AnnotatedTypeBuilder<Session> typeBuilder = new AnnotatedTypeBuilder().setJavaClass(Session.class);
      BeanBuilder<Session> builder = new BeanBuilder<Session>(manager).readFromType(typeBuilder.create());
      builder.qualifiers(qualifiers);
      builder.scope(scope);
      builder.beanClass(member.getDeclaringType().getJavaClass());
      builder.getTypes().add(ManagedPersistenceContext.class);
      builder.getTypes().addAll(persistenceProvider.getAdditionalSessionInterfaces());
      builder.getTypes().add(Object.class);
      builder.beanLifecycle(lifecycle);
      builder.alternative(alternative);
      StringBuilder id = new StringBuilder("SMHS-" + HibernateManagedSessionExtension.class.getName() + "-");
      if (member instanceof AnnotatedField<?>)
      {
         AnnotatedField<?> field = (AnnotatedField<?>) member;
         id.append(Annotateds.createFieldId(field));
      }
      else
      {
         AnnotatedCallable<?> method = (AnnotatedCallable<?>) member;
         id.append(Annotateds.createCallableId(method));
      }
      builder.id(id.toString());
      builder.passivationCapable(true);
      builder.toString("Seam Managed Hibernate Session with qualifiers [" + qualifiers + "] with configured by [" + member + "] on class [" + declaringClass + "]");
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
