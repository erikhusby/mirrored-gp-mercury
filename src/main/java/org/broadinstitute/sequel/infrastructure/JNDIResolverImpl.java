package org.broadinstitute.sequel.infrastructure;


import org.apache.commons.logging.Log;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Real implementation of JNDI resolver that tries to look up properties in JNDI
 *
 */
public class JNDIResolverImpl implements JNDIResolver {

    @Inject
    private Log log;


    @Override
    public String lookupProperty(String name) throws NamingException {

        final InitialContext initialContext = new InitialContext();

        final Hashtable<?,?> environment = initialContext.getEnvironment();

        // The SEQUEL_DEPLOYMENT setting specified in WEB-INF/classes/jndi.properties in Arquillian micro-deployments
        // shows up not in the context but in the environment of the context
        if (environment.containsKey(name)) {

            log.info("Loading property '" + name + "' from context environment, value = '" + environment.get(name) + "'");

            return (String) environment.get(name);
        }


        return initialContext.<String>doLookup(name);

    }

}
