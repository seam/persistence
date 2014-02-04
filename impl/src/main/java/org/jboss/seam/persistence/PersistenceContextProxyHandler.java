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
package org.jboss.seam.persistence;

import org.jboss.solder.logging.Logger;
import org.jboss.seam.persistence.util.InstanceResolver;
import org.jboss.solder.el.Expressions;

import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Proxy handler for a {@link EntityManager} proxy that allows the use of EL in queries.
 *
 * @author Stuart Douglas
 */
public class PersistenceContextProxyHandler implements Serializable {
    private static final long serialVersionUID = -6539267789786229774L;

    private final EntityManager delegate;

    private transient Expressions expressions;

    private final BeanManager beanManager;

    private static final Logger log = Logger.getLogger(PersistenceContextProxyHandler.class);

    public PersistenceContextProxyHandler(EntityManager delegate, BeanManager beanManager) {
        this.delegate = delegate;
        this.beanManager = beanManager;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("createQuery".equals(method.getName()) && method.getParameterTypes().length > 0
                && method.getParameterTypes()[0].equals(String.class)) {
            return handleCreateQueryWithString(method, args);
        }

        return invokeMethod(method, args);
    }

    protected Object handleCreateQueryWithString(Method method, Object[] args) throws Throwable {
        if (args[0] == null) {
            return method.invoke(delegate, args);
        }
        String ejbql = (String) args[0];
        if (ejbql.indexOf('#') > 0) {
            Expressions expressions = getExpressions();
            QueryParser qp = new QueryParser(expressions, ejbql);
            Object[] newArgs = args.clone();
            newArgs[0] = qp.getEjbql();
            Query query = (Query) method.invoke(delegate, newArgs);
            for (int i = 0; i < qp.getParameterValues().size(); i++) {
                query.setParameter(QueryParser.getParameterName(i), qp.getParameterValues().get(i));
            }
            return query;
        } else {
            return invokeMethod(method, args);
        }
    }

    private Expressions getExpressions() {
        if(expressions == null) {
            expressions = InstanceResolver.getInstance(Expressions.class, beanManager).get();
        }
        return expressions;
    }

    /**
     * Invokes the method on the delegate and unwraps any original Exceptions  
     */
    private Object invokeMethod(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(delegate, args);
        }
        catch(InvocationTargetException e) {
            if (e.getCause() != null) {
                throw e.getCause();
            }
            throw e;
        }
    }
}
