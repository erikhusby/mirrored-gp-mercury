package org.broadinstitute.gpinformatics.mercury.infrastructure;


import javax.naming.NamingException;
import java.io.Serializable;

/**
 * Mockable interface for looking up values from JNDI
 */
public interface JNDIResolver extends Serializable {

    String lookupProperty(String name) throws NamingException;

}
