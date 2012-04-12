package org.broadinstitute.sequel.infrastructure.thrift;


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
