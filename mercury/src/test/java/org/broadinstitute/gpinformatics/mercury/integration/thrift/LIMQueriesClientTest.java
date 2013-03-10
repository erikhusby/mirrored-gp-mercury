package org.broadinstitute.gpinformatics.mercury.integration.thrift;

import edu.mit.broad.prodinfo.thrift.lims.*;
import org.apache.thrift.TApplicationException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftConfig;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.testng.Assert.fail;

/**
 * Integration test that explores the behavior of the LIMQueries thrift client
 * API in various positive and negative conditions. The negative conditions are
 * particularly hairy because many methods throw things like TTransportException
 * with no specific details of the error in simple cases such as a tube not
 * existing for the given barcode.
 *
 * With this behavior tested and verified, the supporting layers in Mercury can
 * use test doubles that mimic this behavior. This isolates the tests of Mercury
 * code to avoid relying on test data in the Squid database.
 *
 * @author breilly
 */
@Test(groups = EXTERNAL_INTEGRATION)
public class LIMQueriesClientTest {

    private TTransport transport;

    private LIMQueries.Client client;

    @BeforeMethod
    public void setUp() throws Exception {
        ThriftConfig config = ThriftConfig.produce(Deployment.TEST);
        transport = new TSocket(config.getHost(), config.getPort());
        transport.open();
        client = new LIMQueries.Client(new TBinaryProtocol(transport));
    }

    @AfterMethod
    public void tearDown() throws Exception {
        transport.close();
    }

    @Test
    public void testFetchRun() throws Exception {
        TZamboniRun run =  client.fetchRun("120320_SL-HBN_0159_AFCC0GHCACXX");
        assertThat(run.getRunName(), equalTo("120320_SL-HBN_0159_AFCC0GHCACXX"));
    }

    /**
     * fetchRun throws a TZIMSException with details containing the run name
     * when the run is not found.
     */
    @Test
    public void testFetchRunNotFound() throws Exception {
        try {
            client.fetchRun("unknown_run");
            fail("Should not have found run: 'unknown_run'");
        } catch (TZIMSException e) {
            assertThat(e.getDetails(), containsString("unknown_run"));
        }
    }

    @Test
    public void testFetchLibraryDetailsByTubeBarcode() throws Exception {
        List<LibraryData> datas = client.fetchLibraryDetailsByTubeBarcode(Arrays.asList("50000", "unknown_barcode"), true);
        assertThat(datas.size(), is(2));
        for (LibraryData data : datas) {
            if (data.getTubeBarcode().equals("50000")) {
                assertThat(data.getLibraryName(), equalTo("Solexa-1"));
            } else if (data.getTubeBarcode().equals("unknown_barcode")) {
                assertThat(data.getLibraryName(), equalTo("NOT_FOUND"));
            } else {
                fail("fetchLibraryDetailsByTubeBarcode returned unexpected result: " + data.getTubeBarcode());
            }
        }
    }

    @Test
    public void testDoesSquidRecognizeAllLibraries() throws Exception {
        boolean result = client.doesSquidRecognizeAllLibraries(Arrays.asList("0099443960", "406164"));
        assertThat(result, is(true));
    }

    @Test
    public void testDoesSquidRecognizeAllLibrariesNotFound() throws Exception {
        boolean result = client.doesSquidRecognizeAllLibraries(Arrays.asList("0099443960", "406164", "unknown_barcode"));
        assertThat(result, is(false));
    }

    @Test
    public void testFindFlowcellDesignationByTaskName() throws Exception {
        FlowcellDesignation flowcellDesignation = client.findFlowcellDesignationByTaskName("14A_03.19.2012");
        assertThat(flowcellDesignation.getDesignationName(), equalTo("14A_03.19.2012"));
    }

    /**
     * findFlowcellDesignationByTaskName throws TTransportException with no
     * details when the designation is not found.
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFindFlowcellDesignationByTaskNameNotFound() throws Exception {
        client.findFlowcellDesignationByTaskName("unknown_task");
    }

    @Test
    public void testFindFlowcellDesignationByFlowcellBarcode() throws Exception {
        FlowcellDesignation flowcellDesignation = client.findFlowcellDesignationByFlowcellBarcode("C0GHCACXX");
        assertThat(flowcellDesignation.getDesignationName(), equalTo("14A_03.19.2012"));
    }

    /**
     * findFlowcellDesignationByFlowcellBarcode throws TTransportException with
     * no details when the designation is not found.
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFindFlowcellDesignationByFlowcellBarcodeNotFound() throws Exception {
        client.findFlowcellDesignationByFlowcellBarcode("unknown_flowcell");
    }

    @Test
    public void testFindFlowcellDesignationByReagentBlockBarcode() throws Exception {
        FlowcellDesignation flowcellDesignation = client.findFlowcellDesignationByReagentBlockBarcode("MS0000252-50");
        assertThat(flowcellDesignation.getDesignationName(), equalTo("9A_10.26.2011"));
    }

    /**
     * findFlowcellDesignationByReagentBlockBarcode throws TTransportException
     * with no details when the designation is not found.
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFindFlowcellDesignationByReagentBlockBarcodeNotFound() throws Exception {
        client.findFlowcellDesignationByReagentBlockBarcode("invalid_reagent_block");
    }

    @Test
    public void testFetchLibraryDetailsByLibraryName() throws Exception {
        List<LibraryData> datas = client.fetchLibraryDetailsByLibraryName(Arrays.asList("Solexa-1", "unknown_library"));
        assertThat(datas.size(), equalTo(2));
        for (LibraryData data : datas) {
            if (data.getLibraryName().equals("Solexa-1")) {
                assertThat(data.getTubeBarcode(), equalTo("50000"));
            } else if (data.getLibraryName().equals("unknown_library")) {
                assertThat(data.getTubeBarcode(), equalTo("NOT_FOUND"));
            } else {
                fail("fetchLibraryDetailsByTubeBarcode returned unexpected result: " + data.getLibraryName());
            }
        }
    }

    /*
     * Can not write positive test for findRelatedDesignationsForAnyTube because
     * the test database does not contain any appropriate data. Returned
     * designation have to be "open", which means started but not completed.
     * This is a moving target at best. I'd prefer to have a positive test, but
     * it is likely that a negative test is sufficient for this method.
     */

    @Test
    public void testFindRelatedDesignationsForAnyTubeNotFound() throws Exception {
        List<String> designations = client.findRelatedDesignationsForAnyTube(Arrays.asList("unknown_tube"));
        assertThat(designations.size(), equalTo(0));
    }

    @Test
    public void testFetchUserIdForBadgeId() throws Exception {
        String userId = client.fetchUserIdForBadgeId("8f03f000f7ff12e0");
        assertThat(userId, equalTo("breilly"));
    }

    /**
     * fetchUserIdForBadgeId throws TApplicaionException with no meaningful
     * details when the user ID is not found.
     */
    @Test(expectedExceptions = TApplicationException.class)
    public void testFetchUserIdForBadgeIdNotFound() throws Exception {
        client.fetchUserIdForBadgeId("unknown_badge_id");
    }

    @Test
    public void testFetchParentRackContentsForPlate() throws Exception {
        Map<String, Boolean> result = client.fetchParentRackContentsForPlate("000003343552");
        assertThat(result.size(), equalTo(96));
        assertThat(result.get("A01"), equalTo(true));
        assertThat(result.get("B01"), equalTo(true));
        result.remove("A01");
        result.remove("B01");
        for (Map.Entry<String, Boolean> entry : result.entrySet()) {
            assertThat(entry.getKey() + " should be false", entry.getValue(), equalTo(false));
        }
    }

    /**
     * fetchParentRackContentsForPlate throws TTransportException with no
     * details when the plate is not found.
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFetchParentRackContentsForPlateNotFound() throws Exception {
        client.fetchParentRackContentsForPlate("unknown_plate");
    }

    @Test
    public void testFetchQpcrForTube() throws Exception {
        double qpcr = client.fetchQpcrForTube("0075414288");
        assertThat(qpcr, equalTo(19.37698653));
    }

    /**
     * fetchQpcrForTube throws TTransportException with no details when the tube
     * is not found.
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFetchQpcrForTubeNotFound() throws Exception {
        client.fetchQpcrForTube("unknown_tube");
    }

    /**
     * fetchQpcrForTube throws TTransportException with no details when there is
     * no QPCR data for the tube.
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFetchQpcrForTubeNoQpcr() throws Exception {
        client.fetchQpcrForTube("000001848862");
    }

    @Test
    public void testFetchQuantForTube() throws Exception {
        double quant = client.fetchQuantForTube("0108462600", "Catch Pico");
        assertThat(quant, equalTo(5.33803));
    }

    /**
     * fetchQuantForTube throws TTransportException with no details when the
     * tube is not found.
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFetchQuantForTubeNotFound() throws Exception {
        client.fetchQuantForTube("unknown_tube", "Catch Pico");
    }

    /**
     * fetchQuantForTube throws TTransportException with no details when the
     * quant type is unknown.
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFetchQuantForTubeUnknownQuant() throws Exception {
        client.fetchQuantForTube("0108462600", "Bogus Pico");
    }

    /**
     * fetchQuantForTube throws TTransportException with no details when the
     * tube and quant type exist but there is no quant value for the tube..
     */
    @Test(expectedExceptions = TTransportException.class)
    public void testFetchQuantForTubeNoQuant() throws Exception {
        client.fetchQuantForTube("000001859062", "Catch Pico");
    }
}
