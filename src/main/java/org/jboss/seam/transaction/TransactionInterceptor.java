package org.jboss.seam.transaction;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * Implements transaction propagation rules for Seam JavaBean components.
 * 
 * @author Gavin King
 * @author Shane Bryzak
 */
@Transactional
@Interceptor
public class TransactionInterceptor
{
   private static final long serialVersionUID = -4364203056333738988L;

   transient private Map<AnnotatedElement, TransactionMetadata> transactionMetadata = new HashMap<AnnotatedElement, TransactionMetadata>();

   @Inject
   UserTransaction transaction;

   private class TransactionMetadata
   {
      private boolean annotationPresent;
      TransactionPropagation propType;

      public TransactionMetadata(AnnotatedElement element)
      {
         annotationPresent = element.isAnnotationPresent(Transactional.class);

         if (annotationPresent)
         {
            propType = element.getAnnotation(Transactional.class).value();
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

   private TransactionMetadata lookupTransactionMetadata(AnnotatedElement element)
   {
      if (transactionMetadata == null)
      {
         transactionMetadata = new HashMap<AnnotatedElement, TransactionMetadata>();
      }

      TransactionMetadata metadata = transactionMetadata.get(element);

      if (metadata == null)
      {
         metadata = loadMetadata(element);
      }

      return metadata;
   }

   private synchronized TransactionMetadata loadMetadata(AnnotatedElement element)
   {
      if (!transactionMetadata.containsKey(element))
      {
         TransactionMetadata metadata = new TransactionMetadata(element);
         transactionMetadata.put(element, metadata);
         return metadata;
      }

      return transactionMetadata.get(element);
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
            return isNewTransactionRequired(invocation.getMethod(), invocation.getTarget().getClass(), transactionActive);
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

      }.workInTransaction(transaction);
   }

}
