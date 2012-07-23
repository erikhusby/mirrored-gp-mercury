package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.sequel.infrastructure.deployment.Impl;

import javax.inject.Inject;

@Impl
public class LiveThriftService implements ThriftService {


    @Inject
    private ThriftConfig thriftConfig;

    // stateful: created by open()
    private transient TTransport transport;

    public LiveThriftService() {}

    public LiveThriftService(ThriftConfig config) {
        this.thriftConfig = config;
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
