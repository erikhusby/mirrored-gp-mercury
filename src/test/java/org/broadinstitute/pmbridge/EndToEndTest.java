package org.broadinstitute.pmbridge;

import org.apache.log4j.Logger;
import org.broadinstitute.pmbridge.boundary.projects.ResearchProjectResource;
import org.broadinstitute.pmbridge.control.dao.ResearchProjectDAO;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollection;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.common.QuoteId;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.gap.GapExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.seq.SeqExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.PlatformType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.pmbridge.infrastructure.gap.GenotypingService;
import org.broadinstitute.pmbridge.infrastructure.gap.Product;
import org.broadinstitute.pmbridge.infrastructure.quote.*;
import org.broadinstitute.pmbridge.infrastructure.squid.SequencingService;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.pmbridge.TestGroups.EXTERNAL_INTEGRATION;
import static org.testng.Assert.fail;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/12/12
 * Time: 11:26 AM
 */
public class EndToEndTest extends Arquillian {

    private static final Logger LOG = Logger.getLogger(EndToEndTest.class);

    @Inject private ResearchProjectResource researchProjectResource;

    @Inject private QuoteService quoteService;

    @Inject private BSPSampleSearchService bspService;

    // Just for now can later be replaced by ResearchProjectResource
    @Inject private ResearchProjectDAO researchProjectDAO;

    private static final List<BSPSampleSearchColumn> DefaultMetaDataColumns = new ArrayList<BSPSampleSearchColumn>();
    static {
        for ( BSPSampleSearchColumn bspSampleSearchColumn : BSPSampleSearchColumn.values() ) {
            DefaultMetaDataColumns.add(bspSampleSearchColumn);
        }
    }


    @Inject private SequencingService sequencingService;


    @Inject private GenotypingService genotypingService;


    @Deployment
    public static WebArchive buildBridgeWar() {
        WebArchive war = DeploymentBuilder.buildBridgeWar();
        return war;
    }

    @BeforeClass
    private void init() throws MalformedURLException {
    }

    @Test(groups = {EXTERNAL_INTEGRATION})
    public void testCreateResearchProjectWithSeqExperiment() throws Exception {

        // A user (logs in) and gets created.
        Person programMgr = new Person("namrata", "Namrata", "Gupta",  "1", RoleType.PROGRAM_PM );

        ResearchProject myResearchProject = createTestResearchProject(programMgr);

        Funding funding = myResearchProject.getFundings().iterator().next();
        System.out.println("Associated funding source : " + funding.getGrantDescription() + " " +
                funding.getGrantNumber());

        //SEQUENCING EXPERIMENT REQUEST - get all summarized passes for the program PM
        List<ExperimentRequestSummary> experimentRequestSummaries = null;
//        experimentRequestSummaries = sequencingService.getRequestSummariesByCreator(new Person("mccrory", RoleType.PROGRAM_PM));
        experimentRequestSummaries = sequencingService.getRequestSummariesByCreator(programMgr);

        // Select the first experiment request Summary returned
        ExperimentRequestSummary firstSeqExperimentRequestSummary = experimentRequestSummaries.get(0);


        // Retrieve the corresponding pass experiment request for this experiment summary from SQUID.
        SeqExperimentRequest seqExperimentRequest = sequencingService.getPlatformRequest(firstSeqExperimentRequestSummary);

        System.out.println( "The Sequencing technology is : " + seqExperimentRequest.getSeqTechnology().getDisplayName() ) ;

        // Associate this sequencing experiment request with the above created new Research Project
        seqExperimentRequest.associateWithResearchProject(myResearchProject);

        // Get the first ( & only) experiment that is associated with the research project and display the association.
        ExperimentRequest experimentRequest = myResearchProject.getExperimentRequests().iterator().next();
        System.out.println( experimentRequest.getExperimentRequestSummary().getTitle().name + " refers to " +
                experimentRequest.getExperimentRequestSummary().getRemoteId().value + " and is currently associated with " +
                " research project named " + myResearchProject.getTitle().name  + " id : " + myResearchProject.getId().longValue()  );


        // Get first quote from the fundingSource that was selected above. This will be the Seq quote.
        Quote seqQuote = quoteService.getQuotesInFundingSource(funding).iterator().next();

        // Set the quote Alpha numeric Id with the seq experiment request.
        seqExperimentRequest.setSeqQuoteId( new QuoteId( seqQuote.getAlphanumericId() ) );

        // Get second quote from the fundingSource that was selected above. This will be the Bsp quote.
        Quote bspQuote = quoteService.getQuotesInFundingSource(funding).iterator().next();

        // Set the quote Alpha numeric Id with the seq experiment request for BSP
        seqExperimentRequest.setBspQuoteId( new QuoteId (bspQuote.getAlphanumericId()) );

        // Update the synopsis with a human readable timestamp  just for test purposes
        Date updateDate = new Date();
        String updateDateStr = updateDate.toLocaleString() + " - " + updateDate.getTime();
        seqExperimentRequest.setSynopsis( seqExperimentRequest.getSynopsis() + "\n" + "Timestamp : " + updateDateStr );


        // Get the samples for this cohort.
        BSPCollection bspCollection = myResearchProject.getSampleCohorts().iterator().next();
        List < String > sampleList = bspService.runSampleSearchByCohort(bspCollection);

        // Get sample meta data for all of these samples
        List<String[]> sampleMetaData = bspService.runSampleSearch(sampleList, DefaultMetaDataColumns);


        //Validate the experiment request with Squid.
        sequencingService.validatePlatformRequest(seqExperimentRequest);

        //Now submit the experiment request to Squid.
        seqExperimentRequest = sequencingService.submitRequestToPlatform(programMgr, seqExperimentRequest);



        // Later get the SEQ experiment request from SQUID
        SeqExperimentRequest submittedSeqExperimentRequest = sequencingService.getPlatformRequest(seqExperimentRequest.getExperimentRequestSummary());


        // Assert that the timestamp string in the the newly fetched experiment request.
        Assert.assertTrue( submittedSeqExperimentRequest.getSynopsis().contains( updateDateStr ) );


    }

    @Test(groups = {EXTERNAL_INTEGRATION})
    public void testSaveAndRetrieveRequestToGap() throws Exception {
        // A user (logs in) and gets created.
//        Person programMgr = new Person("shefler@broad", "Erica", "Shefler",  "1", RoleType.PROGRAM_PM );
        Person programMgr = new Person("pmbridge", RoleType.PROGRAM_PM);

        ResearchProject myResearchProject = createTestResearchProject(programMgr);

        ExperimentRequestSummary experimentRequestSummary = new ExperimentRequestSummary  (
                new Person("mccrory", RoleType.PROGRAM_PM),
                new Date(),
                PlatformType.GAP
        );

        experimentRequestSummary.setStatus( new Name("DRAFT"));
        long id = System.currentTimeMillis();
        experimentRequestSummary.setTitle( new Name ("FunctionalTest_ExpRequest_" + id) );
        experimentRequestSummary.setResearchProjectId( myResearchProject.getId() );

        GapExperimentRequest gapExperimentRequest = new GapExperimentRequest(experimentRequestSummary);
        FundingLevel fundLevel = new FundingLevel("50", new Funding("ABC", "Test Funding Description"));
        Quote quoteBsp = new Quote("BSP2A3", new QuoteFunding(fundLevel), ApprovalStatus.APPROVED );
        quoteBsp.setId("2955");
        gapExperimentRequest.setBspQuote(quoteBsp);


        gapExperimentRequest.setTechnologyProduct(new Product("SeqChip", "T1000 Chip", "226"));
        gapExperimentRequest.setGapGroupName("GapGroup");
        gapExperimentRequest.setGapProjectName("GapProject");
        Quote quoteGap = new Quote("MMM3W7", new QuoteFunding(fundLevel), ApprovalStatus.APPROVED );
        quoteGap.setId("5047");
        gapExperimentRequest.setGapQuote(quoteGap);

        // Associate this sequencing experiment request with the above created new Research Project
        gapExperimentRequest.associateWithResearchProject(myResearchProject);

        {
        // Get the first ( & only) experiment that is associated with the research project and display the association.
        ExperimentRequest experimentRequest = myResearchProject.getExperimentRequests().iterator().next();
        System.out.println( experimentRequest.getExperimentRequestSummary().getTitle().name + " refers to " +
                experimentRequest.getExperimentRequestSummary().getLocalId().value + " and is currently associated with " +
                " research project named " + myResearchProject.getTitle().name  + " id : " + myResearchProject.getId().longValue()  );
        }

        GapExperimentRequest submittedExperimentRequest = genotypingService.saveExperimentRequest(programMgr, gapExperimentRequest);
        Assert.assertNotNull( submittedExperimentRequest );

        // Now retrieve the saved experiment by Id.
        GapExperimentRequest savedExperimentRequest = genotypingService.getPlatformRequest(submittedExperimentRequest.getExperimentRequestSummary());

        Assert.assertEquals(savedExperimentRequest.getExperimentRequestSummary().getResearchProjectId(),  myResearchProject.getId() );
        Assert.assertEquals( savedExperimentRequest.getExperimentRequestSummary().getStatus().name, experimentRequestSummary.getStatus().name );
        Assert.assertTrue(savedExperimentRequest.getTitle().name.startsWith("FunctionalTest_ExpRequest_"));
        Assert.assertEquals(savedExperimentRequest.getExperimentRequestSummary().getCreation().person.getUsername(),
                experimentRequestSummary.getCreation().person.getUsername());

        Assert.assertNotNull(savedExperimentRequest.getRemoteId());
        Assert.assertNotNull(savedExperimentRequest.getRemoteId().value.startsWith("GXP-"));

        Assert.assertEquals( savedExperimentRequest.getBspQuote().getAlphanumericId(), quoteBsp.getAlphanumericId() );
        Assert.assertEquals( savedExperimentRequest.getGapQuote().getAlphanumericId(), quoteGap.getAlphanumericId() );

        // has not been submitted to plaform yet so no platform managers assigned.
        Assert.assertNull( savedExperimentRequest.getPlatformProjectManagers() );

        Assert.assertEquals( savedExperimentRequest.getProgramProjectManagers().iterator().next().getUsername(),
                programMgr.getUsername() );

        Assert.assertEquals( savedExperimentRequest.getGapGroupName(), gapExperimentRequest.getGapGroupName() );
        Assert.assertEquals( savedExperimentRequest.getGapProjectName(), gapExperimentRequest.getGapProjectName() );
        Assert.assertEquals( savedExperimentRequest.getTechnologyProduct().getId(), gapExperimentRequest.getTechnologyProduct().getId() );

    }


    private ResearchProject createTestResearchProject( Person programMgr ) throws QuoteServerException, QuoteNotFoundException {
        Date start = new Date();
        ResearchProject aResearchProject = null;


        // Instantiates a research project object
        aResearchProject = new ResearchProject(programMgr,
                new Name("MyResearchProject"), "To study stuff.");

        //COHORTS - Gets all cohorts from BSP for this program PM
        Set<BSPCollection> cohorts = bspService.getCohortsByUser(programMgr);
        for ( BSPCollection bspCollection : cohorts) {
            System.out.println("Found BSP collection : " + bspCollection.name);
        }

        // User chooses first BSP collection/cohort for test purposes.
        BSPCollection selectedSampleCollection=null;
        if (cohorts.size() > 0 ) {
            selectedSampleCollection = cohorts.iterator().next();
        } else {
            fail ( "No elements found");
        }

        // Add it to the research project.
        aResearchProject.addBSPCollection(selectedSampleCollection);
        // Check that it's been added.
        Assert.assertEquals(aResearchProject.getSampleCohorts().size(), 1);

        //FUNDING - Get some funding sources and associated quotes (from a local file for test purposes).
        Funding rpFunding = quoteService.getAllFundingSources().iterator().next();
        aResearchProject.addFunding( rpFunding );

        //IRBs - Add a couple of IRBs to the RP
        aResearchProject.addIrbNumber("irb0123");
        aResearchProject.addIrbNumber("irb0456");
        aResearchProject.setIrbNotes("There are two IRBs. One for this and the other for that.");

        //SCIENTISTS - Add a couple of scientists to the Research Project.
        // One manually created and added
        Person scientist = new Person("eric@broadinstitute.org", "Adam", "Bass", "2", RoleType.BROAD_SCIENTIST );
        aResearchProject.addSponsoringScientist( scientist );
        // & one selected from the list made available in Squid.
        List<Person> squidPeople =  sequencingService.getPlatformPeople();
        // Pick the first.
        Person squidScientist = squidPeople.get(0);
        aResearchProject.addSponsoringScientist( squidScientist );

        // Save the Research Project
        //TODO hmc - implement a persistence service for Research Project,just use DAO for now.
        aResearchProject.setId(381L);
        researchProjectDAO.saveProject( aResearchProject);

        return aResearchProject;
    }


    @Test
    public void testGetPasses() throws Exception {
        List<ExperimentRequestSummary> myExpRequestSummaries = sequencingService.getRequestSummariesByCreator(new Person("mccrory", RoleType.PROGRAM_PM));
        for (ExperimentRequestSummary summary : myExpRequestSummaries ) {
            System.out.println(summary.getLocalId() + " "
                    + summary.getRemoteId() + " "
                    + summary.getStatus() + " "
                    + summary.getModification().person.getUsername() + " "
                    + summary.getTitle() + "\t"
                    + summary.getModification().date.toLocaleString()
            );
        }
    }


//    @Test
//    public void testGettersSetters() throws Exception {
//
//    }

}
