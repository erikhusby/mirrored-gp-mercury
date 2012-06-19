package org.broadinstitute.sequel.infrastructure.deployment;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.infrastructure.JNDIResolver;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.naming.NamingException;


@Startup
@Singleton
/**
 * This class is responsible for looking up the value of <pre>SEQUEL_DEPLOYMENT</pre> from JNDI or the JNDI environment.
 * The former is set in Glassfish Custom JNDI resources (resource type is String, name = 'value', value is one of
 * DEV, TEST, QA, or PROD).  In Arquillian tests extending {@link ContainerTest}, a jndi.properties file is supplied in
 * the archive that
 * will set SEQUEL_DEPLOYMENT to the value of STUBBY in the JNDI environment.  This class is annotated as
 * {@link Startup} and {@link Singleton}, so it will do this JNDI lookup on artifact deployment.  Failure to resolve
 * SEQUEL_DEPLOYMENT or have the value match one of DEV, TEST, QA, PROD, or STUBBY is a fatal error that will halt
 * deployment.
 *
 * I originally named this class just 'Config' but there was a clash on startup with Brian's
 * conversation-related Config class.
 */
public class DeploymentConfig {


    @Inject
    private Log log;


    @Inject
    private JNDIResolver jndiResolver;


    private Deployment deployment;


    @PostConstruct
    public void init() {

        String deploymentString = null;


        try {

            // NamingException lookup failures checked below if SEQUEL_DEPLOYMENT is not found
            deploymentString = jndiResolver.lookupProperty("SEQUEL_DEPLOYMENT");

            // NullPointerException or IllegalArgumentException from Enum#valueOf() checked below, exceptions propagated
            // to abort deployment if we don't like the value of SEQUEL_DEPLOYMENT
            deployment = Deployment.valueOf(deploymentString);

            log.info("SequeL deployment specified in JNDI, set to " + deploymentString);

        }

        catch (NamingException e) {

            // This represents a failure to find the property in JNDI at all.  Per 2012-06-13 Exome Express meeting
            // we are treating this as a Big Deal and aborting the deployment by throwing a RuntimeException here.
            log.error("JNDI lookup of SEQUEL_DEPLOYMENT property failed! " + e);
            throw new RuntimeException(e);
        }

        catch (NullPointerException e) {
            log.error("JNDI lookup of SEQUEL_DEPLOYMENT failed, found SEQUEL_DEPLOYMENT=" + deploymentString);
            log.error(e);
            throw e;
        }

        catch (IllegalArgumentException e) {
            log.error("JNDI lookup of SEQUEL_DEPLOYMENT failed, found SEQUEL_DEPLOYMENT=" + deploymentString);
            log.error(e);
            throw e;
        }

    }


    @Produces
    public Deployment getDeployment() {
        return deployment;
    }
}
