package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;

import java.io.Serializable;
import java.util.List;

/**
 * Wrapper around thrift that helps us
 * test things without having to connect
 * to (slow) deployed services
 */
public interface ThriftService extends Serializable {

    public TZamboniRun fetchRun(String runName) throws TZIMSException, TException;

    boolean doesSquidRecognizeAllLibraries(List<String> barcodes) throws TException, TZIMSException;

    public FlowcellDesignation findFlowcellDesignationByTaskName(final String taskName) throws TException, TZIMSException;

    public FlowcellDesignation findFlowcellDesignationByFlowcellBarcode(final String flowcellBarcode) throws TException, TZIMSException;
}
