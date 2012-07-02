package org.broadinstitute.sequel.infrastructure.thrift;


import java.io.Serializable;

public class ThriftConfig implements Serializable {

    private String host;

    private int port;

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
}
