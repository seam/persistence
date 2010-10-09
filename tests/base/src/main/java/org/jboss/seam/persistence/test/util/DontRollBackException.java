package org.jboss.seam.persistence.test.util;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class DontRollBackException extends Exception
{

}
