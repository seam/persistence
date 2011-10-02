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
package org.jboss.seam.persistence.hibernate;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.solder.logging.Logger;
import org.jboss.solder.reflection.Reflections;

/**
 * The portable extension for Seam Managed Hibernate Sessions. If hibernate is
 * found on the classpath then the real work is done by
 * {@link HibernateManagedSessionExtensionImpl}
 *
 * @author Stuart Douglas
 */
public class HibernateManagedSessionExtension implements Extension {
    private static final Logger log = Logger.getLogger(HibernateManagedSessionExtension.class);

    private final boolean enabled;

    private HibernateExtension delegate;

    public HibernateManagedSessionExtension() {
        boolean en = true;
        try {
            // ensure hibernate is on the CP
            Reflections.classForName("org.hibernate.Session", getClass().getClassLoader());
            delegate = new HibernateManagedSessionExtensionImpl();
        } catch (ClassNotFoundException e) {
            log.debug("Hibernate not found on the classpath, Managed Hibernate Sessions are disabled");
            en = false;
        }
        enabled = en;

    }

    public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event, BeanManager manager) {
        if (enabled) {
            delegate.processAnnotatedType(event, manager);
        }
    }

    public void afterBeanDiscovery(@Observes AfterBeanDiscovery event) {
        if (enabled) {
            delegate.afterBeanDiscovery(event);
        }
    }
}
