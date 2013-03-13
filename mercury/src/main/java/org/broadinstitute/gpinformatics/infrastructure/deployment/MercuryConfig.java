package org.broadinstitute.gpinformatics.infrastructure.deployment;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.io.Serializable;

/**
 * @author breilly
 */
// called "app" because "mercury" has a special meaning in the YAML file
@SuppressWarnings("UnusedDeclaration")
@ConfigKey("app")
public class MercuryConfig extends AbstractConfig implements Serializable {

    @Inject
    public MercuryConfig(@Nullable Deployment mercuryDeployment) {
        super(mercuryDeployment);
    }

    private String host;

    // Use empty string since port can be missing.
    private String port;

    private int jmsPort;

    public String getUrl() {
        if (!StringUtils.isBlank(port)) {
            return "http://" + host + ":" + port + "/Mercury/";
        }
        return "http://" + host + "/Mercury/";
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setJmsPort(int jmsPort) {
        this.jmsPort = jmsPort;
    }

    public int getJmsPort() {
        return jmsPort;
    }

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }
}
