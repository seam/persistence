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

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.jboss.seam.persistence.transaction.SeamTransaction;

/**
 * Event that is fired when the SMPC is created. This allows you to configure
 * the SMPC before it is used, e.g. by enabling Hibernate filters.
 * <p/>
 * NOTE: If you are using {@link EntityTransaction} you must not attempt to
 * access the current {@link SeamTransaction} from observers for this event, as
 * an infinite loop will result.
 * <p/>
 * NOTE: The entityManger property is the unproxied EntityManager, not the seam
 * proxy.
 * 
 * 
 * @author Stuart Douglas <stuart@baileyroberts.com.au>
 * 
 */
public class SeamManagedPersistenceContextCreated
{
   private final EntityManager entityManager;

   public SeamManagedPersistenceContextCreated(EntityManager entityManager)
   {
      this.entityManager = entityManager;
   }

   public EntityManager getEntityManager()
   {
      return entityManager;
   }
}
