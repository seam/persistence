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

import java.lang.annotation.Annotation;
import java.util.Set;

/**
 * Support for additional operations for all seam managed persistence contexts.
 * 
 * 
 * @author Gavin King
 * @author Stuart Douglas
 * 
 */
public interface ManagedPersistenceContext
{
   /**
    * changes the flush mode of the persistence context. This allows changing
    * the flush mode to @{link FlushModeType#MANUAL} provided the underlying
    * {@link SeamPersistenceProvider} supports it.
    * 
    * @param flushMode the new flush mode
    */
   public void changeFlushMode(FlushModeType flushMode);

   /**
    * 
    * @return the persistence contexts qualifiers
    */
   public Set<Annotation> getQualifiers();

   /**
    * Returns the type of this persistence context. For JPA persistence contexts
    * this will be <code>javax.persistence.EntityManager</code>. For pure
    * hibernate PC's this will be <code>org.hibernate.Session</code>
    * 
    */
   public Class<?> getBeanType();

   /**
    * Returns the appropriate {@link SeamPersistenceProvider} implementation for
    * this persistence context.
    * 
    */
   public SeamPersistenceProvider getProvider();

   /**
    * Closes the persistence context after the current transaction has
    * completed.
    * 
    * If no transaction is active the PC will be closed immediately
    */
   public void closeAfterTransaction();

}
