package org.broadinstitute.gpinformatics.athena.boundary;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Get the build and version information.  Also get the deployment type (DEV,QA,PROD,STUBBY).
 *
 * @author Michael Dinsmore
 */
@Named
@RequestScoped
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
        // TODO this code lies to the layout page about RC deployments since we don't currently have an RC watermarked
        // Mercury helmet
        if (deployment == Deployment.RC) {
            return Deployment.QA.name();
        }
        return deployment.name();
    }
}
