package org.broadinstitute.sequel.infrastructure.thrift;


import javax.enterprise.inject.Alternative;

/**
 * Production thrift server
 */
@Alternative
public class ProductionThriftConfiguration implements ThriftConfiguration {

    @Override
    public String getHost() {
        return "seqlims";
    }

    @Override
    public int getPort() {
        return 9090;
    }
}
