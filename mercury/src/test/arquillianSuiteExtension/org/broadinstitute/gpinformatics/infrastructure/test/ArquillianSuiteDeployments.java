package org.broadinstitute.gpinformatics.infrastructure.test;

import org.eu.ingwar.tools.arquillian.extension.suite.annotations.ArquillianSuiteDeployment;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Allows many tests to be run in a single deployment, reducing test time.
 */
@ArquillianSuiteDeployment
public class ArquillianSuiteDeployments {

    public static final String ARQ_SUITE_GROUP = "ArqSuiteGroup";

    @Deployment
    public static WebArchive deploy() {
        String property = System.getProperty(ARQ_SUITE_GROUP);
        if (property == null) {
            throw new RuntimeException("No " + ARQ_SUITE_GROUP + " system property.");
        }
        switch (property) {
        case TestGroups.STUBBY:
            return StubbyContainerTest.buildMercuryWar();
        case TestGroups.STANDARD:
            return DeploymentBuilder.buildMercuryWar(DEV);
        default:
            throw new RuntimeException("Unexpected test group " + property);
        }
    }

}
