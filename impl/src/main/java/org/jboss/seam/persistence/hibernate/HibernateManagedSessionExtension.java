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
package org.jboss.seam.persistence.hibernate;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;

import org.jboss.weld.extensions.reflection.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The portable extension for Seam Managed Hibernate Sessions. If hibernate is
 * found on the classpath then the real work is done by
 * {@link HibernateManagedSessionExtensionImpl}
 * 
 * @author Stuart Douglas
 * 
 */
public class HibernateManagedSessionExtension implements Extension
{
   private static final Logger log = LoggerFactory.getLogger(HibernateManagedSessionExtension.class);

   private final boolean enabled;

   private HibernateExtension delegate;

   public HibernateManagedSessionExtension()
   {
      boolean en = true;
      try
      {
         // ensure hibernate is on the CP
         Reflections.classForName("org.hibernate.Session", getClass().getClassLoader());
         delegate = new HibernateManagedSessionExtensionImpl();
      }
      catch (ClassNotFoundException e)
      {
         log.debug("Hibernate not found on the classpath, Managed Hibernate Sessions are disabled");
         en = false;
      }
      enabled = en;

   }

   public <T> void processAnnotatedType(@Observes ProcessAnnotatedType<T> event, BeanManager manager)
   {
      if (enabled)
      {
         delegate.processAnnotatedType(event, manager);
      }
   }

   public void afterBeanDiscovery(@Observes AfterBeanDiscovery event)
   {
      if (enabled)
      {
         delegate.afterBeanDiscovery(event);
      }
   }
}
