package org.broadinstitute.sequel.infrastructure.deployment;


import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * Real implementation of JNDI resolver that tries to look up properties in JNDI
 *
 */
public class JNDIResolverImpl implements JNDIResolver {

    @Override
    public String lookupProperty(String name) throws NamingException {

        return InitialContext.<String>doLookup(name);

    }

}
