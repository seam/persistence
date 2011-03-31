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

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.test.HibernateSearchTestBase;
import org.jboss.seam.persistence.test.util.ArtifactNames;
import org.jboss.seam.persistence.test.util.MavenArtifactResolver;
import org.jboss.seam.transaction.test.util.JBossASTestUtils;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class JBossASHibernateSearchTest extends HibernateSearchTestBase
{
   @Deployment
   public static Archive<?> createTestArchive()
   {
      WebArchive war = JBossASTestUtils.createTestArchive();
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.HIBERNATE_SEARCH));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.LUCENE_ANALYZERS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.LUCENE_CORE));
      war.addClasses(getTestClasses());
      war.addWebResource("META-INF/persistence-search.xml", "classes/META-INF/persistence.xml");
      return war;
   }

}