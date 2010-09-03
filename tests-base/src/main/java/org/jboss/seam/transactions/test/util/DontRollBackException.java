package org.jboss.seam.transactions.test.util;

import javax.ejb.ApplicationException;

@ApplicationException(rollback = false)
public class DontRollBackException extends Exception
{

}
