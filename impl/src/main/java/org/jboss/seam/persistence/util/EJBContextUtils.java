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

import javax.ejb.EJBContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;


/**
 * utility class to look up the EJBContext
 */
public class EJBContextUtils {
    public static String ejbContextName = "java:comp.ejb3/EJBContext";
    public static final String STANDARD_EJB_CONTEXT_NAME = "java:comp/EJBContext";

    public static EJBContext getEJBContext() throws NamingException {
        try {
            return (EJBContext) NamingUtils.getInitialContext().lookup(ejbContextName);
        } catch (NameNotFoundException nnfe) {
            return (EJBContext) NamingUtils.getInitialContext().lookup(STANDARD_EJB_CONTEXT_NAME);
        }
    }

    protected static String getEjbContextName() {
        return ejbContextName;
    }

    protected static void setEjbContextName(String ejbContextName) {
        EJBContextUtils.ejbContextName = ejbContextName;
    }

}
