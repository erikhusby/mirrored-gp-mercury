package org.broadinstitute.sequel.infrastructure.thrift;


public class ThriftConfig {

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
