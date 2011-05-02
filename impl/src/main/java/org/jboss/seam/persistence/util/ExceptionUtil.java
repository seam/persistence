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

import java.lang.reflect.Method;

import org.jboss.seam.transaction.SeamApplicationException;

/**
 * Utility class for dealing with application exceptions
 *
 * @author Stuart Douglas
 */
public class ExceptionUtil {

    private ExceptionUtil() {

    }

    public static boolean exceptionCausesRollback(Exception e) {
        boolean defaultRollback = false;
        if (e instanceof RuntimeException) {
            defaultRollback = true;
        }
        Class<?> exClass = e.getClass();
        if (exClass.isAnnotationPresent(SeamApplicationException.class)) {
            SeamApplicationException sae = exClass.getAnnotation(SeamApplicationException.class);
            return sae.rollback();
        } else if (exClass.isAnnotationPresent(EjbApi.APPLICATION_EXCEPTION)) {
            Object ae = exClass.getAnnotation(EjbApi.APPLICATION_EXCEPTION);
            try {
                Method rollback = EjbApi.APPLICATION_EXCEPTION.getMethod("rollback");
                return (Boolean) rollback.invoke(ae);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        return defaultRollback;
    }
}
