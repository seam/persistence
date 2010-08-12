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

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.persistence.EntityManagerFactory;

/**
 * SMPC lifecycle for SMPC's configured via @SeamManaged
 * 
 * @author Stuart Douglas
 * 
 */
public class ManagedPersistenceContextBeanLifecycle extends AbstractManagedPersistenceContextBeanLifecycle
{

   protected final Annotation[] qualifiers;
   protected final BeanManager manager;

   private EntityManagerFactory emf;

   public ManagedPersistenceContextBeanLifecycle(Set<Annotation> qualifiers, ClassLoader loader, BeanManager manager)
   {
      super(manager, loader);
      this.qualifiers = new Annotation[qualifiers.size()];
      int i = 0;
      for (Annotation a : qualifiers)
      {
         this.qualifiers[i++] = a;
      }
      this.manager = manager;
   }

   /**
    * lazily resolve the relevant EMF
    */
   protected EntityManagerFactory getEntityManagerFactory()
   {
      if (emf == null)
      {
         Bean<EntityManagerFactory> bean = (Bean) manager.resolve(manager.getBeans(EntityManagerFactory.class, qualifiers));
         if (bean == null)
         {
            throw new RuntimeException("Could not find EntityManagerFactory bean with qualifiers" + qualifiers);
         }
         CreationalContext<EntityManagerFactory> ctx = manager.createCreationalContext(bean);
         emf = (EntityManagerFactory) manager.getReference(bean, EntityManagerFactory.class, ctx);
      }
      return emf;
   }
}
