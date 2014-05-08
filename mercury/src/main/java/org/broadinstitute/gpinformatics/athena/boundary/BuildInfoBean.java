package org.broadinstitute.gpinformatics.athena.boundary;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

/**
 * Get the build and version information.  Also get the deployment type (DEV,QA,PROD,STUBBY).
 */
@ApplicationScoped
public class BuildInfoBean {
    @Inject
    private Deployment deployment;

    /**
     * Gets the maven injected build info.
     *
     * @return String of the build info to insert into page footer
     */
    public String getBuildInformation() {
        return MercuryConfiguration.getInstance().getBuildInformation();
    }

    /*
     * Get the deployed environment type from the JBoss Server.
     *
     * @return string of the deployment
     */
    public String getDeployment() {
        return deployment.name();
    }
}
