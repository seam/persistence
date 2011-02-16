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

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.seam.persistence.test.util.DontRollBackException;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.persistence.transaction.TransactionPropagation;
import org.jboss.seam.persistence.transaction.Transactional;

@TransactionalStereotype
public class StereotypeTransactionManagedBean
{

   @Inject
   EntityManager entityManager;

   public void addHotel()
   {
      entityManager.joinTransaction();
      Hotel h = new Hotel("test", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      entityManager.persist(h);
      entityManager.flush();
   }

   public void failToAddHotel()
   {
      entityManager.joinTransaction();
      Hotel h = new Hotel("test2", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      entityManager.persist(h);
      entityManager.flush();
      throw new RuntimeException("Roll back transaction");
   }

   public void addHotelWithApplicationException() throws DontRollBackException
   {
      entityManager.joinTransaction();
      Hotel h = new Hotel("test3", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      entityManager.persist(h);
      entityManager.flush();
      throw new DontRollBackException();
   }

   @Transactional(TransactionPropagation.NEVER)
   public void tryAndAddHotelWithNoTransaction()
   {
      entityManager.joinTransaction();
      Hotel h = new Hotel("test3", "Fake St", "Wollongong", "NSW", "2518", "Australia");
      entityManager.persist(h);
      entityManager.flush();
   }

}
