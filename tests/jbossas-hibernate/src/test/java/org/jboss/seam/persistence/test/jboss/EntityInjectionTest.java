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
package org.jboss.seam.persistence.test.jboss;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.test.EntityInjectionTestBase;
import org.jboss.seam.persistence.test.util.JBossASTestUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Tests that injection is working for JPA entities
 *
 * @author Stuart Douglas
 */
@RunWith(Arquillian.class)
public class EntityInjectionTest extends EntityInjectionTestBase {
    @Deployment(name="EntityInjection")
    public static Archive<?> createTestArchive() {
        WebArchive war = JBossASTestUtils.createTestArchive();

        war.addClasses(getTestClasses());
        war.addAsWebInfResource("META-INF/persistence-orm.xml", "classes/META-INF/persistence.xml");
        war.addAsWebInfResource("META-INF/orm.xml", "classes/META-INF/orm.xml");
        return war;
    }

}
