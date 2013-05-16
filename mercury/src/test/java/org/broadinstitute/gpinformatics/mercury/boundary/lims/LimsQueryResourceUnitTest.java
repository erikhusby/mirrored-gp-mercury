package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import edu.mit.broad.prodinfo.thrift.lims.FlowcellDesignation;
import edu.mit.broad.prodinfo.thrift.lims.TZIMSException;
import org.apache.thrift.TException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.infrastructure.thrift.ThriftService;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.lims.LimsQueryResourceResponseFactory;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.FlowcellDesignationType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexPositionType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.IndexingSchemeType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.broadinstitute.gpinformatics.mercury.boundary.lims.MercuryOrSquidRouter.MercuryOrSquid.BOTH;
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

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() throws Exception {
        mockMercuryOrSquidRouter = createMock(MercuryOrSquidRouter.class);
        mockThriftService = createMock(ThriftService.class);
        mockLimsQueries = createMock(LimsQueries.class);
        mockResponseFactory = createMock(LimsQueryResourceResponseFactory.class);
        mockTwoDBarcodedTubeDAO = createMock(TwoDBarcodedTubeDAO.class);
        mockStaticPlateDAO = createMock(StaticPlateDAO.class);
        BSPUserList bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        resource = new LimsQueryResource(mockThriftService, mockLimsQueries, mockResponseFactory, mockMercuryOrSquidRouter, bspUserList);

    }

    /*
     * Tests for findFlowcellDesignationByTaskName
     */

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskName() throws TException, TZIMSException {
        FlowcellDesignation flowcellDesignation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByTaskName("TestTask")).andReturn(flowcellDesignation);
        expect(mockResponseFactory.makeFlowcellDesignation(flowcellDesignation)).andReturn(new FlowcellDesignationType());
        replayAll();

        resource.findFlowcellDesignationByTaskName("TestTask");

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFindFlowcellDesignationByTaskNameRuntimeException() throws Exception {
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
    public void testFindFlowcellDesignationByReagentBlockBarcode() throws Exception {
        FlowcellDesignation flowcellDesignation = new FlowcellDesignation();
        expect(mockThriftService.findFlowcellDesignationByReagentBlockBarcode("TestReagentBlock")).andReturn(flowcellDesignation);
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
    public void testDoesLimsRecognizeAllTubesFromSquid() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("squid_barcode"))).andReturn(true);
//        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("squid_barcode"))).andReturn(new HashMap<String, TwoDBarcodedTube>());
        replayAll();

        boolean result = resource.doesLimsRecognizeAllTubes(Arrays.asList("squid_barcode"));
        assertThat(result, is(true));

        verifyAll();
    }

    // TODO: enable when Mercury implementation is complete
    @Test(enabled = false, groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesFromSequel() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("sequel_barcode"))).andReturn(false);
        Map<String, TwoDBarcodedTube> sequelTubes = new HashMap<String, TwoDBarcodedTube>();
        sequelTubes.put("sequel_barcode", new TwoDBarcodedTube("sequel_barcode"));
        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("sequel_barcode"))).andReturn(sequelTubes);
        replayAll();

        boolean result = resource.doesLimsRecognizeAllTubes(Arrays.asList("sequel_barcode"));
        assertThat(result, is(true));
    }

    // TODO: enable when Mercury implementation is complete
    @Test(enabled = false, groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesInBothSquidAndSequel() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("good_barcode"))).andReturn(true);
        Map<String, TwoDBarcodedTube> sequelTubes = new HashMap<String, TwoDBarcodedTube>();
        sequelTubes.put("good_barcode", new TwoDBarcodedTube("good_barcode"));
        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("good_barcode"))).andReturn(sequelTubes);
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("bad_barcode"))).andReturn(false);
        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("bad_barcode"))).andReturn(new HashMap<String, TwoDBarcodedTube>());
        replayAll();

        boolean result1 = resource.doesLimsRecognizeAllTubes(Arrays.asList("good_barcode"));
        assertThat(result1, is(true));

        boolean result2 = resource.doesLimsRecognizeAllTubes(Arrays.asList("bad_barcode"));
        assertThat(result2, is(false));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesSplitBetweenSquidAndSequel() throws Exception {
        expect(mockThriftService.doesSquidRecognizeAllLibraries(Arrays.asList("squid_barcode", "sequel_barcode"))).andReturn(false);
        Map<String, TwoDBarcodedTube> sequelTubes = new HashMap<String, TwoDBarcodedTube>();
        sequelTubes.put("sequel_barcode", new TwoDBarcodedTube("sequel_barcode"));
//        expect(mockTwoDBarcodedTubeDAO.findByBarcodes(Arrays.asList("squid_barcode", "sequel_barcode"))).andReturn(sequelTubes);
        replayAll();

        boolean result = resource.doesLimsRecognizeAllTubes(Arrays.asList("squid_barcode", "sequel_barcode"));
        // result is false because one system does not know about both tubes (TBD if this is correct behavior)
        assertThat(result, is(false));

        verifyAll();
    }

    /*
     * Tests for findImmediatePlateParents
     */

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParentsFromMercury() {
        expect(mockMercuryOrSquidRouter.routeForVessel("mercuryPlate")).andReturn(MERCURY);
        expect(mockLimsQueries.findImmediatePlateParents("mercuryPlate")).andReturn(Arrays.asList("mp1", "mp2"));
        replayAll();

        List<String> result = resource.findImmediatePlateParents("mercuryPlate");
        assertThat(result, equalTo(Arrays.asList("mp1", "mp2")));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParentsFromSquid() {
        expect(mockMercuryOrSquidRouter.routeForVessel("squidPlate")).andReturn(SQUID);
        expect(mockThriftService.findImmediatePlateParents("squidPlate")).andReturn(Arrays.asList("sp1", "sp2"));
        replayAll();

        List<String> result = resource.findImmediatePlateParents("squidPlate");
        assertThat(result, equalTo(Arrays.asList("sp1", "sp2")));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFindImmediatePlateParentsFromBoth() {
        expect(mockMercuryOrSquidRouter.routeForVessel("squidPlate")).andReturn(BOTH);
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
        expect(mockThriftService.fetchMaterialTypesForTubeBarcodes(Arrays.asList("barcode1", "barcode2"))).andReturn(Arrays.asList("type1", "type2"));
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
    public void testFindFlowcellDesignationByFlowcellBarcode() throws Exception {
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
        expect(mockMercuryOrSquidRouter.routeForVessel("mercuryPlate")).andReturn(MERCURY);
        Map<String, Boolean> map = new HashMap<String, Boolean>();
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
        expect(mockMercuryOrSquidRouter.routeForVessel("squidPlate")).andReturn(SQUID);
        Map<String, Boolean> map = new HashMap<String, Boolean>();
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
        expect(mockMercuryOrSquidRouter.routeForVessel("squidPlate")).andReturn(BOTH);
        Map<String, Boolean> map = new HashMap<String, Boolean>();
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
    public void testFetchQpcrForTube() throws Exception {
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
    public void testFetchQuantForTube() throws Exception {
        expect(mockThriftService.fetchQuantForTube("barcode", "test")).andReturn(1.23);
        replayAll();

        double result = resource.fetchQuantForTube("barcode", "test");
        assertThat(result, equalTo(1.23));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE, enabled = false)
    public void testFetchUserByBadge() throws Exception {

        String testUserBadge = "Test" + BSPManagerFactoryStub.QA_DUDE_USER_ID;
        replayAll();

        String userId = resource.fetchUserIdForBadgeId(testUserBadge);

        assertThat(userId, equalTo("QADudeTest"));

        verifyAll();
    }

    @Test(groups = DATABASE_FREE, enabled = false)
    public void testFetchNoUserByBogusBadge() throws Exception {

        String testUserBadge = "BOGUSFAKENONEXISTANTBADGE";
        replayAll();

        try {
            String userId = resource.fetchUserIdForBadgeId(testUserBadge);

            Assert.fail();
        } catch (Exception e) {

        }

        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testFetchIlluminaSeqTemplate() {

        final SequencingTemplateType template = new SequencingTemplateType();
        template.setBarcode("BARCODE_1234");
        template.setName("NAME_1234");
        template.setOnRigWorkflow("Resequencing");
        template.setOnRigChemistry("Default");
        template.setReadStructure("76T8B76T");
        template.setPairedRun(true);

        IndexingSchemeType indexingSchemeType=new IndexingSchemeType();
        indexingSchemeType.setPosition(IndexPositionType.A);
        indexingSchemeType.setSequence("AGCT");

        SequencingTemplateLaneType laneType=new SequencingTemplateLaneType();
        laneType.setIndexingScheme(indexingSchemeType);
        laneType.setLaneName("LANE_1324");
        laneType.setLoadingConcentration(3.33);
        laneType.setLoadingVesselLabel("LOADING_VESSEL_1234");

        template.getLanes().add(laneType);

        expect(resource.fetchIlluminaSeqTemplate(1234L, LimsQueries.IdType.FLOWCELL, true)).andReturn(template);
        replayAll();
        SequencingTemplateType result = resource.fetchIlluminaSeqTemplate(1234L, LimsQueries.IdType.FLOWCELL, true);

        assertThat(result, notNullValue());
        Assert.assertEquals(result.getBarcode(), "BARCODE_1234");
        Assert.assertEquals(result.getName(), "NAME_1234");
        Assert.assertEquals(result.getOnRigChemistry(), "Default");
        Assert.assertEquals(result.getOnRigWorkflow(), "Resequencing");
        Assert.assertEquals(result.getReadStructure(), "76T8B76T");
        Assert.assertEquals(result.isPairedRun(), true);
        Assert.assertEquals(result.getLanes().size(), 1);
        final SequencingTemplateLaneType laneOne = result.getLanes().get(0);
        Assert.assertEquals(laneOne.getIndexingScheme(), indexingSchemeType);
        Assert.assertEquals(laneOne.getLoadingConcentration(), 3.33);
        Assert.assertEquals(laneOne.getLoadingVesselLabel(), "LOADING_VESSEL_1234");
        Assert.assertEquals(laneOne.getLaneName(), "LANE_1324");
        Assert.assertEquals(laneOne.getIndexingScheme(), indexingSchemeType);

        verifyAll();
    }

    private void replayAll() {
        replay(mockMercuryOrSquidRouter, mockThriftService, mockLimsQueries, mockResponseFactory, mockTwoDBarcodedTubeDAO, mockStaticPlateDAO);
    }

    private void verifyAll() {
        verify(mockMercuryOrSquidRouter, mockThriftService, mockLimsQueries, mockResponseFactory, mockTwoDBarcodedTubeDAO, mockStaticPlateDAO);
    }
}
