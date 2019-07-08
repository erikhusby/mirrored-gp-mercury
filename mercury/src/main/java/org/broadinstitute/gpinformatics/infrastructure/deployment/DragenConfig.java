package org.broadinstitute.gpinformatics.infrastructure.deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

@SuppressWarnings("UnusedDeclaration")
@ConfigKey("dragen")
@ApplicationScoped
public class DragenConfig extends AbstractConfig implements Serializable {
    private String demultiplexOutputDirectory;

    public DragenConfig() {
    }

    @Inject
    public DragenConfig(@Nonnull Deployment deploymentConfig) {
        super(deploymentConfig);
    }

    public String getDemultiplexOutputDirectory() {
        return demultiplexOutputDirectory;
    }

    public void setDemultiplexOutputDirectory(String demultiplexOutputDirectory) {
        this.demultiplexOutputDirectory = demultiplexOutputDirectory;
    }
}
