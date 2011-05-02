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
package org.jboss.seam.persistence.hibernate;

import org.hibernate.Session;

/**
 * event that is fired when the hibernate session is created. This allows you to
 * configure the session before it is used, e.g. by enabling filters
 *
 * @author Stuart Douglas <stuart@baileyroberts.com.au>
 */
public class SeamManagedHibernateSessionCreated {
    private final Session session;

    public SeamManagedHibernateSessionCreated(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

}
