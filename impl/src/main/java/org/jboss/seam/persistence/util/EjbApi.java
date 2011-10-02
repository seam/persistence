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
package org.jboss.seam.persistence.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.enterprise.inject.spi.AnnotatedType;

import org.jboss.solder.reflection.Reflections;

/**
 * Utility class that provides access to some annotations from the Java
 * Enterprise Edition specs if they are present on the classpath
 */
public class EjbApi {

    public @interface Dummy {
    }

    public static final Class<? extends Annotation> TRANSACTION_ATTRIBUTE;
    public static final Class<? extends Enum> TRANSACTION_ATTRIBUTE_TYPE;
    public static final Class<? extends Annotation> APPLICATION_EXCEPTION;

    public static final Class<? extends Annotation> STATEFUL;
    public static final Class<? extends Annotation> STATELESS;
    public static final Class<? extends Annotation> MESSAGE_DRIVEN;
    public static final Class<? extends Annotation> SINGLETON;

    public static final Object MANDATORY;

    public static final Object REQUIRED;

    public static final Object REQUIRES_NEW;

    public static final Object SUPPORTS;

    public static final Object NOT_SUPPORTED;

    public static final Object NEVER;

    public static final boolean INVOCATION_CONTEXT_AVAILABLE;

    private static Class classForName(String name) {
        try {
            return Reflections.classForName(name);
        } catch (ClassNotFoundException cnfe) {
            return Dummy.class;
        }
    }

    static {
        APPLICATION_EXCEPTION = classForName("javax.ejb.ApplicationException");
        TRANSACTION_ATTRIBUTE = classForName("javax.ejb.TransactionAttribute");

        TRANSACTION_ATTRIBUTE_TYPE = classForName("javax.ejb.TransactionAttributeType");
        if (TRANSACTION_ATTRIBUTE_TYPE.getName().equals("javax.ejb.TransactionAttributeType")) {
            MANDATORY = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "MANDATORY");
            REQUIRED = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "REQUIRED");
            NOT_SUPPORTED = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "NOT_SUPPORTED");
            REQUIRES_NEW = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "REQUIRES_NEW");
            NEVER = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "NEVER");
            SUPPORTS = Enum.valueOf(TRANSACTION_ATTRIBUTE_TYPE, "SUPPORTS");
        } else {
            MANDATORY = Dummy.class;
            REQUIRED = Dummy.class;
            NOT_SUPPORTED = Dummy.class;
            REQUIRES_NEW = Dummy.class;
            NEVER = Dummy.class;
            SUPPORTS = Dummy.class;
        }
        INVOCATION_CONTEXT_AVAILABLE = !classForName("javax.interceptor.InvocationContext").equals(Dummy.class);

        STATEFUL = classForName("javax.ejb.Stateful");
        STATELESS = classForName("javax.ejb.Stateless");
        MESSAGE_DRIVEN = classForName("javax.ejb.MessageDriven");
        SINGLETON = classForName("javax.ejb.Singleton");

    }

    public static String name(Annotation annotation) {
        return (String) invokeAndWrap(Reflections.findDeclaredMethod(annotation.annotationType(), "name"), annotation);
    }

    public static Class[] value(Annotation annotation) {
        return (Class[]) invokeAndWrap(Reflections.findDeclaredMethod(annotation.annotationType(), "value"), annotation);
    }

    public static boolean rollback(Annotation annotation) {
        return (Boolean) invokeAndWrap(Reflections.findDeclaredMethod(annotation.annotationType(), "rollback"), annotation);
    }

    private static Object invokeAndWrap(Method method, Object instance, Object... parameters) {
        try {
            return method.invoke(instance, parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <X> boolean isEjb(AnnotatedType<X> type) {
        return type.isAnnotationPresent(STATEFUL) || type.isAnnotationPresent(STATELESS) || type.isAnnotationPresent(MESSAGE_DRIVEN) || type.isAnnotationPresent(SINGLETON);
    }
}
