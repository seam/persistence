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
package org.jboss.seam.persistence.transaction;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

import org.jboss.seam.persistence.util.EjbApi;

/**
 * Implements transaction propagation rules for Seam JavaBean components.
 *
 * @author Gavin King
 * @author Shane Bryzak
 * @author Stuart Douglas
 */
@TransactionalInterceptorBinding
@Interceptor
public class TransactionInterceptor implements Serializable
{
   private static final long serialVersionUID = -4364203056333738988L;

   transient private Map<AnnotatedElement, TransactionMetadata> transactionMetadata = new HashMap<AnnotatedElement, TransactionMetadata>();

   @Inject
   @DefaultTransaction
   private Instance<SeamTransaction> transaction;

   @Inject
   private TransactionExtension transactionExtension;

   private class TransactionMetadata
   {
      private final boolean annotationPresent;
      private final TransactionPropagation propType;

      public TransactionMetadata(Annotation annotation)
      {
         if (annotation == null)
         {
            annotationPresent = false;
            propType = null;
         }
         else if (annotation.annotationType() == Transactional.class)
         {
            annotationPresent = true;
            propType = ((Transactional) annotation).value();
         }
         else if (annotation.annotationType() == EjbApi.TRANSACTION_ATTRIBUTE)
         {
            annotationPresent = true;
            try
            {
               Object value = annotation.getClass().getMethod("value").invoke(annotation);

               if (value == EjbApi.REQUIRED)
               {
                  propType = TransactionPropagation.REQUIRED;
               }
               else if (value == EjbApi.MANDATORY)
               {
                  propType = TransactionPropagation.MANDATORY;
               }
               else if (value == EjbApi.NEVER)
               {
                  propType = TransactionPropagation.NEVER;
               }
               else if (value == EjbApi.SUPPORTS)
               {
                  propType = TransactionPropagation.SUPPORTS;
               }
               else if (value == EjbApi.NOT_SUPPORTED)
               {
                  throw new RuntimeException("TransactionAttributeType.NOT_SUPPORTED is not allowed on managed beans that are not EJB's.");
               }
               else if (value == EjbApi.REQUIRES_NEW)
               {
                  throw new RuntimeException("TransactionAttributeType.REQUIRES_NEW is not allowed on managed beans that are not EJB's.");
               }
               else
               {
                  throw new RuntimeException("Unkown TransactionAttributeType: " + value);
               }
            }
            catch (Exception e)
            {
               throw new RuntimeException(e);
            }
         }
         else
         {
            annotationPresent = false;
            propType = null;
         }
      }

      public boolean isAnnotationPresent()
      {
         return annotationPresent;
      }

      public boolean isNewTransactionRequired(boolean transactionActive)
      {
         return propType != null && propType.isNewTransactionRequired(transactionActive);
      }
   }

   private TransactionMetadata lookupTransactionMetadata(Method element)
   {
      if (transactionMetadata == null)
      {
         transactionMetadata = new HashMap<AnnotatedElement, TransactionMetadata>();
      }

      TransactionMetadata metadata = transactionMetadata.get(element);

      if (metadata == null)
      {
         synchronized (this)
         {
            if (element.isAnnotationPresent(Transactional.class))
            {
               metadata = new TransactionMetadata(element.getAnnotation(Transactional.class));
            }
            else if (element.isAnnotationPresent(EjbApi.TRANSACTION_ATTRIBUTE))
            {
               metadata = new TransactionMetadata(element.getAnnotation(EjbApi.TRANSACTION_ATTRIBUTE));
            }
            else
            {
               metadata = new TransactionMetadata(null);
            }
            transactionMetadata.put(element, metadata);
         }
      }
      return metadata;
   }

   private TransactionMetadata lookupTransactionMetadata(Class<?> element)
   {
      if (transactionMetadata == null)
      {
         transactionMetadata = new HashMap<AnnotatedElement, TransactionMetadata>();
      }

      TransactionMetadata metadata = transactionMetadata.get(element);

      if (metadata == null)
      {
         synchronized (this)
         {
            // we need access to cached stereotype information, so we load it
            // from the transaction extension
            metadata = new TransactionMetadata(transactionExtension.getClassLevelTransactionAnnotation(element));
            transactionMetadata.put((AnnotatedElement) element, metadata);
         }
      }
      return metadata;
   }

   @AroundInvoke
   public Object aroundInvoke(final InvocationContext invocation) throws Exception
   {
      return new Work()
      {

         @Override
         protected Object work() throws Exception
         {
            return invocation.proceed();
         }

         @Override
         protected boolean isNewTransactionRequired(boolean transactionActive)
         {
            return isNewTransactionRequired(invocation.getMethod(), invocation.getMethod().getDeclaringClass(), transactionActive);
         }

         private boolean isNewTransactionRequired(Method method, Class<?> beanClass, boolean transactionActive)
         {
            TransactionMetadata metadata = lookupTransactionMetadata(method);
            if (metadata.isAnnotationPresent())
            {
               return metadata.isNewTransactionRequired(transactionActive);
            }
            else
            {
               metadata = lookupTransactionMetadata(beanClass);
               return metadata.isNewTransactionRequired(transactionActive);
            }
         }

      }.workInTransaction(transaction.get());
   }
}
