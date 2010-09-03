package org.jboss.seam.persistence.transaction.test.jboss;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.transactions.test.UserTransactionTestBase;
import org.jboss.seam.transactions.test.util.ArtifactNames;
import org.jboss.seam.transactions.test.util.MavenArtifactResolver;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.ByteArrayAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class UserTransactionTest extends UserTransactionTestBase
{
   @Deployment
   public static Archive<?> createTestArchive()
   {
      WebArchive war = ShrinkWrap.createDomain().getArchiveFactory().create(WebArchive.class, "test.war");
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.WELD_EXTENSIONS));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.SEAM_PERSISTENCE_API));
      war.addLibraries(MavenArtifactResolver.resolve(ArtifactNames.SEAM_PERSISTENCE_IMPL));
      war.addClasses(getTestClasses());
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      war.addWebResource(new ByteArrayAsset(new byte[0]), "beans.xml");
      war.addWebResource("META-INF/services/javax.enterprise.inject.spi.Extension", "classes/META-INF/services/javax.enterprise.inject.spi.Extension");
      return war;
   }

}
