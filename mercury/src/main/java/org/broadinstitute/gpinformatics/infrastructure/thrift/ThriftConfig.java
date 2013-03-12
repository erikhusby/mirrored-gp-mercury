package org.broadinstitute.gpinformatics.infrastructure.thrift;



import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nullable;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.io.Serializable;

@ConfigKey("thrift")
public class ThriftConfig extends AbstractConfig implements Serializable {

    private String host;

    private int port;

    @Inject
    public ThriftConfig(@Nullable Deployment deployment) {
        super(deployment);
    }

    public ThriftConfig(String host, int port) {
        super(null);
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
