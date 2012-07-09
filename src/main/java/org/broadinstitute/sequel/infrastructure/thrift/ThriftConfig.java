package org.broadinstitute.sequel.infrastructure.thrift;



import org.broadinstitute.sequel.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.sequel.infrastructure.deployment.ConfigKey;

import java.io.Serializable;

@ConfigKey("thrift")
public class ThriftConfig extends AbstractConfig implements Serializable {

    private String host;

    private int port;

    public ThriftConfig() {
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
