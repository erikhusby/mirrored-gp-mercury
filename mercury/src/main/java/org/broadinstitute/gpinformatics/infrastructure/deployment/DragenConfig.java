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

    private String referenceFileServer;

    private String aggregationFilepath;

    private String intermediateResults;

    private String gsBucket;

    private String prologScriptFolder;

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

    public String getReferenceFileServer() {
        return referenceFileServer;
    }

    public void setReferenceFileServer(String referenceFileServer) {
        this.referenceFileServer = referenceFileServer;
    }

    public String getAggregationFilepath() {
        return aggregationFilepath;
    }

    public void setAggregationFilepath(String aggregationFilepath) {
        this.aggregationFilepath = aggregationFilepath;
    }

    public String getIntermediateResults() {
        return intermediateResults;
    }

    public void setIntermediateResults(String intermediateResults) {
        this.intermediateResults = intermediateResults;
    }

    public String getGsBucket() {
        return gsBucket;
    }

    public void setGsBucket(String gsBucket) {
        this.gsBucket = gsBucket;
    }

    public String getPrologScriptFolder() {
        return prologScriptFolder;
    }

    public void setPrologScriptFolder(String prologScriptFolder) {
        this.prologScriptFolder = prologScriptFolder;
    }
}
