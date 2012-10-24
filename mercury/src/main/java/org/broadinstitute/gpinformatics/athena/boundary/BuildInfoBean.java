package org.broadinstitute.gpinformatics.athena.boundary;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

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
        return deployment.name();
    }
}
