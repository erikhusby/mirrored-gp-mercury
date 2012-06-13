package org.broadinstitute.sequel.infrastructure.deployment;


import javax.naming.NamingException;

/**
 * Mockable interface for looking stuff up from JNDI
 */
public interface JNDIResolver {

    String lookupProperty(String name) throws NamingException;

}
