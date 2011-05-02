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
package org.jboss.seam.transaction.test.jboss;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.transaction.TransactionInterceptor;
import org.jboss.seam.transaction.test.TransactionInterceptorTestBase;
import org.jboss.seam.transaction.test.util.JBossASTestUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

/**
 * Tests the @Transactional interceptor
 * <p/>
 * TODO: refactor the tests to share a common superclass
 *
 * @author stuart
 */
@RunWith(Arquillian.class)
public class TransactionInterceptorTest extends TransactionInterceptorTestBase {
    @Deployment
    public static Archive<?> createTestArchive() {

        WebArchive war = JBossASTestUtils.createTestArchive(false);
        war.addClasses(getTestClasses());
        war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
        war.addWebResource(new ByteArrayAsset(("<beans><interceptors><class>" + TransactionInterceptor.class.getName() + "</class></interceptors></beans>").getBytes()), "beans.xml");
        return war;
    }

}
