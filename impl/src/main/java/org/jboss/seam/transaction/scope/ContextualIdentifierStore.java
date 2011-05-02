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
package org.jboss.seam.transaction.scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.enterprise.context.spi.Contextual;
import javax.enterprise.inject.spi.PassivationCapable;

/**
 * Class that maps a Contextual to an identifier. If the Contextual is
 * {@link PassivationCapable} then the id used, otherwise one is generated
 *
 * @author Stuart Douglas
 */
public class ContextualIdentifierStore {
    private final Map<Contextual<?>, String> identifiers = new ConcurrentHashMap<Contextual<?>, String>();

    private final Map<String, Contextual<?>> contextualForIdentifier = new ConcurrentHashMap<String, Contextual<?>>();

    private int count = 0;

    private final String PREFIX = "CID-";

    public Contextual<?> getContextual(String id) {
        return contextualForIdentifier.get(id);
    }

    public String getId(Contextual<?> contextual) {
        if (identifiers.containsKey(contextual)) {
            return identifiers.get(contextual);
        }
        if (contextual instanceof PassivationCapable) {
            PassivationCapable p = (PassivationCapable) contextual;
            String id = p.getId();
            contextualForIdentifier.put(id, contextual);
            return id;
        } else {
            synchronized (this) {
                // check again inside the syncronized block
                if (identifiers.containsKey(contextual)) {
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
