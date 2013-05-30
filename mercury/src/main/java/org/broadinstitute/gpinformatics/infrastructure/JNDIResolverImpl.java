package org.broadinstitute.gpinformatics.infrastructure;


import org.apache.commons.logging.Log;

import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Hashtable;

/**
 * Real implementation of JNDI resolver that tries to look up properties in JNDI.
 */
public class JNDIResolverImpl implements JNDIResolver {
    @Inject
    private Log log;

    @Override
    public String lookupProperty(String name) throws NamingException {
        InitialContext initialContext = new InitialContext();

        Hashtable<?, ?> environment = initialContext.getEnvironment();

        // The MERCURY_DEPLOYMENT setting specified in WEB-INF/classes/jndi.properties in Arquillian micro-deployments
        // shows up not in the context but in the environment of the context.
        if (environment.containsKey(name)) {
            log.info("Loading property '" + name + "' from context environment, value = '" + environment.get(name) + "'");

            return (String) environment.get(name);
        }

        // Failing the Arquillian lookup in the JNDI environment, look up in System properties for JBoss.
        if (System.getProperty(name) != null) {
            return System.getProperty(name);
        }

        // Failing JBoss lookup, look up in regular JNDI for Glassfish.
        return InitialContext.doLookup(name);
    }
}
