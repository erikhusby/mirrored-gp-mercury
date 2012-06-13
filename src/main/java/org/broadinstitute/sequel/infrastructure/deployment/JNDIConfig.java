package org.broadinstitute.sequel.infrastructure.deployment;

import org.apache.commons.logging.Log;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.naming.NamingException;


// @Startup
// @Singleton
/**
 * I originally named this class just 'Config' but there was a clash on startup with Brian's
 * conversation-related Config class.
 */
public class JNDIConfig {


    public enum Deployment {
        DEV,
        TEST,
        QA,
        PROD
    }


    @Inject
    private Log log;


    @Inject
    private JNDIResolver jndiResolver;


    private Deployment deployment;


    @PostConstruct
    public void init() {

        String deploymentString = null;


        try {

            deploymentString = jndiResolver.lookupProperty("SEQUEL_DEPLOYMENT");

            // Not checking for NullPointerException or IllegalArgumentException from Enum#valueOf().  If we see either
            // of these that's a Big Deal since there appears to have been a botched attempt to specify the
            // SEQUEL_DEPLOYMENT in JNDI.  Let the exception propagate out of this @PostConstruct method.
            deployment = Deployment.valueOf(deploymentString);

            log.info("SequeL deployment specified in JNDI, set to " + deploymentString);

        }

        catch (NamingException e) {

            // This is not a botched JNDI specification, but rather a failure to find the property in JNDI.  Per
            // 2012-06-13 Exome Express meeting we are also treating this as a Big Deal.
            log.error("JNDI lookup of SEQUEL_DEPLOYMENT property failed!");
            throw new RuntimeException(e);
        }

    }



    public Deployment getDeployment() {
        return deployment;
    }
}
