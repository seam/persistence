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

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.jboss.seam.persistence.test.util.HelloService;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.test.util.ManagedPersistenceContextProvider;
import org.jboss.seam.persistence.transaction.DefaultTransaction;
import org.jboss.seam.persistence.transaction.SeamTransaction;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests that injection is working for JPA entities
 * 
 * @author Stuart Douglas
 * 
 */

public class EntityInjectionTestBase
{

   public static Class<?>[] getTestClasses()
   {
      return new Class[] { HibernateSearchTestBase.class, Hotel.class, ManagedPersistenceContextProvider.class, HelloService.class, EntityInjectionTestBase.class };
   }

   @Inject
   @DefaultTransaction
   SeamTransaction transaction;

   @Inject
   EntityManagerFactory emf;

   @Test
   public void testInjectionIntoEntity() throws NotSupportedException, SystemException, SecurityException, IllegalStateException, RollbackException, HeuristicMixedException, HeuristicRollbackException
   {
      EntityManager em = null;
      try
      {
         em = emf.createEntityManager();
         transaction.begin();
         em.joinTransaction();
         Hotel h = new Hotel("Hilton", "Fake St", "Wollongong", "NSW", "2518", "Australia");
         em.persist(h);
         em.flush();
         transaction.commit();
         em.close();
         transaction.begin();
         em = emf.createEntityManager();
         em.joinTransaction();

         h = (Hotel) em.createQuery("select h from Hotel h where h.name='Hilton'").getSingleResult();
         Assert.assertTrue(h.isInitalizerCalled());
         Assert.assertEquals(h.sayHello(), "Hello");
      }
      finally
      {
         em.close();
         transaction.rollback();
      }

   }

}
