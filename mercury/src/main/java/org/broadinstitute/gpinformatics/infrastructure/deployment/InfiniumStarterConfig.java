package org.broadinstitute.gpinformatics.infrastructure.deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for the infinium run starter processing.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("infiniumStarter")
@ApplicationScoped
public class InfiniumStarterConfig extends AbstractConfig implements Serializable {
    private String dataPath;
    private long minimumIdatFileLength;
    private String jmsHost;
    private int jmsPort;
    private String jmsQueue;

    public InfiniumStarterConfig(){}

    @Inject
    public InfiniumStarterConfig(@Nonnull Deployment deploymentConfig) {
        super(deploymentConfig);
    }

    public String getDataPath() {
        return dataPath;
    }

    public void setDataPath(String dataPath) {
        this.dataPath = dataPath;
    }

    public long getMinimumIdatFileLength() {
        return minimumIdatFileLength;
    }

    public void setMinimumIdatFileLength(long minimumIdatFileLength) {
        this.minimumIdatFileLength = minimumIdatFileLength;
    }

    public String getJmsHost() {
        return jmsHost;
    }

    public void setJmsHost(String jmsHost) {
        this.jmsHost = jmsHost;
    }

    public int getJmsPort() {
        return jmsPort;
    }

    public void setJmsPort(int jmsPort) {
        this.jmsPort = jmsPort;
    }

    public String getJmsQueue() {
        return jmsQueue;
    }

    public void setJmsQueue(String jmsQueue) {
        this.jmsQueue = jmsQueue;
    }
}
