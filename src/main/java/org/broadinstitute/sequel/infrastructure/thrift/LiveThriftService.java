package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.inject.Inject;

@Default
public class LiveThriftService implements  ThriftService {

    private ThriftConfiguration thriftConfig;

    // stateful: created by open()
    private TTransport transport;

    @Inject
    public LiveThriftService(ThriftConfiguration thriftConfiguration) {
        this.thriftConfig = thriftConfiguration;
    }

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

    private void close() {
        if (transport != null) {
            transport.close();
        }
    }

    @Override
    public TZamboniRun fetchRun(String runName) throws TZIMSException, TException {
        open();
        TZamboniRun run = null;
        try {
            run = new LIMQueries.Client(new TBinaryProtocol(transport)).fetchRun(runName);
        }
        finally {
            close();
        }
        return run;
    }
}
