package org.broadinstitute.pmbridge;

import org.apache.log4j.Logger;
import org.broadinstitute.pmbridge.boundary.projects.ResearchProjectResource;
import org.broadinstitute.pmbridge.control.dao.ResearchProjectDAO;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollection;
import org.broadinstitute.pmbridge.entity.common.Name;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequest;
import org.broadinstitute.pmbridge.entity.experiments.ExperimentRequestSummary;
import org.broadinstitute.pmbridge.entity.experiments.seq.SeqExperimentRequest;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.broadinstitute.pmbridge.entity.person.RoleType;
import org.broadinstitute.pmbridge.entity.project.ResearchProject;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.pmbridge.infrastructure.quote.*;
import org.broadinstitute.pmbridge.infrastructure.squid.SequencingService;
import org.broadinstitute.pmbridge.quotes.QuotesCacheTestUtil;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.util.*;

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

    ResearchProject myResearchProject = null;

    @Inject private SequencingService sequencingService;

    @Deployment
    public static WebArchive buildBridgeWar() {
        WebArchive war = DeploymentBuilder.buildBridgeWar();
        return war;
    }

    @BeforeClass
    private void init() throws MalformedURLException {
    }

    @Test
    public void testCreateResearchProjectWithSeqExperiment() throws Exception {
        Date start = new Date();

        // A user (logs in) and gets created.
        Person programMgr = new Person("shefler@broad", "Erica", "Shefler",  "1", RoleType.PROGRAM_PM );

        // Instantiates a research project object
        myResearchProject = new ResearchProject(programMgr,
                new Name("MyResearchProject"), "To study stuff.");

        //COHORTS - Gets all cohorts from BSP for this program PM
        List<BSPCollection> cohorts = bspService.getCohortsByUser(programMgr);
        for ( BSPCollection bspCollection : cohorts) {
            System.out.println("Found BSP collection : " + bspCollection.name);
        }

        // User chooses first BSP collection/cohort for test purposes.
        BSPCollection selectedSampleCollection = cohorts.get(0);

        // Add it to the research project.
        myResearchProject.addBSPCollection( selectedSampleCollection );
        // Check that it's been added.
        Assert.assertEquals( myResearchProject.getSampleCohorts().size(), 1 );

        //FUNDING - Get some funding sources and associated quotes (from a local file for test purposes).
        QuotesCacheTestUtil quotesCacheTestUtil = new QuotesCacheTestUtil();
        QuotesCache quotesCache = quotesCacheTestUtil.getLocalQuotes("quoteTestData.xml");
        Map<Funding, HashSet<Quote>> quotesByFundingSource = quotesCache.getQuotesByFundingSource();

        // Choose the first funding source and associate it with the RP
        Funding funding = quotesByFundingSource.keySet().iterator().next();
        Collection<Funding> fundings = myResearchProject.addFunding(funding);
        Funding rpFunding = fundings.iterator().next();

        System.out.println("Chose funding source : " + rpFunding.getGrantDescription() + " " +
                rpFunding.getGrantNumber());


        //IRBs - Add a couple of IRBs to the RP
        myResearchProject.addIrbNumber("irb0123");
        myResearchProject.addIrbNumber("irb0456");
        myResearchProject.setIrbNotes("There are two IRBs. One for this and the other for that.");


        //SCIENTISTS - Add a couple of scientists to the Research Project.
        // One manually created and added
        Person scientist = new Person("eric@broadinstitute.org", "Adam", "Bass", "2", RoleType.BROAD_SCIENTIST );
        myResearchProject.addSponsoringScientist( scientist );
        // & one selected from the list made available in Squid.
        List<Person> squidPeople =  sequencingService.getPlatformPeople();
        // Pick the first.
        Person squidScientist = squidPeople.get(0);
        myResearchProject.addSponsoringScientist( squidScientist );

        // Save the Research Project
        //TODO hmc - implement a persistence service for Research Project,just use DAO for now.
        myResearchProject.setId( 381L );
        researchProjectDAO.saveProject( myResearchProject);


        //SEQUENCING EXPERIMENT REQUEST - get all summarized passes for the program PM
        List<ExperimentRequestSummary> experimentRequestSummaries = null;
        experimentRequestSummaries = sequencingService.getRequestSummariesByCreator(new Person("mccrory", RoleType.PROGRAM_PM));

        // Select the first experiment request Summary returned
        ExperimentRequestSummary firstSeqExperimentRequestSummary = experimentRequestSummaries.get(0);


        // Retrieve the corresponding pass experiment request for this experiment summary from SQUID.
        SeqExperimentRequest seqExperimentRequest = sequencingService.getPlatformRequest(firstSeqExperimentRequestSummary);


        // Associate this sequencing experiment request with the above created new Research Project
        seqExperimentRequest.associateWithResearchProject(myResearchProject);

        // Get the first ( & only) experiment that is associated with the research project and display the association.
        ExperimentRequest experimentRequest = myResearchProject.getExperimentRequests().iterator().next();
        System.out.println( experimentRequest.getExperimentRequestSummary().getTitle().name + " refers to " +
                experimentRequest.getExperimentRequestSummary().getRemoteId().value + " and is currently associated with " +
                " research project named " + myResearchProject.getTitle().name  + " id : " + myResearchProject.getId().longValue()  );


        // Get first quote from the fundingSource that was selected above. This will be the Seq quote.
        Quote seqQuote = quotesByFundingSource.get(funding).iterator().next();

        // Set the quote Alpha numeric Id with the seq experiment request.
        seqExperimentRequest.setSeqQuoteId( seqQuote.getAlphanumericId() );

        // Get second quote from the fundingSource that was selected above. This will be the Bsp quote.
        Quote bspQuote = quotesByFundingSource.get(funding).iterator().next();

        // Set the quote Alpha numeric Id with the seq experiment request for BSP
        seqExperimentRequest.setBspQuoteId( bspQuote.getAlphanumericId() );

        // Update the synopsis with a human readable timestamp  just for test purposes
        Date updateDate = new Date();
        String updateDateStr = updateDate.toLocaleString() + " - " + updateDate.getTime();
        seqExperimentRequest.setSynopsis( "Timestamp : " + updateDateStr );


        // Get the samples for this cohort.
        List < String > sampleList = bspService.runSampleSearchByCohort(selectedSampleCollection);

        // Get sample meta data for all of these samples
        List<String[]> sampleMetaData = bspService.runSampleSearch(sampleList, DefaultMetaDataColumns);


        //Validate the experiment request with Squid.
        sequencingService.validatePlatformRequest(seqExperimentRequest);

        //Now submit the experiment request to Squid.
        seqExperimentRequest = sequencingService.submitRequestToPlatform(seqExperimentRequest);



        // Later get the SEQ experiment request from SQUID
        SeqExperimentRequest submittedSeqExperimentRequest = sequencingService.getPlatformRequest(seqExperimentRequest.getExperimentRequestSummary());


        // Assert that the timestamp string in the the newly fetched experiment request.
        Assert.assertTrue( submittedSeqExperimentRequest.getSynopsis().contains( updateDateStr ) );


    }


    @Test
    public void createGapExperimentRequest() throws Exception {

        //TODO -
//        Date stop = new Date();
//        FundingSource fundingSource1 = new FundingSource(new GrantId("100"), new Name("SmallGrant"), start, stop,
//                new Name("NIH") );
//        FundingSource fundingSource2 = new FundingSource(new GrantId("200"), new Name("OtherGrant"), start, stop,
//                new Name("NHGRI") );

//        // Create a GAP Experiment Request
//        Person platformManager1 = new Person("Rob", "Onofrio", "onofrio@broad", "2", RoleType.PLATFORM_PM );
//        Person platformManager2 = new Person("Maeghan", "harden", "harden@broad", "3", RoleType.PLATFORM_PM );
//        List gapPlatformManagers =  new ArrayList<Person>();
//        gapPlatformManagers.add(platformManager1);
//        gapPlatformManagers.add(platformManager2);
//
//        List gapProgramManagers =  new ArrayList<Person>();
//        gapProgramManagers.add(programMgr);

        //TODO
//        ExperimentPlan expPlan = new ExperimentPlan();
//        expPlan.setProjectName();
        /*
               GapExperimentRequest gapExperimentRequest = new GapExperimentRequest(
                       new RemoteId("GAP"), platformManagers, programManagers, null, new QuoteId("BSP-123"), new QuoteId("GAN-123") );


               researchProject.addExperimentRequest(gapExperimentRequest);


               // Retrieve a Sequencing Experiment Request
               final String PASS_NUMBER = "PASS-5447";
               final String PASS_TITLE = "1000 Genomes Seq Based Validation Using Custom Bait (Plate 2)";


               //Get a PASS from Squid.
               AbstractPass pass = squidServicePort.loadPassByNumber(PASS_NUMBER);


               // Verify that the the tile and number of the retrieved Pass is what we expect.
               Assert.assertEquals(pass.getProjectInformation().getTitle(), PASS_TITLE );
               Assert.assertEquals(pass.getProjectInformation().getPassNumber(), PASS_NUMBER);



               SeqExperimentRequest passExperimentRequest = new SeqExperimentRequest(programMgr, new ExperimentId("PASS-5555"),
                       new Name("aPassTitle"),
                       new RemoteId("SEQ"), platformManagers, programManagers, null, new QuoteId("BSP-123"), new QuoteId("GAN-123") );

               passExperimentRequest.setPass(pass);

               researchProject.addExperimentRequest(passExperimentRequest);

               Assert.assertEquals(researchProject.getExperimentRequests().size(), 2 );

               System.out.println(researchProject.toString());
        */

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
