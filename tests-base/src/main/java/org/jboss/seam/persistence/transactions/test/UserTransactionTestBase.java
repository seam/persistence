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
package org.jboss.seam.persistence.transactions.test;

import java.util.List;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import junit.framework.Assert;

import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.jboss.seam.transactions.test.util.HelloService;
import org.jboss.seam.transactions.test.util.Hotel;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class UserTransactionTestBase
{

   public static Class<?>[] getTestClasses()
   {
      return new Class[] { UserTransactionTestBase.class, Hotel.class, HelloService.class };
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @PersistenceContext
   EntityManager em;

   @Test
   public void userTransactionTest() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      transaction.begin();
      em.joinTransaction();
      Hotel h = new Hotel("test", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.commit();
      em.clear();

      transaction.begin();
      em.joinTransaction();
      h = new Hotel("test2", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      em.persist(h);
      em.flush();
      transaction.rollback();
      em.clear();

      transaction.begin();
      em.joinTransaction();
      List<Hotel> hotels = em.createQuery("select h from Hotel h").getResultList();
      Assert.assertTrue(hotels.size() == 1);
      transaction.rollback();
      em.clear();

   }

}
