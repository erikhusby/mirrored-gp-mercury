package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import javax.inject.Inject;

/**
 * Guts behind connecting to the LIMQueries thrift service and calling service
 * methods. This is designed to be used by clients that need to make individual
 * thrift service calls, such as JAX-RS wrapper services around thrift. This
 * simplifies the client API because there's no need to worry about opening and
 * closing the connection. However, it is probably not the most efficient way to
 * perform multiple service calls.
 *
 * @author andrew
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
     * An object containing the code to call a particular thrift service method.
     * Called from ThriftConnection#call(Call).
     *
     * @param <T>    the type of result returned from the service method
     * @see ThriftConnection#call(org.broadinstitute.sequel.infrastructure.thrift.ThriftConnection.Call)
     */
    public interface Call<T> {
        T call(LIMQueries.Client client) throws TException, TZIMSException;
    }

    /**
     * Performs a Squid thrift service call by opening a connection to the
     * service endpoint, invoking the given call object (which in turn makes use
     * of the thrift client interface), and finally closes the thrift service
     * connection. Returns the result returned from the call object.
     *
     * @param call    a call object containing the call to the actual service method
     * @param <T>     the type of result returned from the service method
     * @return the result of the thrift service call
     * @throws TException when there is a problem communicating with the thrift service
     * @throws TZIMSException when there is an error in a ZIMS service method
     */
    public <T> T call(Call<T> call) throws TException, TZIMSException {
        open();
        T result = null;
        try {
            result = call.call(getClient());
        } finally {
            close();
        }
        return result;
    }

    /**
     * Opens a connection to the thrift service based on the injected config.
     * The connection must be opened before calls can be made through the
     * client interface.
     */
    private void open() {
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
    private void close() {
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
     * @see org.broadinstitute.sequel.infrastructure.thrift.ThriftConnection#open()
     */
    private LIMQueries.Client getClient() {
        if (transport == null) {
            throw new IllegalStateException("Thrift connection not open. Call ThriftConnection.open() before using the client.");
        }
        return new LIMQueries.Client(new TBinaryProtocol(transport));
    }
}
