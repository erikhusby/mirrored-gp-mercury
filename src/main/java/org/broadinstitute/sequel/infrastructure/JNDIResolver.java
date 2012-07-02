package org.broadinstitute.sequel.infrastructure;


import javax.naming.NamingException;
import java.io.Serializable;

/**
 * Mockable interface for looking up values from JNDI
 */
public interface JNDIResolver extends Serializable {

    String lookupProperty(String name) throws NamingException;

}
