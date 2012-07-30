package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test against the development thrift service.
 *
 * // TODO: consider moving these into a LimsQueryResource integration test instead
 *
 * @author breilly
 */
public class LiveThriftServiceTest {

    private LiveThriftService thriftService;

    @BeforeMethod
    public void setUp() throws Exception {
        thriftService = new LiveThriftService(new ThriftConnection(ThriftConfigProducer.produce(Deployment.DEV)));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchRun() throws Exception {
        TZamboniRun run = thriftService.fetchRun("120320_SL-HBN_0159_AFCC0GHCACXX");
        assertThat(run, not(nullValue()));
    }

    @Test(groups = EXTERNAL_INTEGRATION, expectedExceptions = TZIMSException.class)
    public void testFetchRunInvalid() throws Exception {
        thriftService.fetchRun("invalid_run");
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testDoesSquidRecognizeAllLibraries() throws Exception {
        boolean result1 = thriftService.doesSquidRecognizeAllLibraries(Arrays.asList("0099443960", "406164"));
        assertThat(result1, is(true));

        boolean result2 = thriftService.doesSquidRecognizeAllLibraries(Arrays.asList("0099443960", "406164", "unknown_barcode"));
        assertThat(result2, is(false));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByTaskName() throws Exception {
        FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByTaskName("14A_03.19.2012");
        assertThat(flowcellDesignation, not(nullValue()));
    }

    // TODO: figure out why this is throwing TTransportException instead of TApplicationException
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, expectedExceptions = TTransportException.class)
    public void testFindFlowcellDesignationByTaskNameInvalid() throws Exception {
        thriftService.findFlowcellDesignationByTaskName("invalid_task");
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByFlowcellBarcode() throws Exception {
        FlowcellDesignation designation = thriftService.findFlowcellDesignationByFlowcellBarcode("C0GHCACXX");
        assertThat(designation, not(nullValue()));
    }

    // TODO: figure out why this is throwing TTransportException instead of TApplicationException
    @Test(groups = EXTERNAL_INTEGRATION, expectedExceptions = TTransportException.class)
    public void testFindFlowcellDesignationByFlowcellBarcodeInvalid() throws Exception {
        thriftService.findFlowcellDesignationByFlowcellBarcode("invalid_flowcell");
    }
}
