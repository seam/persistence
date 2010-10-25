package org.jboss.seam.persistence.test.openjpa.util;

import java.util.Collections;
import java.util.Set;

import org.apache.openjpa.persistence.PersistenceMetaDataFactory;

public class SeamMetaDataFactory extends PersistenceMetaDataFactory
{

   public Set<String> getPersistentTypeNames(boolean arg0, ClassLoader arg1)
   {
      return Collections.singleton("org.jboss.seam.persistence.test.util.Hotel");
   }

}
