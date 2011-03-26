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
package org.jboss.seam.transaction.test;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.seam.persistence.test.util.DontRollBackException;
import org.jboss.seam.persistence.test.util.Hotel;
import org.jboss.seam.transaction.TransactionPropagation;
import org.jboss.seam.transaction.Transactional;

@Transactional(TransactionPropagation.REQUIRED)
public class TransactionManagedBean
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
