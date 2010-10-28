package org.jboss.seam.persistence.transaction.test.jboss;

import org.jboss.arquillian.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.seam.persistence.transaction.test.util.JbossasTestUtils;
import org.jboss.seam.persistence.transactions.test.UserTransactionTestBase;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.runner.RunWith;

@RunWith(Arquillian.class)
public class UserTransactionTest extends UserTransactionTestBase
{
   @Deployment
   public static Archive<?> createTestArchive()
   {
      WebArchive war = JbossasTestUtils.createTestArchive();
      war.addClasses(getTestClasses());
      war.addWebResource("META-INF/persistence.xml", "classes/META-INF/persistence.xml");
      return war;
   }

}
