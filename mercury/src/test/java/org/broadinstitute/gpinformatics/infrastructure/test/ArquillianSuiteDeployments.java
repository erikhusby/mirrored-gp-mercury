package org.broadinstitute.gpinformatics.infrastructure.test;

import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;

/**
 * Allows many tests to be run in a single deployment, reducing test time.
 */
@ArquillianSuiteDeployment
public class ArquillianSuiteDeployments {

    @Deployment
    public static WebArchive deploy() {
        return DeploymentBuilder.buildMercuryWar();
    }
}
