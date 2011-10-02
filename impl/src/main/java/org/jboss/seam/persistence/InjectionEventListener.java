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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.inject.Inject;

import org.jboss.solder.logging.Logger;
import org.jboss.solder.beanManager.BeanManagerAware;
import org.jboss.solder.reflection.Reflections;
import org.jboss.solder.reflection.annotated.AnnotatedTypeBuilder;

/**
 * Event listener that enables injection and initalizer methods for JPA entities
 * <p/>
 * Other CDI featues such as interceptors, observer methods and decorators are
 * not supported
 * <p/>
 * TODO: should we check for the presence of invalid annotations such as @Observes
 * and log a warning?
 * <p/>
 * This listener must be enabled in orm.xml
 *
 * @author Stuart Douglas
 */
public class InjectionEventListener extends BeanManagerAware {

    private final static Logger log = Logger.getLogger(InjectionEventListener.class);

    private final Map<Class<?>, InjectionTarget<?>> injectionTargets = new ConcurrentHashMap<Class<?>, InjectionTarget<?>>();

    public void load(Object entity) {
        if (!injectionTargets.containsKey(entity.getClass())) {
            if (!injectionRequired(entity.getClass())) {
                injectionTargets.put(entity.getClass(), NULL_INJECTION_TARGET);
                log.debugv("Entity {} has no injection points so injection will not be enabled", entity.getClass());
            } else {
                // it is ok for this code to run twice, so we don't really need to
                // lock
                AnnotatedTypeBuilder<?> builder = new AnnotatedTypeBuilder().readFromType(entity.getClass());
                InjectionTarget<?> injectionTarget = getBeanManager().createInjectionTarget(builder.create());
                injectionTargets.put(entity.getClass(), injectionTarget);
                log.infov("Enabling injection into entity {}", entity.getClass());
            }
        }
        InjectionTarget it = injectionTargets.get(entity.getClass());
        if (it != NULL_INJECTION_TARGET) {
            log.debugv("Running CDI injection for {}", entity.getClass());
            it.inject(entity, new CreationalContextImpl());
        }

    }

    /**
     * returns true if the class has injection points or initalizer methods
     */
    private boolean injectionRequired(Class<?> entityClass) {
        for (Field f : Reflections.getAllDeclaredFields(entityClass)) {
            if (f.isAnnotationPresent(Inject.class)) {
                return true;
            }
        }

        for (Method m : Reflections.getAllDeclaredMethods(entityClass)) {
            if (m.isAnnotationPresent(Inject.class)) {
                return true;
            }
        }

        for (Constructor<?> c : Reflections.getAllDeclaredConstructors(entityClass)) {
            if (c.isAnnotationPresent(Inject.class)) {
                return true;
            }
        }
        return false;
    }

    /**
     * marker used for the null value, as a ConcurrentHashMap does not support
     * null values
     */
    private static final InjectionTarget NULL_INJECTION_TARGET = new InjectionTarget() {

        public void inject(Object instance, CreationalContext ctx) {
        }

        public void postConstruct(Object instance) {
        }

        public void preDestroy(Object instance) {
        }

        public void dispose(Object instance) {
        }

        public Set getInjectionPoints() {
            return null;
        }

        public Object produce(CreationalContext ctx) {
            return null;
        }
    };

    // no-op creational context
    private static class CreationalContextImpl implements CreationalContext {

        public void push(Object incompleteInstance) {

        }

        public void release() {

        }

    }
}
