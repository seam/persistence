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

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.util.AnnotationLiteral;

import org.jboss.seam.persistence.util.EjbApi;
import org.jboss.weld.extensions.annotated.AnnotatedTypeBuilder;
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

   /**
    * Looks for @Transaction or @TransactionAttribute annotations and if they
    * are found adds the transaction intercepter binding
    * 
    */
   public <X> void processAnnotatedType(@Observes ProcessAnnotatedType<X> event)
   {
      boolean addInterceptor = false;
      AnnotatedType<X> type = event.getAnnotatedType();
      if (type.isAnnotationPresent(Transactional.class))
      {

         addInterceptor = true;
      }
      else if (type.isAnnotationPresent(EjbApi.TRANSACTION_ATTRIBUTE) && !EjbApi.isEjb(event.getAnnotatedType()))
      {
         checkTransactionAttributeIsValue(type, type);
         addInterceptor = true;
      }

      for (AnnotatedMethod<? super X> m : type.getMethods())
      {
         if (m.isAnnotationPresent(Transactional.class))
         {
            addInterceptor = true;
         }
         else if (m.isAnnotationPresent(EjbApi.TRANSACTION_ATTRIBUTE) && !EjbApi.isEjb(event.getAnnotatedType()))
         {
            checkTransactionAttributeIsValue(type, m);
            addInterceptor = true;
         }
      }
      if (addInterceptor)
      {
         event.setAnnotatedType(addInterceptorBinding(type));
      }
   }

   public <X> AnnotatedType addInterceptorBinding(AnnotatedType<X> type)
   {
      AnnotatedTypeBuilder<X> builder = new AnnotatedTypeBuilder<X>().readFromType(type);
      builder.addToClass(new AnnotationLiteral<TransactionalInterceptorBinding>()
      {
      });
      return builder.create();
   }

   private void checkTransactionAttributeIsValue(AnnotatedType type, Annotated element)
   {
      Object attribute = element.getAnnotation(EjbApi.TRANSACTION_ATTRIBUTE);
      if (attribute == EjbApi.REQUIRES_NEW)
      {
         throw new RuntimeException("TransactionAttributeType.REQUIRED_NEW is not supported on Managed Beans that are not EJB's. Annotation was found on type " + type);
      }
      if (attribute == EjbApi.NOT_SUPPORTED)
      {
         throw new RuntimeException("TransactionAttributeType.NOT_SUPPORTED is not supported on Managed Beans that are not EJB's. Annotation was found on type " + type);
      }
   }
}
