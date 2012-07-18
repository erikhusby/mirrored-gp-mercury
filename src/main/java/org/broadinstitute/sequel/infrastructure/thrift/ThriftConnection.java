package org.broadinstitute.sequel.infrastructure.thrift;

import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import javax.inject.Inject;

/**
 * @author breilly
 */
public class ThriftConnection {

    @Inject
    private ThriftConfig thriftConfig;

    // stateful: created by open()
    private transient TTransport transport;

    public void open() {
        close();
        transport = new TSocket(thriftConfig.getHost(), thriftConfig.getPort());
        try {
            transport.open();
        }
        catch(TTransportException e) {
            throw new RuntimeException("Could not open thrift connection",e);
        }
    }

    public void close() {
        if (transport != null) {
            transport.close();
        }
    }
}
