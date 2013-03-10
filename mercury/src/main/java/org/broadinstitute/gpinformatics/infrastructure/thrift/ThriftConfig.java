package org.broadinstitute.gpinformatics.infrastructure.thrift;



import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import java.io.Serializable;

@ConfigKey("thrift")
public class ThriftConfig extends AbstractConfig implements Serializable {

    private String host;

    private int port;

    public ThriftConfig() {
    }

    public ThriftConfig(String host, int port) {
        this.host = host;
        this.port = port;
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

    public static ThriftConfig produce(Deployment deployment) {
        return produce(ThriftConfig.class, deployment);
    }

}
