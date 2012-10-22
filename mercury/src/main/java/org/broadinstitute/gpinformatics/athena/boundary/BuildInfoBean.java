package org.broadinstitute.gpinformatics.athena.boundary;

import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.MercuryConfiguration;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Get the build and version information.
 *
 * @author Michael Dinsmore
 */
@Named
@RequestScoped
public class BuildInfoBean {
    /**
     * Gets the maven injected build info.
     *
     * @return String of the build info to insert into page footer
     */
    public String getBuildInformation() {
        return MercuryConfiguration.getInstance().getBuildInformation();
    }
}
