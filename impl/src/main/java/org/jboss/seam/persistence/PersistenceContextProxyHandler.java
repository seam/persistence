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
import java.lang.reflect.Method;

import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.jboss.seam.persistence.util.InstanceResolver;
import org.jboss.seam.solder.el.Expressions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxy handler for a {@link EntityManager} proxy that allows the use of EL in
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

   static final Logger log = LoggerFactory.getLogger(ManagedPersistenceContextProxyHandler.class);

   public PersistenceContextProxyHandler(EntityManager delegate, BeanManager beanManager)
   {
      this.delegate = delegate;
      expressionsInstance = InstanceResolver.getInstance(Expressions.class, beanManager);
   }

   public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
   {
      if ("createQuery".equals(method.getName()) && method.getParameterTypes().length > 0 && method.getParameterTypes()[0].equals(String.class))
      {
         return handleCreateQueryWithString(method, args);
      }

      return method.invoke(delegate, args);
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
