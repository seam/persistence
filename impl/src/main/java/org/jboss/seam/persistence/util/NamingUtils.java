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
package org.jboss.seam.persistence.util;

import java.util.Hashtable;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.jboss.solder.logging.Logger;

/**
 * this has been ported to seam and hacked to make it work
 * <p/>
 * we need to figure out what we are doing with JNDI in seam 3 and make this go
 * away
 *
 * @author stuart
 */
public final class NamingUtils {
    private static final Logger log = Logger.getLogger(NamingUtils.class);
    private static Hashtable initialContextProperties;

    private static InitialContext initialContext;

    public static InitialContext getInitialContext(Hashtable<String, String> props) throws NamingException {
        if (props == null) {
            // throw new
            // IllegalStateException("JNDI properties not initialized, Seam was not started correctly");
        }
        props = new Hashtable<String, String>();

        if (log.isDebugEnabled()) {
            log.debug("JNDI InitialContext properties:" + props);
        }

        try {
            return props.size() == 0 ? new InitialContext() : new InitialContext(props);
        } catch (NamingException e) {
            log.debug("Could not obtain initial context");
            throw e;
        }

    }

    public static InitialContext getInitialContext() throws NamingException {
        if (initialContext == null)
            initInitialContext();

        return initialContext;
    }

    private static synchronized void initInitialContext() throws NamingException {
        if (initialContext == null) {
            initialContext = getInitialContext(initialContextProperties);
        }
    }

    private NamingUtils() {
    }

    public static void setInitialContextProperties(Hashtable initialContextProperties) {
        NamingUtils.initialContextProperties = initialContextProperties;
        initialContext = null;
    }

    public static Hashtable getInitialContextProperties() {
        return initialContextProperties;
    }
}
