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

import javax.naming.NamingException;
import javax.persistence.EntityManagerFactory;

import org.jboss.seam.persistence.util.NamingUtils;
import org.jboss.weld.extensions.core.Veto;

/**
 * Class that enabled the seam managed persitence context to be configured via
 * xml.
 * 
 * There are two ways to do this. Either the persistenceUnintJndi name can be
 * set, in which case the entityManagerFactory is looked up from JNDI.
 * 
 * Alternativly the @Inject annotation can be applied to the
 * entityManagerFactory propery along with any qualifiers needed and the
 * entityManagerFactory will be injected
 * 
 * Any qualifiers that are applied to this class are also applied to the managed
 * persistence context in question
 * 
 * @author Stuart Douglas
 * 
 */
@Veto
public class ManagedPersistenceContext
{

   public String getPersistenceUnitJndiName()
   {
      return persistenceUnitJndiName;
   }

   public void setPersistenceUnitJndiName(String persistenceUnitJndiName)
   {
      this.persistenceUnitJndiName = persistenceUnitJndiName;
   }

   public EntityManagerFactory getEntityManagerFactory()
   {
      if (entityManagerFactory != null)
      {
         return entityManagerFactory;
      }
      try
      {
         return (EntityManagerFactory) NamingUtils.getInitialContext().lookup(persistenceUnitJndiName);
      }
      catch (NamingException ne)
      {
         throw new IllegalArgumentException("EntityManagerFactory not found in JNDI : " + persistenceUnitJndiName, ne);
      }
   }

   public void setEntityManagerFactory(EntityManagerFactory entityManagerFactory)
   {
      this.entityManagerFactory = entityManagerFactory;
   }

   String persistenceUnitJndiName;

   EntityManagerFactory entityManagerFactory;

}
