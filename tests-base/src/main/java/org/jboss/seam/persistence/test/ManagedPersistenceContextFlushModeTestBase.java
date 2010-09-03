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
package org.jboss.seam.persistence.test;

import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.jboss.seam.persistence.FlushModeManager;
import org.jboss.seam.persistence.FlushModeType;
import org.jboss.seam.persistence.ManagedPersistenceContext;
import org.jboss.seam.persistence.PersistenceContexts;
import org.jboss.seam.transactions.test.util.HelloService;
import org.jboss.seam.transactions.test.util.Hotel;
import org.jboss.seam.transactions.test.util.ManagedPersistenceContextProvider;
import org.junit.Test;

public class ManagedPersistenceContextFlushModeTestBase
{
   public static Class<?>[] getTestClasses()
   {
      return new Class[] { ManagedPersistenceContextFlushModeTestBase.class, Hotel.class, ManagedPersistenceContextProvider.class, HelloService.class };
   }

   @Inject
   FlushModeManager manager;

   @Inject
   EntityManager em;

   @Inject
   ManagedPersistenceContext context;

   @Inject
   PersistenceContexts pc;

   @Inject
   BeanManager bm;

   @Test
   public void testPersistenceContextDefaultFlushMode() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      manager.setFlushModeType(FlushModeType.MANUAL);
      Assert.assertEquals(FlushMode.MANUAL, ((Session) em.getDelegate()).getFlushMode());
   }

   @Test
   public void testChangedTouchedPersistenceContextFlushMode()
   {
      try
      {
         em.setFlushMode(javax.persistence.FlushModeType.AUTO);
         pc.changeFlushMode(FlushModeType.MANUAL);
         Assert.assertEquals(FlushMode.MANUAL, ((Session) em.getDelegate()).getFlushMode());
      }
      finally
      {
         em.setFlushMode(javax.persistence.FlushModeType.AUTO);
      }
   }
}
