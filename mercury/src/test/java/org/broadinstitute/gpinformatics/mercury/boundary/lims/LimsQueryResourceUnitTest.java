package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.PlateTransfer;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.gpinformatics.mercury.entity.limsquery.SequencingTemplate;
import org.broadinstitute.gpinformatics.mercury.entity.limsquery.SequencingTemplateLane;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.FlowcellDesignationType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.MERCURY;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.SQUID;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests of LimsQueryResource's API behavior and interactions with other services.
 */
@Test(singleThreaded = true)
public class LimsQueryResourceUnitTest {

    private MercuryOrSquidRouter mockMercuryOrSquidRouter;
    private ThriftService mockThriftService;
    private LimsQueries mockLimsQueries;
    private LimsQueryResourceResponseFactory mockResponseFactory;
    private TwoDBarcodedTubeDAO mockTwoDBarcodedTubeDAO;
    private LimsQueryResource resource;
    private StaticPlateDAO mockStaticPlateDAO;
    private SequencingTemplateFactory sequencingTemplateFactory;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() {
        mockMercuryOrSquidRouter = createMock(MercuryOrSquidRouter.class);
        mockThriftService = createMock(ThriftService.class);
        mockLimsQueries = createMock(LimsQueries.class);
        sequencingTemplateFactory = createMock(SequencingTemplateFactory.class);
        mockResponseFactory = createMock(LimsQueryResourceResponseFactory.class);
        mockTwoDBarcodedTubeDAO = createMock(TwoDBarcodedTubeDAO.class);
        mockStaticPlateDAO = createMock(StaticPlateDAO.class);
        BSPUserList bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        resource =
                new LimsQueryResource(mockThriftService, mockLimsQueries, sequencingTemplateFactory,
                        mockResponseFactory, mockMercuryOrSquidRouter, bspUserList);

    }

    /*
     * Tests for findFlowcellDesignationByTaskName
     */

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskName() throws TException, TZIMSException {
        FlowcellDesignation flowcellDesignation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andReturn(flowcellDesignation);
        expect(mockResponseFactory.makeFlowcellDesignation(flowcellDesignation))
                .andReturn(new FlowcellDesignationType());
        replayAll();

        resource.findFlowcellDesignationByTaskName("TestTask");

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskNameRuntimeException() {
        RuntimeException thrown = new RuntimeException("Runtime exception!");
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andThrow(thrown);
        replayAll();

        RuntimeException caught = null;
        try {
            resource.findFlowcellDesignationByTaskName("TestTask");
        } catch (RuntimeException e) {
            caught = e;
        }
        assertThat(caught.getMessage(), equalTo(thrown.getMessage()));

        verifyAll();
    }

    /*
     * Tests for findFlowcellDesignationByReagentBlockBarcode
     */

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByReagentBlockBarcode() {
        FlowcellDesignation flowcellDesignation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock"))
                .andReturn(flowcellDesignation);
        FlowcellDesignationType expected = new FlowcellDesignationType();
        expect(mockResponseFactory.makeFlowcellDesignation(flowcellDesignation)).andReturn(expected);
        replayAll();

        FlowcellDesignationType result = resource.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock");
        assertThat(result, equalTo(expected));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByReagentBlockBarcodeNotFound() {
        RuntimeException thrown = new RuntimeException("Not found");
        expect(mockThriftService.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock")).andThrow(thrown);
        replayAll();

        RuntimeException caught = null;
        try {
            resource.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock");
        } catch (RuntimeException e) {
            caught = e;
        }
        assertThat(caught.getMessage(), equalTo(thrown.getMessage()));

        verifyAll();
    }

    /*
     * Tests for doesLimsRecognizeAllTubes
     */

    @Test(groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesFromSquid() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVesselBarcodes(Arrays.asList("squid_barcode"))).andReturn(SQUID);
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("squid_barcode"))).andReturn(true);
        replayAll();

        boolean result = resource.doesLimsRecognizeAllTubes(Arrays.asList("squid_barcode"));
        assertThat(result, is(true));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesFromMercury() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVesselBarcodes(Arrays.asList("good_barcode"))).
                andReturn(MERCURY);
        expect(mockLimsQueries.doesLimsRecognizeAllTubes(Arrays.asList("good_barcode"))).andReturn(true);

        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVesselBarcodes(Arrays.asList("bad_barcode"))).
                andReturn(SQUID);
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("bad_barcode"))).andReturn(false);
        replayAll();

        boolean result1 = resource.doesLimsRecognizeAllTubes(Arrays.asList("good_barcode"));
        assertThat(result1, is(true));

        boolean result2 = resource.doesLimsRecognizeAllTubes(Arrays.asList("bad_barcode"));
        assertThat(result2, is(false));

        verifyAll();
    }

    /*
     * Tests for findImmediatePlateParents
     */

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParentsFromMercury() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("mercuryPlate")).andReturn(MERCURY);
        expect(mockLimsQueries.findImmediatePlateParents("mercuryPlate")).andReturn(Arrays.asList("mp1", "mp2"));
        replayAll();

        List<String> result = resource.findImmediatePlateParents("mercuryPlate");
        assertThat(result, equalTo(Arrays.asList("mp1", "mp2")));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParentsFromSquid() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("squidPlate")).andReturn(SQUID);
        expect(mockThriftService.findImmediatePlateParents("squidPlate")).andReturn(Arrays.asList("sp1", "sp2"));
        replayAll();

        List<String> result = resource.findImmediatePlateParents("squidPlate");
        assertThat(result, equalTo(Arrays.asList("sp1", "sp2")));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParentsFromBoth() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("squidPlate")).andReturn(SQUID);
        expect(mockThriftService.findImmediatePlateParents("squidPlate")).andReturn(Arrays.asList("sp1", "sp2"));
        replayAll();

        List<String> result = resource.findImmediatePlateParents("squidPlate");
        assertThat(result, equalTo(Arrays.asList("sp1", "sp2")));

        verifyAll();
    }


    /*
     * Tests for fetchMaterialTypesForTubeBarcodes
     */

    @Test(groups = DATABASE_FREE)
    public void testFetchMaterialTypesForTubeBarcodes() {
        expect(mockThriftService.fetchMaterialTypesForTubeBarcodes(Arrays.asList("barcode1", "barcode2")))
                .andReturn(Arrays.asList("type1", "type2"));
        replayAll();

        List<String> result = resource.fetchMaterialTypesForTubeBarcodes(Arrays.asList("barcode1", "barcode2"));
        assertThat(result.size(), equalTo(2));
        assertThat(result.get(0), equalTo("type1"));
        assertThat(result.get(1), equalTo("type2"));

        verifyAll();
    }

    /*
     * Tests for findFlowcellDesignationByFlowcellBarcode
     */

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByFlowcellBarcode() {
        FlowcellDesignation designation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByFlowcellBarcode("good_barcode")).andReturn(designation);
        expect(mockResponseFactory.makeFlowcellDesignation(designation)).andReturn(new FlowcellDesignationType());
        replayAll();

        FlowcellDesignationType result = resource.findFlowcellDesignationByFlowcellBarcode("good_barcode");

        assertThat(result, notNullValue());

        verifyAll();
    }

    /*
     * Tests for fetchParentRackContentsForPlate
     */

    @Test(groups = DATABASE_FREE)
    public void testFetchParentRackContentsForPlateFromMercury() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("mercuryPlate")).andReturn(MERCURY);
        Map<String, Boolean> map = new HashMap<>();
        map.put("A01", true);
        map.put("A02", false);
        expect(mockLimsQueries.fetchParentRackContentsForPlate("mercuryPlate")).andReturn(map);
        replayAll();

        Map<String, Boolean> result = resource.fetchParentRackContentsForPlate("mercuryPlate");
        assertThat(result, equalTo(map));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchParentRackContentsForPlateFromSquid() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("squidPlate")).andReturn(SQUID);
        Map<String, Boolean> map = new HashMap<>();
        map.put("A01", true);
        map.put("A02", false);
        expect(mockThriftService.fetchParentRackContentsForPlate("squidPlate")).andReturn(map);
        replayAll();

        Map<String, Boolean> result = resource.fetchParentRackContentsForPlate("squidPlate");
        assertThat(result, equalTo(map));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchParentRackContentsForPlateFromBoth() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("squidPlate")).andReturn(SQUID);
        Map<String, Boolean> map = new HashMap<>();
        map.put("A01", true);
        map.put("A02", false);
        expect(mockThriftService.fetchParentRackContentsForPlate("squidPlate")).andReturn(map);
        replayAll();

        Map<String, Boolean> result = resource.fetchParentRackContentsForPlate("squidPlate");
        assertThat(result, equalTo(map));

        verifyAll();
    }

    /*
     * Tests for fetchQpcrForTube
     */

    @Test(groups = DATABASE_FREE)
    public void testFetchQpcrForTube() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("barcode")).andReturn(SQUID);
        expect(mockThriftService.fetchQpcrForTube("barcode")).andReturn(1.23);
        replayAll();

        double result = resource.fetchQpcrForTube("barcode");
        assertThat(result, equalTo(1.23));

        verifyAll();
    }

    /*
     * Tests for fetchQuantForTube
     */

    @Test(groups = DATABASE_FREE)
    public void testFetchQuantForTube() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("barcode")).andReturn(SQUID);
        expect(mockThriftService.fetchQuantForTube("barcode", "test")).andReturn(1.23);
        replayAll();

        double result = resource.fetchQuantForTube("barcode", "test");
        assertThat(result, equalTo(1.23));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE, enabled = false)
    public void testFetchUserByBadge() {

        String testUserBadge = "Test" + BSPManagerFactoryStub.QA_DUDE_USER_ID;
        replayAll();

        String userId = resource.fetchUserIdForBadgeId(testUserBadge);

        assertThat(userId, equalTo("QADudeTest"));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE, enabled = false)
    public void testFetchNoUserByBogusBadge() {

        String testUserBadge = "BOGUSFAKENONEXISTANTBADGE";
        replayAll();

        try {
            String userId = resource.fetchUserIdForBadgeId(testUserBadge);

            Assert.fail();
        } catch (Exception e) {

        }

        verifyAll();
    }

    /**
     * Test that fetchTransfersForPlate calls the Mercury version of the lookup when the router says that Mercury is the
     * system of record for the plate.
     */
    @Test(groups = DATABASE_FREE)
    public void testFetchTransfersForPlateForMercury() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("barcode")).andReturn(MERCURY);
        expect(mockLimsQueries.fetchTransfersForPlate("barcode", 2)).andReturn(new ArrayList<PlateTransferType>());
        replayAll();

        resource.fetchTransfersForPlate("barcode", (short) 2);

        verifyAll();
    }

    /**
     * Test that fetchTransfersForPlate calls the Squid version of the lookup when the router says that Squid is the
     * system of record for the plate.
     */
    @Test(groups = DATABASE_FREE)
    public void testFetchTransfersForPlateForSquid() {
        expect(mockMercuryOrSquidRouter.getSystemOfRecordForVessel("barcode")).andReturn(SQUID);
        expect(mockThriftService.fetchTransfersForPlate("barcode", (short) 2)).andReturn(new ArrayList<PlateTransfer>());
        replayAll();

        resource.fetchTransfersForPlate("barcode", (short) 2);

        verifyAll();
    }

    @Test(groups = DATABASE_FREE, enabled = true)
    public void testFetchIlluminaSeqTemplate() {
        SequencingTemplateLane laneType =
                new SequencingTemplateLane("LANE_1234", 33.333, "LOADING_VESSEL_1234");
        SequencingTemplate template =
new SequencingTemplate("NAME_1234", "BARCODE_1234", true, "Resequencing", "Default",
                                "76T8B76T", Arrays.asList(laneType));
        expect(resource.fetchIlluminaSeqTemplate("12345", SequencingTemplateFactory.QueryVesselType.FLOWCELL, true))
                .andReturn(template);
        replayAll();
        SequencingTemplate result = resource.fetchIlluminaSeqTemplate("12345",
                SequencingTemplateFactory.QueryVesselType.FLOWCELL, true);

        assertThat(result, notNullValue());
        Assert.assertEquals(result.getBarcode(), "BARCODE_1234");
        Assert.assertEquals(result.getName(), "NAME_1234");
        Assert.assertEquals(result.getOnRigChemistry(), "Default");
        Assert.assertEquals(result.getOnRigWorkflow(), "Resequencing");
        Assert.assertEquals(result.getReadStructure(), "76T8B76T");
        Assert.assertTrue(result.isPairedRun());
        Assert.assertEquals(result.getLanes().size(), 1);
        SequencingTemplateLaneType laneOne = result.getLanes().get(0);
        Assert.assertEquals(laneOne.getLoadingConcentration(), 33.333);
        Assert.assertEquals(laneOne.getLoadingVesselLabel(), "LOADING_VESSEL_1234");
        Assert.assertEquals(laneOne.getLaneName(), "LANE_1324");
        verifyAll();
    }

    private void replayAll() {
        replay(mockMercuryOrSquidRouter, mockThriftService, mockLimsQueries, sequencingTemplateFactory,
                mockResponseFactory, mockTwoDBarcodedTubeDAO, mockStaticPlateDAO);
    }

    private void verifyAll() {
        verify(mockMercuryOrSquidRouter, mockThriftService, mockLimsQueries, sequencingTemplateFactory,
                mockResponseFactory, mockTwoDBarcodedTubeDAO, mockStaticPlateDAO);
    }
}
