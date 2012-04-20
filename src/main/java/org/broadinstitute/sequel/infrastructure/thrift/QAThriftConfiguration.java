package org.broadinstitute.sequel.infrastructure.thrift;


import javax.enterprise.inject.Default;

@Default
public class QAThriftConfiguration implements ThriftConfiguration {

    @Override
    public String getHost() {
        return "seqtest04";
    }

    @Override
    public int getPort() {
        return 9090;
    }
}
