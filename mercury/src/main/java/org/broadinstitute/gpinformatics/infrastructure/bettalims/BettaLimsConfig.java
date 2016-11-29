package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * Configuration for the BettaLIMS server, part of the Squid suite of applications.
 */
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("bettaLimsServer")
@ApplicationScoped
public class BettaLimsConfig extends AbstractConfig implements Serializable {
    private String wsHost;
    private int wsPort;
    private String jmsHost;
    private int jmsPort;
    private String jmsQueue;

    public BettaLimsConfig(){}

    @Inject
    public BettaLimsConfig(@Nonnull Deployment deployment) {
        super(deployment);
    }

    public String getWsHost() {
        return wsHost;
    }

    public void setWsHost(String wsHost) {
        this.wsHost = wsHost;
    }

    public int getWsPort() {
        return wsPort;
    }

    public void setWsPort(int wsPort) {
        this.wsPort = wsPort;
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
