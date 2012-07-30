package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import java.io.IOException;
import java.util.List;

@Alternative
public class MockThriftService implements ThriftService {

    public MockThriftService() {}

    @Override
    public TZamboniRun fetchRun(String runName) throws TZIMSException, TException {
        TZamboniRun run = null;

        try {
            run = ThriftFileAccessor.deserializeRun();
        }
        catch(IOException e) {
            throw new RuntimeException("Failed to deserialize run from local file",e);
        }
        return run;
    }

    @Override
    public boolean doesSquidRecognizeAllLibraries(List<String> barcodes) {
        return false;
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByTaskName(String taskName) throws TException, TZIMSException {
        return null;
    }

    @Override
    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(String flowcellBarcode) throws TException, TZIMSException {
        return null;
    }
}