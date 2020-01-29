package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryStub;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.GenericReagentDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ConcentrationAndVolumeAndWeightType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.PlateTransferType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ProductType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateLaneType;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.SequencingTemplateType;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests of LimsQueryResource's API behavior and interactions with other services.
 */
@Test(singleThreaded = true, groups = DATABASE_FREE)
public class LimsQueryResourceUnitTest {

    private SystemOfRecord mockSystemOfRecord;
    private LimsQueries mockLimsQueries;
    private LimsQueryResource resource;
    private StaticPlateDao mockStaticPlateDao;
    private SequencingTemplateFactory sequencingTemplateFactory;
    private GenericReagentDao mockGenericReagentDao;

    @BeforeMethod(groups = DATABASE_FREE)
    public void setUp() {
        mockSystemOfRecord = createMock(SystemOfRecord.class);
        mockLimsQueries = createMock(LimsQueries.class);
        sequencingTemplateFactory = createMock(SequencingTemplateFactory.class);
        mockStaticPlateDao = createMock(StaticPlateDao.class);
        mockGenericReagentDao = createMock(GenericReagentDao.class);
        BSPUserList bspUserList = new BSPUserList(BSPManagerFactoryProducer.stubInstance());
        resource = new LimsQueryResource(mockLimsQueries, sequencingTemplateFactory, mockSystemOfRecord,
                bspUserList, mockGenericReagentDao);

    }

    /*
     * Tests for doesLimsRecognizeAllTubes
     */

    @Test(groups = DATABASE_FREE)
    public void testDoesLimsRecognizeAllTubesFromMercury() {
        expect(mockLimsQueries.doesLimsRecognizeAllTubes(Arrays.asList("good_barcode"))).andReturn(true);
        expect(mockLimsQueries.doesLimsRecognizeAllTubes(Arrays.asList("bad_barcode"))).andReturn(false);
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
        expect(mockLimsQueries.findImmediatePlateParents("mercuryPlate")).andReturn(Arrays.asList("mp1", "mp2"));
        replayAll();

        List<String> result = resource.findImmediatePlateParents("mercuryPlate");
        assertThat(result, equalTo(Arrays.asList("mp1", "mp2")));

        verifyAll();
    }

    /*
     * Tests for fetchParentRackContentsForPlate
     */

    @Test(groups = DATABASE_FREE)
    public void testFetchParentRackContentsForPlateFromMercury() {
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
    public void testFetchConcentrationAndVolumeForTubeBarcodesFromMercury() {
        List<String> barcodes = Arrays.asList("barcode");
        Map<String, ConcentrationAndVolumeAndWeightType> map = new HashMap<>();
        ConcentrationAndVolumeAndWeightType concentrationAndVolume  = new ConcentrationAndVolumeAndWeightType();
        map.put("barcode", concentrationAndVolume);
        expect(mockLimsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(barcodes, true)).andReturn(map);
        replayAll();

        Map<String, ConcentrationAndVolumeAndWeightType> result =
                resource.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(barcodes, true);
        assertThat(result.get("barcode"), equalTo(concentrationAndVolume));

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
        expect(mockLimsQueries.fetchTransfersForPlate("barcode", 2)).andReturn(new ArrayList<PlateTransferType>());
        replayAll();

        resource.fetchTransfersForPlate("barcode", (short) 2);

        verifyAll();
    }

    @Test(groups = DATABASE_FREE, enabled = true)
    public void testFetchIlluminaSeqTemplate() {
        SequencingTemplateLaneType laneType =
                LimsQueryObjectFactory.createSequencingTemplateLaneType("LANE_1234", BigDecimal.valueOf(33.333f), "LOADING_VESSEL_1234",
                        "LOADING_VESSEL_1234");
        SequencingTemplateType template =
                LimsQueryObjectFactory
                        .createSequencingTemplate("NAME_1234", "BARCODE_1234", true, "Resequencing", "Default",
                                "76T8B76T", laneType);
        ProductType productType = new ProductType();
        productType.setName("Lims Product");
        template.getProducts().add(productType);
        template.getRegulatoryDesignation().add("RESEARCH_ONLY");
        expect(resource.fetchIlluminaSeqTemplate("12345", SequencingTemplateFactory.QueryVesselType.FLOWCELL, true))
                .andReturn(template);
        replayAll();
        SequencingTemplateType result = resource.fetchIlluminaSeqTemplate("12345",
                SequencingTemplateFactory.QueryVesselType.FLOWCELL, true);

        assertThat(result, notNullValue());
        Assert.assertEquals(result.getBarcode(), "BARCODE_1234");
        Assert.assertEquals(result.getName(), "NAME_1234");
        Assert.assertEquals(result.getOnRigChemistry(), "Default");
        Assert.assertEquals(result.getOnRigWorkflow(), "Resequencing");
        Assert.assertEquals(result.getReadStructure(), "76T8B76T");
        Assert.assertEquals(result.getProducts().size(), 1);
        ProductType productType1 = result.getProducts().get(0);
        Assert.assertEquals(productType1.getName(), "Lims Product");
        assertThat(template.getRegulatoryDesignation(), Matchers.hasSize(1));
        assertThat(template.getRegulatoryDesignation(), Matchers.hasItem("RESEARCH_ONLY"));
        Assert.assertTrue(result.isPairedRun());
        Assert.assertEquals(result.getLanes().size(), 1);
        SequencingTemplateLaneType laneOne = result.getLanes().get(0);
        Assert.assertEquals(laneOne.getLoadingConcentration(), BigDecimal.valueOf(33.333f));
        Assert.assertEquals(laneOne.getLoadingVesselLabel(), "LOADING_VESSEL_1234");
        Assert.assertEquals(laneOne.getLaneName(), "LANE_1234");
        verifyAll();
    }

    @Test(groups = DATABASE_FREE)
    public void testIsReagentNameLotExpirationRegistered() throws ParseException {
        Date expirationDate = new SimpleDateFormat("yyyy-MM-dd").parse("2016-04-28");
        expect(mockGenericReagentDao.findByReagentNameLotExpiration("SbsKitBox1", "SbsLotBarcode", expirationDate))
                .andReturn(new GenericReagent("SbsKitBox1", "SbsLotBarcode", new Date()));
        replayAll();

        Set<ReagentType> reagentTypes =
                resource.findAllReagentsListedInEventWithReagent("SbsKitBox1", "SbsLotBarcode", "2016-04-28");
        assertThat(reagentTypes, notNullValue());

        verifyAll();
    }

    private void replayAll() {
        replay(mockSystemOfRecord, mockLimsQueries, sequencingTemplateFactory, mockStaticPlateDao,
                mockGenericReagentDao);
    }

    private void verifyAll() {
        verify(mockSystemOfRecord, mockLimsQueries, sequencingTemplateFactory, mockStaticPlateDao,
                mockGenericReagentDao);
    }
}
