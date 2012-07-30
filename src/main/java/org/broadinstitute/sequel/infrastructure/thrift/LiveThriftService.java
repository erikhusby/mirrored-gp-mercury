package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.infrastructure.deployment.Impl;

import javax.inject.Inject;
import java.util.List;

@Impl
public class LiveThriftService implements ThriftService {

    @Inject
    private ThriftConnection thriftConnection;

    public LiveThriftService() {}

    public LiveThriftService(ThriftConnection thriftConnection) {
        this.thriftConnection = thriftConnection;
    }

    @Override
    public TZamboniRun fetchRun(final String runName) throws TZIMSException, TException {
        return thriftConnection.call(new ThriftConnection.Call<TZamboniRun>() {
            @Override
            public TZamboniRun call(LIMQueries.Client client) throws TException, TZIMSException {
                return client.fetchRun(runName);
            }
        });
    }

    @Override
    public boolean doesSquidRecognizeAllLibraries(final List<String> barcodes) throws TException, TZIMSException {
        return thriftConnection.call(new ThriftConnection.Call<Boolean>() {
            @Override
            public Boolean call(LIMQueries.Client client) throws TException, TZIMSException {
                return client.doesSquidRecognizeAllLibraries(barcodes);
            }
        });
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByTaskName(final String taskName) throws TException, TZIMSException {
        return thriftConnection.call(new ThriftConnection.Call<FlowcellDesignation>() {
            @Override
            public FlowcellDesignation call(LIMQueries.Client client) throws TException, TZIMSException {
                return client.findFlowcellDesignationByTaskName(taskName);
            }
        });
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(final String flowcellBarcode) throws TException, TZIMSException {
        return thriftConnection.call(new ThriftConnection.Call<FlowcellDesignation>() {
            @Override
            public FlowcellDesignation call(LIMQueries.Client client) throws TException, TZIMSException {
                return client.findFlowcellDesignationByFlowcellBarcode(flowcellBarcode);
            }
        });
    }
}
