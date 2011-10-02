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
import java.lang.reflect.Member;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionPoint;

import org.jboss.solder.literal.DefaultLiteral;


/**
 * Utillity class that can get an Instance<T> from the bean manager
 *
 * @author stuart
 */
public class InstanceResolver {
    private InstanceResolver() {

    }

    public static <T> Instance<T> getInstance(Class<T> type, BeanManager manager) {
        return getInstance(type, manager, DefaultLiteral.INSTANCE);
    }

    public static <T> Instance<T> getInstance(Class<T> type, BeanManager manager, Annotation... qualifiers) {
        Type instanceType = new InstanceParamatizedTypeImpl<T>(type);
        Bean<?> bean = manager.resolve(manager.getBeans(instanceType, qualifiers));
        CreationalContext ctx = manager.createCreationalContext(bean);
        return (Instance<T>) manager.getInjectableReference(new InstanceInjectionPoint<T>(type, qualifiers), ctx);
    }

    private static class InstanceParamatizedTypeImpl<T> implements ParameterizedType {
        private final Class<T> type;

        public InstanceParamatizedTypeImpl(Class<T> type) {
            this.type = type;
        }

        public Type[] getActualTypeArguments() {
            Type[] ret = new Type[1];
            ret[0] = type;
            return ret;
        }

        public Type getOwnerType() {
            return null;
        }

        public Type getRawType() {
            return Instance.class;
        }

    }

    /**
     * TODO: this is not portable, needs to be a proper implementation as this
     * could cause a NPE due to some methods returning null
     */
    private static class InstanceInjectionPoint<T> implements InjectionPoint {

        private final Class<T> type;
        private final Set<Annotation> qualifiers;

        public InstanceInjectionPoint(Class<T> type, Annotation... quals) {
            this.type = type;
            qualifiers = new HashSet<Annotation>();
            for (Annotation a : quals) {
                qualifiers.add(a);
            }
        }

        public Annotated getAnnotated() {
            return null;
        }

        public Bean<?> getBean() {
            return null;
        }

        public Member getMember() {
            return null;
        }

        public Set<Annotation> getQualifiers() {

            return qualifiers;
        }

        public Type getType() {
            return new InstanceParamatizedTypeImpl<T>(type);
        }

        public boolean isDelegate() {
            return false;
        }

        public boolean isTransient() {
            return false;
        }

    }
}
