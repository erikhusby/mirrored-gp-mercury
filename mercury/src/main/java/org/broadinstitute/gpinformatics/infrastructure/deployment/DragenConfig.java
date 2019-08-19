package org.broadinstitute.gpinformatics.infrastructure.deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

@SuppressWarnings("UnusedDeclaration")
@ConfigKey("dragen")
@ApplicationScoped
public class DragenConfig extends AbstractConfig implements Serializable {

    private String demultiplexOutputPath;

    private String dragenPath;

    private String slurmHost;

    public DragenConfig() {
    }

    @Inject
    public DragenConfig(@Nonnull Deployment deploymentConfig) {
        super(deploymentConfig);
    }

    public String getDemultiplexOutputPath() {
        return demultiplexOutputPath;
    }

    public void setDemultiplexOutputPath(String demultiplexOutputPath) {
        this.demultiplexOutputPath = demultiplexOutputPath;
    }

    public String getDragenPath() {
        return dragenPath;
    }

    public void setDragenPath(String dragenPath) {
        this.dragenPath = dragenPath;
    }

    public String getSlurmHost() {
        return slurmHost;
    }

    public void setSlurmHost(String slurmHost) {
        this.slurmHost = slurmHost;
    }
}
