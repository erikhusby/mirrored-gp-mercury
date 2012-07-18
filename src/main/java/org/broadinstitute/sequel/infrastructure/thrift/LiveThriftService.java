package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
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


//    @Inject
    private ThriftConfig thriftConfig;

    @Inject
    private ThriftConnection thriftConnection;

    // stateful: created by open()
    private transient TTransport transport;

    @Inject
    public LiveThriftService(ThriftConfig thriftConfig) {
        this.thriftConfig = thriftConfig;
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
    public TZamboniRun fetchRun(final String runName) throws TZIMSException, TException {
        open();
        TZamboniRun run = null;
        try {
            run = new LIMQueries.Client(new TBinaryProtocol(transport)).fetchRun(runName);
        }
        finally {
            close();
        }
        return run;
/*
        ThriftCall<TZamboniRun> call = new ThriftCall<TZamboniRun>() {
            @Override
            protected TZamboniRun doCall() throws TException, TZIMSException {
                return client.fetchRun(runName);
            }
        };
        return call.connectAndCall();
*/
    }

    public FlowcellDesignation findFlowcellDesignationByTaskName(final String taskName) throws TException, TZIMSException {
//        FlowcellDesignation flowcellDesignation = new LIMQueries.Client(new TBinaryProtocol(transport)).findFlowcellDesignationByTaskName(taskName);
        ThriftCall<FlowcellDesignation> call = new ThriftCall<FlowcellDesignation>() {
            @Override
            protected FlowcellDesignation doCall() throws TException {
                return client.findFlowcellDesignationByTaskName(taskName);
            }
        };
        return call.connectAndCall();
    }

    private abstract class ThriftCall<T> {
        protected abstract T doCall() throws TException, TZIMSException;

        protected final LIMQueries.Client client;

        protected ThriftCall() {
            client = new LIMQueries.Client(new TBinaryProtocol(transport));
        }

        public T connectAndCall() throws TException, TZIMSException {
            open();
            T result = null;
            try {
                result = doCall();
            } finally {
                close();
            }
            return result;
        }
    }
}
