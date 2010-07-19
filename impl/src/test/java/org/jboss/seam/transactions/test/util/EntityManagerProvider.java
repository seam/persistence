package org.jboss.seam.transactions.test.util;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Produces;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import org.jboss.seam.persistence.SeamManaged;

public class EntityManagerProvider
{
   @PersistenceUnit
   @RequestScoped
   @Produces
   @SeamManaged
   EntityManagerFactory emf;
}
