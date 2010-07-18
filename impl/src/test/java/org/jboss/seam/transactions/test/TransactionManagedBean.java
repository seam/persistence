package org.jboss.seam.transactions.test;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import org.jboss.seam.transaction.TransactionPropagation;
import org.jboss.seam.transaction.Transactional;
import org.jboss.seam.transactions.test.util.DontRollBackException;

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

}
