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

/**
 * PersistenceContexts tracks active persistence contexts within a conversation.
 * 
 * This allows for features such as changing the flush mode of all entity
 * managers to @{link {@link FlushModeType#MANUAL} during the render response
 * phase when using seam managed transactions in JSF
 * 
 */
public interface PersistenceContexts
{

   public abstract FlushModeType getFlushMode();

   /**
    * Changes the flush mode of all persistence contexts in the conversation
    * 
    * @param flushMode the new flush mode
    */
   public abstract void changeFlushMode(FlushModeType flushMode);

   /**
    * Restore the previous flush mode if the current flush mode is marked as
    * temporary.
    */
   public abstract void restoreFlushMode();

   /**
    * Perform
    */
   public abstract void beforeRender();

   public abstract void afterRender();

   public abstract void touch(ManagedPersistenceContext context);

   public abstract void untouch(ManagedPersistenceContext context);

}