package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import javax.inject.Inject;

/**
 * Guts behind connecting to the LIMQueries thrift service.
 *
 * @author breilly
 */
public class ThriftConnection {

    private ThriftConfig thriftConfig;

    // stateful: created by open()
    private transient TTransport transport;

    @Inject
    public ThriftConnection(ThriftConfig thriftConfig) {
        this.thriftConfig = thriftConfig;
    }

    /**
     * Opens a connection to the thrift service based on the injected config.
     * The connection must be opened before calls can be made through the
     * client interface.
     *
     * @see org.broadinstitute.sequel.infrastructure.thrift.ThriftConnection#getClient()
     */
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

    /**
     * Closes the connection to the thrift service.
     */
    public void close() {
        if (transport != null) {
            transport.close();
            transport = null;
        }
    }

    /**
     * Returns a client interface to the thrift service. The thrift connection
     * must be opened before obtaining a client interface.
     *
     * @return the client interface to make thrift calls on
     */
    public LIMQueries.Client getClient() {
        if (transport == null) {
            throw new IllegalStateException("Thrift connection not open. Call ThriftConnection.open() before using the client.");
        }
        return new LIMQueries.Client(new TBinaryProtocol(transport));
    }
}
