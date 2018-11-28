package org.broadinstitute.gpinformatics.infrastructure.thrift;


import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.infrastructure.deployment.ConfigKey;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.Serializable;

@SuppressWarnings("UnusedDeclaration")
@ConfigKey("thrift")
@Dependent
public class ThriftConfig extends AbstractConfig implements Serializable {

    private String host;

    private int port;

    @Inject
    public ThriftConfig(@Nonnull Deployment deployment) {
        super(deployment);
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
