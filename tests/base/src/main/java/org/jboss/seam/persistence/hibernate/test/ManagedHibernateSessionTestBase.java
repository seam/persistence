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
package org.jboss.seam.persistence.hibernate.test;

import java.util.List;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.hibernate.Session;
import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.test.util.ManagedHibernateSessionProvider;
import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.junit.Test;

public class ManagedHibernateSessionTestBase
{

   public static Class<?>[] getTestClasses()
   {
      return new Class[] { ManagedHibernateSessionTestBase.class, Hotel.class, ManagedHibernateSessionProvider.class, HelloService.class };
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @Inject
   Session session;

   @Test
   public void testManagedHibernateSession() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      transaction.begin();
      Hotel h = new Hotel("test", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      session.persist(h);
      session.flush();
      transaction.commit();

      transaction.begin();
      h = new Hotel("test2", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      session.persist(h);
      session.flush();
      transaction.rollback();

      transaction.begin();
      List<Hotel> hotels = session.createQuery("select h from Hotel h").list();
      Assert.assertEquals(1, hotels.size());
      transaction.rollback();
   }

}
