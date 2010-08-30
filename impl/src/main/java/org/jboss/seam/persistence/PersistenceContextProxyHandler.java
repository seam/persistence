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

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jboss.seam.persistence.transaction.FlushModeType;
import org.jboss.seam.persistence.util.InstanceResolver;
import org.jboss.weld.extensions.el.Expressions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy handler for the entity manager proxy that allows the use of EL in
 * queries.
 * 
 * @author Stuart Douglas
 * 
 */
public class PersistenceContextProxyHandler implements Serializable
{

   private static final long serialVersionUID = -6539267789786229774L;

   private final EntityManager delegate;

   private final Instance<Expressions> expressionsInstance;

   private final Instance<DefaultPersistenceProvider> persistenceProvider;

   private final Set<Annotation> qualifiers;

   private final SeamPersistenceProvider provider;

   static final Logger log = LoggerFactory.getLogger(ManagedPersistenceContextProxyHandler.class);

   public PersistenceContextProxyHandler(EntityManager delegate, BeanManager beanManager, Set<Annotation> qualifiers, SeamPersistenceProvider provider)
   {
      this.delegate = delegate;
      this.provider = provider;
      expressionsInstance = InstanceResolver.getInstance(Expressions.class, beanManager);
      persistenceProvider = InstanceResolver.getInstance(DefaultPersistenceProvider.class, beanManager);
      this.qualifiers = new HashSet<Annotation>(qualifiers);
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if ("createQuery".equals(method.getName()) && method.getParameterTypes().length > 0 && method.getParameterTypes()[0].equals(String.class))
      {
         return handleCreateQueryWithString(method, args);
      }
      if ("changeFlushMode".equals(method.getName()) && method.getParameterTypes().length == 1 && method.getParameterTypes()[0].equals(FlushModeType.class))
      {
         changeFushMode((FlushModeType) args[0]);
         return null;
      }
      if ("getBeanType".equals(method.getName()) && method.getParameterTypes().length == 0)
      {
         return EntityManager.class;
      }
      if ("getQualifiers".equals(method.getName()) && method.getParameterTypes().length == 0)
      {
         return Collections.unmodifiableSet(qualifiers);
      }
      if ("getPersistenceProvider".equals(method.getName()) && method.getParameterTypes().length == 0)
      {
         return provider;
      }
      return method.invoke(delegate, args);
   }

   private void changeFushMode(FlushModeType flushModeType)
   {
      persistenceProvider.get().setFlushMode(delegate, flushModeType);
   }

   protected Object handleCreateQueryWithString(Method method, Object[] args) throws Throwable
   {
      if (args[0] == null)
      {
         return method.invoke(delegate, args);
      }
      String ejbql = (String) args[0];
      if (ejbql.indexOf('#') > 0)
      {
         Expressions expressions = expressionsInstance.get();
         QueryParser qp = new QueryParser(expressions, ejbql);
         Object[] newArgs = args.clone();
         newArgs[0] = qp.getEjbql();
         Query query = (Query) method.invoke(delegate, newArgs);
         for (int i = 0; i < qp.getParameterValues().size(); i++)
         {
            query.setParameter(QueryParser.getParameterName(i), qp.getParameterValues().get(i));
         }
         return query;
      }
      else
      {
         return method.invoke(delegate, args);
      }
   }
}
