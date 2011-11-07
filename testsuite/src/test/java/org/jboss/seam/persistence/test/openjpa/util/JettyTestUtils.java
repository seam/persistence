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
package org.jboss.seam.persistence.test.openjpa.util;

import javax.el.ExpressionFactory;

import com.sun.el.ExpressionFactoryImpl;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * @author Stuart Douglas
 */
public class JettyTestUtils {
    public static WebArchive createTestArchive() {
        WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
        war.addAsWebInfResource("META-INF/seam-beans.xml", "classes/META-INF/seam-beans.xml");
        war.addAsWebInfResource("WEB-INF/jetty-env.xml", "jetty-env.xml");
        war.addAsWebInfResource("WEB-INF/web.xml", "web.xml");
        war.addAsServiceProvider(ExpressionFactory.class, ExpressionFactoryImpl.class);
        return war;
    }
}
