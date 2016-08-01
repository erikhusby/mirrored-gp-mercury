package org.broadinstitute.gpinformatics.infrastructure.thrift;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.LIMQueries;
import edu.mit.broad.prodinfo.thrift.lims.PlateTransfer;
import edu.mit.broad.prodinfo.thrift.lims.PoolGroup;
import edu.mit.broad.prodinfo.thrift.lims.TZamboniRun;
import edu.mit.broad.prodinfo.thrift.lims.WellAndSourceTube;
import org.apache.commons.logging.Log;
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.easymock.IExpectationSetters;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Integration test against the development thrift service.
 * <p/>
 * // TODO: consider moving these into a LimsQueryResource integration test instead
 * <p/>
 * This test class now has a mix of integration and unit tests using mocks. I'm
 * thinking that unit tests are fine at this level because I'm just testing how
 * LiveThriftService handles exceptions. I think that perhaps
 * LimsQueryResourceTest provides enough integration testing for this, but I'm
 * still undecided, which is why I haven't yet standardized these tests one way
 * or the other.
 *
 * @author breilly
 */
@Test(singleThreaded = true, groups = EXTERNAL_INTEGRATION)
public class LiveThriftServiceTest {

    private LiveThriftService thriftService;
    private ThriftConnection mockThriftConnection;
    private Log mockLog;
    private LIMQueries.Client mockClient;

    @BeforeMethod(groups = {DATABASE_FREE, EXTERNAL_INTEGRATION})
    public void setUp() throws Exception {
        mockLog = createMock(Log.class);
        thriftService = new LiveThriftService(new ThriftConnection(ThriftConfig.produce(Deployment.DEV)), mockLog);
        mockThriftConnection = createMock(ThriftConnection.class);
        mockClient = createMock(LIMQueries.Client.class);
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchRun() throws Exception {
        TZamboniRun run = thriftService.fetchRun("120320_SL-HBN_0159_AFCC0GHCACXX");
        assertThat(run, not(nullValue()));
        assertThat(run.getRunName(), equalTo("120320_SL-HBN_0159_AFCC0GHCACXX"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchRunByBarcode() throws Exception {
        TZamboniRun run = thriftService.fetchRunByBarcode("C0GHCACXX120320");
        assertThat(run, not(nullValue()));
        assertThat(run.getRunName(), equalTo("120320_SL-HBN_0159_AFCC0GHCACXX"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchRunNotFound() throws Exception {
        mockLog.info(eq(
                "Run bogus run doesn't appear to have been registered yet.  Please try again later or contact the mercury team if the problem persists."));
        replay(mockLog);
        Assert.assertNull(thriftService.fetchRun("bogus run"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testDoesSquidRecognizeAllLibraries() throws Exception {
        boolean result1 = thriftService.doesSquidRecognizeAllLibraries(Arrays.asList("0099443960", "406164"));
        assertThat(result1, is(true));

        boolean result2 =
                thriftService.doesSquidRecognizeAllLibraries(Arrays.asList("0099443960", "406164", "unknown_barcode"));
        assertThat(result2, is(false));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchMaterialTypesForTubeBarcodes() throws Exception {
        List<String> result = thriftService.fetchMaterialTypesForTubeBarcodes(Arrays.asList("0099443960", "406164"));
        assertThat(result.size(), equalTo(2));
        assertThat(result.get(0), equalTo("454 Material-Diluted ssDNA Library"));
        assertThat(result.get(1), equalTo("454 Beads-Recovered Sequencing Beads"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchMaterialTypesForTubeBarcodesNotFound() throws Exception {
        List<String> result = thriftService.fetchMaterialTypesForTubeBarcodes(Arrays.asList("unknown_barcode"));
        assertThat(result.size(), equalTo(0));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchMaterialTypesForTubeBarcodesMixed() throws Exception {
        List<String> result = thriftService.fetchMaterialTypesForTubeBarcodes(
                Arrays.asList("0099443960", "unknown_barcode", "406164"));
        // TODO: should an error be raised here because not all tubes were found and, therefore, the index values of the query and response don't line up?
        assertThat(result.size(), equalTo(2));
        assertThat(result.get(0), equalTo("454 Material-Diluted ssDNA Library"));
        assertThat(result.get(1), equalTo("454 Beads-Recovered Sequencing Beads"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByTaskName() throws Exception {
        FlowcellDesignation flowcellDesignation = thriftService.findFlowcellDesignationByTaskName("14A_03.19.2012");
        assertThat(flowcellDesignation, not(nullValue()));
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByTaskNameNotFound() throws Exception {
        mockLog.error(eq("Thrift error. Probably couldn't find designation for task name 'invalid_task': null"),
                isA(TTransportException.class));
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
        assertThat(designation, notNullValue());
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByFlowcellBarcodeNotFound() throws Exception {
        mockLog.error(
                eq("Thrift error. Probably couldn't find designation for flowcell barcode 'invalid_flowcell': null"),
                    isA(TTransportException.class));
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

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByReagentBlockBarcode() throws Exception {
        FlowcellDesignation designation = thriftService.findFlowcellDesignationByReagentBlockBarcode("MS0000252-50");
        assertThat(designation, notNullValue());
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindFlowcellDesignationByReagentBlockBarcodeNotFound() throws Exception {
        mockLog.error(
                eq("Thrift error. Probably couldn't find designation for reagent block barcode 'invalid_reagent_block': null"),
                isA(TTransportException.class));
        replay(mockLog);

        Exception caught = null;
        try {
            thriftService.findFlowcellDesignationByReagentBlockBarcode("invalid_reagent_block");
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo("Designation not found for flowcell barcode: invalid_reagent_block"));

        verify(mockLog);
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindImmediatePlateParentsNoResult() {
        List<String> result = thriftService.findImmediatePlateParents("000000703408");
        assertThat(result.size(), equalTo(0));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindImmediatePlateParentsPlateResult() {
        List<String> result = thriftService.findImmediatePlateParents("000009873173");
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0), equalTo("000009891873"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindImmediatePlateParentsMultiplePlateResult() {
        List<String> result = thriftService.findImmediatePlateParents("000001383666");
        assertThat(result.size(), equalTo(2));
        assertThat(result.get(0), equalTo("000000010208"));
        assertThat(result.get(1), equalTo("000002458823"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFindImmediatePlateParentsUnknownPlate() {
        List<String> result = thriftService.findImmediatePlateParents("unknown_barcode");
        assertThat(result.size(), equalTo(0));
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchParentRackContentsForPlate() throws Exception {
        expectThriftCall();
        Map<String, Boolean> expected = new HashMap<>();
        expected.put("0001", true);
        expected.put("0002", false);
        expect(mockClient.fetchParentRackContentsForPlate("123456")).andReturn(expected);

        replayAll();

        LiveThriftService thriftService = new LiveThriftService(mockThriftConnection, mockLog);
        Map<String, Boolean> result = thriftService.fetchParentRackContentsForPlate("123456");
        assertThat(result, equalTo(expected));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchParentRackContentsForPlateNotFound() throws Exception {
        expectThriftCall();
        expect(mockClient.fetchParentRackContentsForPlate("123456")).andThrow(new TException("not found"));
        mockLog.error(eq("Thrift error. Probably couldn't find the plate for barcode '123456': not found"),
                isA(TException.class));

        replayAll();

        LiveThriftService thriftService = new LiveThriftService(mockThriftConnection, mockLog);
        Exception caught = null;
        try {
            thriftService.fetchParentRackContentsForPlate("123456");
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo("Plate not found for barcode: 123456"));

        verifyAll();
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchUnfulfilledDesignations() {
        List<String> result = thriftService.fetchUnfulfilledDesignations();
        // This is about all we can do because the result is going to change over time
        assertThat(result, notNullValue());
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchRelatedDesignationsForAnyTube() {
        List<String> result =
                thriftService.findRelatedDesignationsForAnyTube(Arrays.asList("0115399989", "0115399754"));
        // TODO: this is tough to test because it only returns open designations, or no content if the isn't one
        assertThat(result, notNullValue());
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchSourceTubesForPlate() {
        List<WellAndSourceTube> result = thriftService.fetchSourceTubesForPlate("000009873173");
        assertThat(result.size(), equalTo(191));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchSourceTubesForPlateNotFound() {
        Exception caught = null;
        try {
            thriftService.fetchSourceTubesForPlate("unknown_plate");
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo("Plate not found for barcode: unknown_plate"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchTransfersForPlate() {
        List<PlateTransfer> result = thriftService.fetchTransfersForPlate("000009873173", (short) 2);
        // spot check number of transfers and that one was from a rack of 95 tubes
        assertThat(result.size(), equalTo(3));
        assertThat(result.get(1).getSourcePositionMap().size(), equalTo(95));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchTransfersForPlateNotFound() {
        Exception caught = null;
        try {
            thriftService.fetchTransfersForPlate("unknown_plate", (short) 2);
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo("Plate not found for barcode: unknown_plate"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchPoolGroups() {
        List<PoolGroup> result = thriftService.fetchPoolGroups(Arrays.asList("0089526681", "0089526682"));
        assertThat(result.size(), equalTo(1));
        assertThat(result.get(0).getName(), equalTo("21490_pg"));
        assertThat(result.get(0).getTubeBarcodes(), hasItem("0089526681"));
        assertThat(result.get(0).getTubeBarcodes(), hasItem("0089526682"));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchPoolGroupsAllTubesNotFound() {
        Exception caught = null;
        try {
            thriftService.fetchPoolGroups(Arrays.asList("unknown_tube1", "unknown_tube2"));
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo("Some or all of the tubes were not found."));
    }

    @Test(groups = EXTERNAL_INTEGRATION)
    public void testFetchPoolGroupsSomeTubesNotFound() {
        Exception caught = null;
        try {
            thriftService.fetchPoolGroups(Arrays.asList("0089526681", "unknown_tube1"));
        } catch (Exception e) {
            caught = e;
        }
        assertThat(caught, instanceOf(RuntimeException.class));
        assertThat(caught.getMessage(), equalTo("Some or all of the tubes were not found."));
    }

    private IExpectationSetters<Object> expectThriftCall() {
        IExpectationSetters<Object> expect;
        expect = expect(mockThriftConnection.call(isA(ThriftConnection.Call.class))).andDelegateTo(
                new ThriftConnection(ThriftConfig.produce(Deployment.DEV)) {
                    @Override
                    public <T> T call(Call<T> call) {
                        return call.call(mockClient);
                    }
                });
        return expect;
    }

    private void replayAll() {
        replay(mockThriftConnection, mockClient, mockLog);
    }

    private void verifyAll() {
        verify(mockThriftConnection, mockClient, mockLog);
    }
}
