package org.broadinstitute.sequel.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import org.apache.commons.logging.Log;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.sequel.TestGroups;
import org.broadinstitute.sequel.infrastructure.deployment.Deployment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;
import static org.easymock.EasyMock.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration test against the development thrift service.
 *
 * // TODO: consider moving these into a LimsQueryResource integration test instead
 *
 * @author breilly
 */
public class LiveThriftServiceTest {

    private LiveThriftService thriftService;
    private Log mockLog;

    @BeforeMethod
    public void setUp() throws Exception {
        mockLog = createMock(Log.class);
        thriftService = new LiveThriftService(new ThriftConnection(ThriftConfigProducer.produce(Deployment.DEV)), mockLog);
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchRun() throws Exception {
        TZamboniRun run = thriftService.fetchRun("120320_SL-HBN_0159_AFCC0GHCACXX");
        assertThat(run, not(nullValue()));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchRunNotFound() throws Exception {
        String message = "Failed to fetch run: invalid_run";
        mockLog.error(eq(message), isA(TZIMSException.class));
        replay(mockLog);

        Exception caught = null;
        try {
            thriftService.fetchRun("invalid_run");
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo(message));

        verify(mockLog);
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

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByTaskNameNotFound() throws Exception {
        mockLog.error(eq("Thrift error. Probably couldn't find designation for task name 'invalid_task': null"), isA(TTransportException.class));
        replay(mockLog);

        Exception caught = null;
        try {
            thriftService.findFlowcellDesignationByTaskName("invalid_task");
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo("Designation not found for task name: invalid_task"));

        verify(mockLog);
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByFlowcellBarcode() throws Exception {
        FlowcellDesignation designation = thriftService.findFlowcellDesignationByFlowcellBarcode("C0GHCACXX");
        assertThat(designation, not(nullValue()));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByFlowcellBarcodeNotFound() throws Exception {
        mockLog.error(eq("Thrift error. Probably couldn't find designation for flowcell barcode 'invalid_flowcell': null"), isA(TTransportException.class));
        replay(mockLog);

        Exception caught = null;
        try {
            thriftService.findFlowcellDesignationByFlowcellBarcode("invalid_flowcell");
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo("Designation not found for flowcell barcode: invalid_flowcell"));

        verify(mockLog);
    }
}
