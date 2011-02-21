/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.seam.persistence.transaction;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.seam.persistence.util.EjbApi;
import org.jboss.seam.solder.reflection.annotated.AnnotatedTypeBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension than provides a {@link SeamTransaction} if no other UserTransaction
 * has been registered.
 * 
 * This allows the user to register a transaction via seam XML and have it
 * automatically replace the default UserTransaction implementation
 * 
 * This is not done with alternatives, because that would require specifying the
 * transaction manager on a per module basis, and some of the UserTransaction
 * implementations need to be configured via seam xml anyway, so they would have
 * to be configured twice
 * 
 * @author Stuart Douglas
 * 
 */
public class TransactionExtension implements Extension
{

   private static final Logger log = LoggerFactory.getLogger(TransactionExtension.class);

   private final Set<Throwable> exceptions = new HashSet<Throwable>();

   private final Map<Class<?>, Annotation> classLevelAnnotations = new HashMap<Class<?>, Annotation>();

   /**
    * Looks for @Transaction or @TransactionAttribute annotations and if they
    * are found adds the transaction intercepter binding
    * 
    */
   public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> event, final BeanManager beanManager)
   {
      AnnotatedTypeBuilder<X> builder = null;
      AnnotatedType<X> type = event.getAnnotatedType();
      boolean addedToClass = false;
      if (type.isAnnotationPresent(Transactional.class))
      {
         builder = new AnnotatedTypeBuilder<X>().readFromType(type);
         builder.addToClass(TransactionInterceptorBindingLiteral.INSTANCE);
         addedToClass = true;
         classLevelAnnotations.put(type.getJavaClass(), type.getAnnotation(Transactional.class));
      }
      else if (type.isAnnotationPresent(EjbApi.TRANSACTION_ATTRIBUTE) && !EjbApi.isEjb(event.getAnnotatedType()))
      {
         checkTransactionAttributeIsValue(type, type);
         builder = new AnnotatedTypeBuilder<X>().readFromType(type);
         builder.addToClass(TransactionInterceptorBindingLiteral.INSTANCE);
         addedToClass = true;
         classLevelAnnotations.put(type.getJavaClass(), type.getAnnotation(EjbApi.TRANSACTION_ATTRIBUTE));
      }
      if (!addedToClass)
      {
         for (final Annotation annotation : type.getAnnotations())
         {
            if (beanManager.isStereotype(annotation.annotationType()))
            {
               for (Annotation stereotypeAnnotation : beanManager.getStereotypeDefinition(annotation.annotationType()))
               {
                  if (stereotypeAnnotation.annotationType().equals(Transactional.class))
                  {
                     builder = new AnnotatedTypeBuilder<X>().readFromType(type);
                     builder.addToClass(TransactionInterceptorBindingLiteral.INSTANCE);
                     addedToClass = true;
                     classLevelAnnotations.put(type.getJavaClass(), stereotypeAnnotation);
                  }
                  else if (stereotypeAnnotation.annotationType().equals(EjbApi.TRANSACTION_ATTRIBUTE) && !EjbApi.isEjb(event.getAnnotatedType()))
                  {
                     checkTransactionAttributeIsValue(type, type);
                     builder = new AnnotatedTypeBuilder<X>().readFromType(type);
                     builder.addToClass(TransactionInterceptorBindingLiteral.INSTANCE);
                     addedToClass = true;
                     classLevelAnnotations.put(type.getJavaClass(), stereotypeAnnotation);
                  }
               }
            }
         }
      }
      if (!addedToClass)
      {
         for (AnnotatedMethod<? super X> m : type.getMethods())
         {
            if (m.isAnnotationPresent(Transactional.class))
            {
               if (builder == null)
               {
                  builder = new AnnotatedTypeBuilder<X>().readFromType(type);
               }
               builder.addToMethod(m, TransactionInterceptorBindingLiteral.INSTANCE);
            }
            else if (m.isAnnotationPresent(EjbApi.TRANSACTION_ATTRIBUTE) && !EjbApi.isEjb(event.getAnnotatedType()))
            {
               checkTransactionAttributeIsValue(type, m);
               if (builder == null)
               {
                  builder = new AnnotatedTypeBuilder<X>().readFromType(type);
               }
               builder.addToMethod(m, TransactionInterceptorBindingLiteral.INSTANCE);
            }
         }
      }
      if (builder != null)
      {
         event.setAnnotatedType(builder.create());
      }
   }

   public Annotation getClassLevelTransactionAnnotation(Class<?> clazz)
   {
      return classLevelAnnotations.get(clazz);
   }

   private void afterBeanDiscover(@Observes AfterBeanDiscovery event)
   {
      for (Throwable throwable : exceptions)
      {
         event.addDefinitionError(throwable);
      }
   }

   private void checkTransactionAttributeIsValue(AnnotatedType type, Annotated element)
   {
      Object attribute = element.getAnnotation(EjbApi.TRANSACTION_ATTRIBUTE);
      if (attribute == EjbApi.REQUIRES_NEW)
      {
         exceptions.add(new RuntimeException("TransactionAttributeType.REQUIRED_NEW is not supported on Managed Beans that are not EJB's. Annotation was found on type " + type));
      }
      if (attribute == EjbApi.NOT_SUPPORTED)
      {
         exceptions.add(new RuntimeException("TransactionAttributeType.NOT_SUPPORTED is not supported on Managed Beans that are not EJB's. Annotation was found on type " + type));
      }
   }

}
