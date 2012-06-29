package org.broadinstitute.pmbridge.infrastructure.gap;

import org.broadinstitute.pmbridge.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.pmbridge.TestGroups.EXTERNAL_INTEGRATION;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/23/12
 * Time: 10:58 AM
 */
public class GenotypingServiceExternalTest extends Arquillian {


    @BeforeMethod
    public void setUp() throws Exception {
    }

    @AfterMethod
    public void tearDown() throws Exception {
    }

    @Inject
    private GenotypingService genotypingService;

    @Deployment
    public static WebArchive buildBridgeWar() {
//        WebArchive war = DeploymentBuilder.buildBridgeWar();
        WebArchive war = DeploymentBuilder.buildBridgeWarWithAlternatives(
                GenotypingServiceImpl.class
        );
        return war;
    }


    @Test(groups = {EXTERNAL_INTEGRATION})
    public void testLookupTechnologyProductById() throws Exception {

        // <product name="HumanCytoSNP-12v1-0_D" display-name="Cyto 12" id="153"/>
        Product product = genotypingService.lookupTechnologyProductById(new Integer(236));
        Assert.assertNotNull(product);
        Assert.assertNotNull(product.getId());
        Assert.assertEquals(product.getId(), "236");
        Assert.assertEquals(product.getDisplayName(), "HumanOmni2.5-8v1_A");
        Assert.assertEquals(product.getName(), "HumanOmni2.5-8v1_A");
    }


//    @Test(groups = {EXTERNAL_INTEGRATION})
//    public void testGetPlatformRequest() throws Exception {
//        //Person creator, Date createdDate, PlatformType platformType, String subType
//        ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary(
//                new Person("mccrory", RoleType.PROGRAM_PM),
//                new Date(),
//                PlatformType.GAP,
//                ""
//        );
//        experimentRequestSummary.setExperimentId( new ExperimentId( "GXP-10601"));
//
//        GapExperimentRequest gapExperimentRequest = genotypingService.getPlatformRequest(experimentRequestSummary);
//
//        Assert.assertNotNull(gapExperimentRequest);
//        Assert.assertNotNull(gapExperimentRequest.getExperimentId());
//        Assert.assertEquals( gapExperimentRequest.getExperimentId().value, "GXP-10601" );
//
//
//    }

//    @Test(groups = {EXTERNAL_INTEGRATION})
//    public void testSaveAndRetrieveRequestToPlatform() throws Exception {
//
//        ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary  (
//                new Person("mccrory", RoleType.PROGRAM_PM),
//                new Date(),
//                PlatformType.GAP,
//                ""
//        );
//
//        experimentRequestSummary.setResearchProjectId( 111L );
//        experimentRequestSummary.setStatus( new Name("DRAFT"));
//        long id = System.currentTimeMillis();
//        experimentRequestSummary.setTitle( new Name ("FunctionalTest_ExpRequest_" + id) );
//
//        Person programMgr = new Person("pmbridge", RoleType.PROGRAM_PM);
//        GapExperimentRequest gapExperimentRequest = new GapExperimentRequest(experimentRequestSummary);
//        FundingLevel fundLevel = new FundingLevel("50", new Funding("ABC", "Test Funding Description"));
//        Quote quoteBsp = new Quote("BSP2A3", new QuoteFunding(fundLevel), ApprovalStatus.APPROVED );
//        quoteBsp.setId("2955");
//        gapExperimentRequest.setBspQuote(quoteBsp);
//
//
//        gapExperimentRequest.setTechnologyProduct(new Product("SeqChip", "T1000 Chip", "226"));
//        gapExperimentRequest.setGapGroupName("GapGroup");
//        gapExperimentRequest.setGapProjectName("GapProject");
//        Quote quoteGap = new Quote("MMM3W7", new QuoteFunding(fundLevel), ApprovalStatus.APPROVED );
//        quoteGap.setId("5047");
//        gapExperimentRequest.setGapQuote(quoteGap);
//
//        GapExperimentRequest submittedExperimentRequest = genotypingService.saveExperimentRequest(programMgr, gapExperimentRequest);
//        Assert.assertNotNull( submittedExperimentRequest );
//
//        // Now retrieve the saved experiment by Id.
//        GapExperimentRequest savedExperimentRequest = genotypingService.getPlatformRequest(submittedExperimentRequest.getExperimentRequestSummary());
//
//        Assert.assertEquals(savedExperimentRequest.getExperimentRequestSummary().getResearchProjectId(), new Long(111L));
//        Assert.assertEquals( savedExperimentRequest.getExperimentRequestSummary().getStatus().name, "DRAFT" );
//        Assert.assertTrue(savedExperimentRequest.getTitle().name.startsWith("FunctionalTest_ExpRequest_"));
//        Assert.assertEquals(savedExperimentRequest.getExperimentRequestSummary().getCreation().person.getUsername(), "pmbridge");
//
//        Assert.assertNotNull(savedExperimentRequest.getExperimentId());
//        Assert.assertNotNull(savedExperimentRequest.getExperimentId().value.startsWith("GXP-"));
//
//        Assert.assertEquals( savedExperimentRequest.getBspQuote().getAlphanumericId(), quoteBsp.getAlphanumericId() );
//        Assert.assertEquals( savedExperimentRequest.getGapQuote().getAlphanumericId(), quoteGap.getAlphanumericId() );
//
//        // has not been submitted to plaform yet so no platform managers assigned.
//        Assert.assertNull( savedExperimentRequest.getPlatformProjectManagers() );
//
//        Assert.assertEquals( savedExperimentRequest.getProgramProjectManagers().iterator().next().getUsername(), "pmbridge");
//
//        Assert.assertEquals( savedExperimentRequest.getGapGroupName(), "GapGroup");
//        Assert.assertEquals( savedExperimentRequest.getGapProjectName(), "GapProject");
//        Assert.assertEquals( savedExperimentRequest.getTechnologyProduct().getId(), "226");
//
//    }

    @Test
    public void testGetRequestSummariesByCreator() throws Exception {

    }

}
