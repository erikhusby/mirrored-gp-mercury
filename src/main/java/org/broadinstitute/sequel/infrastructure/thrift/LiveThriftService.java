package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;
import org.broadinstitute.sequel.infrastructure.deployment.Impl;

import javax.inject.Inject;

@Impl
public class LiveThriftService implements ThriftService {

    private ThriftConnection thriftConnection;

    @Inject
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
    public FlowcellDesignation findFlowcellDesignationByTaskName(final String taskName) throws TException, TZIMSException {
        return thriftConnection.call(new ThriftConnection.Call<FlowcellDesignation>() {
            @Override
            public FlowcellDesignation call(LIMQueries.Client client) throws TException, TZIMSException {
                return client.findFlowcellDesignationByTaskName(taskName);
            }
        });
    }
}
