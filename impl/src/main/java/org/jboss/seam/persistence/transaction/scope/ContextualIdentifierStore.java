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
package org.jboss.seam.persistence.transaction.scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.PassivationCapable;

/**
 * Class that maps a Contextual to an identifier. If the Contextual is
 * {@link PassivationCapable} then the id used, otherwise one is generated
 * 
 * @author Stuart Douglas
 * 
 */
public class ContextualIdentifierStore
{
   private final Map<Contextual<?>, String> identifiers = new ConcurrentHashMap<Contextual<?>, String>();

   private final Map<String, Contextual<?>> contextualForIdentifier = new ConcurrentHashMap<String, Contextual<?>>();

   private int count = 0;

   private final String PREFIX = "CID-";

   public Contextual<?> getContextual(String id)
   {
      return contextualForIdentifier.get(id);
   }

   public String getId(Contextual<?> contextual)
   {
      if (identifiers.containsKey(contextual))
      {
         return identifiers.get(contextual);
      }
      if (contextual instanceof PassivationCapable)
      {
         PassivationCapable p = (PassivationCapable) contextual;
         String id = p.getId();
         contextualForIdentifier.put(id, contextual);
         return id;
      }
      else
      {
         synchronized (this)
         {
            // check again inside the syncronized block
            if (identifiers.containsKey(contextual))
            {
               return identifiers.get(contextual);
            }
            String id = PREFIX + getClass().getName() + "-" + (count++);
            identifiers.put(contextual, id);
            contextualForIdentifier.put(id, contextual);
            return id;
         }
      }
   }
}
