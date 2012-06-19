package org.broadinstitute.sequel.infrastructure;


import javax.naming.NamingException;

/**
 * Mockable interface for looking up values from JNDI
 */
public interface JNDIResolver {

    String lookupProperty(String name) throws NamingException;

}
