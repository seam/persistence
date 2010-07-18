package org.jboss.seam.transactions.test.util;

import javax.enterprise.inject.Produces;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

public class EntityManagerProvider
{
   @PersistenceContext
   @Produces
   EntityManager entityManager;
}
