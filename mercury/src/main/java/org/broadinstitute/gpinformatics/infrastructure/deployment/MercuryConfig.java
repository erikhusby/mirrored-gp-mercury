package org.broadinstitute.gpinformatics.infrastructure.deployment;

import java.io.Serializable;

/**
 * @author breilly
 */
// called "app" because "mercury" has a special meaning in the YAML file
@ConfigKey("app")
public class MercuryConfig extends AbstractConfig implements Serializable {

    private String host;

    // Use empty string since port can be missing.
    private String port = "";

    private int jmsPort;

    public String getUrl() {
        return "http://" + host + port + "/Mercury/";
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
