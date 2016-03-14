package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.JNDIResolver;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.naming.NamingException;
import java.io.Serializable;
import java.text.MessageFormat;

/**
 * This class is responsible for looking up the value of <pre>MERCURY_DEPLOYMENT</pre> from JNDI or the JNDI environment.
 * The former is set in Glassfish Custom JNDI resources (resource type is String, name = 'value', value is one of
 * DEV, TEST, QA, or PROD).  In Arquillian tests extending ContainerTest, a jndi.properties file is supplied in
 * the archive that
 * will set MERCURY_DEPLOYMENT to the value of STUBBY in the JNDI environment.  This class is annotated as
 * {@link Startup} and {@link Singleton}, so it will do this JNDI lookup on artifact deployment.  Failure to resolve
 * MERCURY_DEPLOYMENT or have the value match one of Deployment instance values (DEV, PROD, etc.) is a fatal error that
 * will halt deployment.
 */
@Startup
@Singleton
public class DeploymentProducer implements Serializable {
    public static final String MERCURY_DEPLOYMENT = "MERCURY_DEPLOYMENT";

    private static final Log log = LogFactory.getLog(DeploymentProducer.class);

    @Inject
    private JNDIResolver jndiResolver;

    private Deployment deployment;

    @PostConstruct
    public void init() {
        String deploymentString = null;

        try {
            // NamingException lookup failures checked below if MERCURY_DEPLOYMENT is not found.
            deploymentString = jndiResolver.lookupProperty(MERCURY_DEPLOYMENT);

            // NullPointerException or IllegalArgumentException from Enum#valueOf() checked below, exceptions propagated
            // to abort deployment if we don't like the value of MERCURY_DEPLOYMENT.
            deployment = Deployment.valueOf(deploymentString);

            log.info(MessageFormat.format("Mercury deployment specified in JNDI, set to {0}.", deploymentString));
        } catch (NamingException e) {
            // This represents a failure to find the property in JNDI at all.  Per 2012-06-13 Exome Express meeting
            // we are treating this as a Big Deal and aborting the deployment by throwing a RuntimeException.
            log.error(MessageFormat.format("JNDI lookup of {0} property failed!", MERCURY_DEPLOYMENT), e);
            throw new RuntimeException(e);
        } catch (NullPointerException | IllegalArgumentException e) {
            log.error(MessageFormat.format("JNDI lookup of {0} failed, found {0}={1}.", MERCURY_DEPLOYMENT, deploymentString), e);
            throw e;
        }
    }

    @Produces
    public Deployment produce() {
        return deployment;
    }

}
