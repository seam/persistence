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
package org.jboss.seam.persistence.test.util;

import java.io.File;

import org.jboss.seam.persistence.test.util.ArtifactNames;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.api.importer.ZipImporter;
import org.jboss.shrinkwrap.resolver.api.DependencyResolvers;
import org.jboss.shrinkwrap.resolver.api.maven.MavenDependencyResolver;


/**
 * @author Stuart Douglas
 */
public class JBossASTestUtils {
    /**
     * Creates a test archive with an empty beans.xml
     *
     * @return
     */
    public static WebArchive createTestArchive() {
        return createTestArchive(true);
    }

    public static WebArchive createTestArchive(boolean includeEmptyBeansXml) {
        WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");

        war.addAsLibraries(
                DependencyResolvers.use(MavenDependencyResolver.class)
                .configureFrom("../settings.xml")
                .loadReposFromPom("pom.xml")
                .artifact(ArtifactNames.SOLDER)
                .artifact(ArtifactNames.SEAM_TRANSACTION)
                .resolveAs(JavaArchive.class)
        );
        
        war.addAsLibraries(
                ShrinkWrap.create(
                    ZipImporter.class, "seam-persistence-api.jar")
                        .importFrom(new File(ArtifactNames.SEAM_PERSISTENCE_API_JAR))
                        .as(JavaArchive.class),
                ShrinkWrap.create(
                    ZipImporter.class, "seam-persistence.jar")
                        .importFrom(new File(ArtifactNames.SEAM_PERSISTENCE_IMPL_JAR))
                        .as(JavaArchive.class)
             );
        
        if (includeEmptyBeansXml) {
            war.addAsWebInfResource(new ByteArrayAsset(new byte[0]), "beans.xml");
        }
        return war;
    }

}
