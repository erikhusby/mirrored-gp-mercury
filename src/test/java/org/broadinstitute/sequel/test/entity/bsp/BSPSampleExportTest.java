package org.broadinstitute.sequel.test.entity.bsp;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.queue.AliquotParameters;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.bsp.BSPConfig;
import org.broadinstitute.sequel.infrastructure.bsp.plating.BSPPlatingRequestService;
import org.broadinstitute.sequel.infrastructure.bsp.plating.BSPPlatingRequestServiceImpl;
import org.broadinstitute.sequel.infrastructure.bsp.plating.BSPPlatingRequestServiceProducer;
import org.broadinstitute.sequel.infrastructure.bsp.plating.BSPPlatingRequestServiceStub;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.Funding;
import org.broadinstitute.sequel.infrastructure.quote.FundingLevel;
import org.broadinstitute.sequel.infrastructure.quote.QuoteFunding;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.*;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;
import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class BSPSampleExportTest extends ContainerTest {

    public static final String masterSample1 = "SM-1111";
    public static final String masterSample2 = "SM-2222";

    //private String aliquot1LSID = "broadinstitute.org:bsp.prod.sample:Test Aliquot 1111";
    //private String aliquot2LSID = "broadinstitute.org:bsp.prod.sample:Test Aliquot 2222";

    @Inject
    BSPConfig bspConfig;

    @Inject
    private Log log;

    @Test(groups = {DATABASE_FREE})
    public void testExport() throws Exception {

        BasicProject project = new BasicProject("BSPExportTestingProject", new JiraTicket());
        // BasicProjectPlan
        BasicProjectPlan projectPlan = new BasicProjectPlan(
                project,
                "ExomeExpressPlan1",
                new WorkflowDescription("HybridSelection", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));


        String quoteString = "DNA385";
        Quote billingQuote = new org.broadinstitute.sequel.entity.billing.Quote(quoteString,
                new org.broadinstitute.sequel.infrastructure.quote.Quote(quoteString,new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NCI")))));
        projectPlan.setQuote(billingQuote);

        StartingSample startingSample = new BSPStartingSample(masterSample1, projectPlan);
        projectPlan.getStarters().add(startingSample);

        StartingSample startingSample2 = new BSPStartingSample(masterSample2, projectPlan);
        projectPlan.getStarters().add(startingSample2);

        BSPPlatingExportEntityBuilder bspExportEntityBuilder = new BSPPlatingExportEntityBuilder(projectPlan);
        bspExportEntityBuilder.runTest();

    }

    @Test(groups = {EXTERNAL_INTEGRATION} , enabled = true)
    //@Test
    public void testIssueBSPPlating() throws Exception {

        log.info("running IssueBSPPlating test");

        //BSPPlatingRequestService platingService = BSPPlatingRequestServiceProducer.qaInstance();
        BSPPlatingRequestServiceProducer producer = new BSPPlatingRequestServiceProducer();
        BSPPlatingRequestService platingService = producer.produce(new BSPPlatingRequestServiceStub(), new BSPPlatingRequestServiceImpl(bspConfig));

        //BSPPlatingRequestService platingService =

        BasicProject project = new BasicProject("BSPPlatingTestingProject", new JiraTicket());
        // BasicProjectPlan
        BasicProjectPlan projectPlan = new BasicProjectPlan(
                project,
                "ExomeExpressPlan1",
                new WorkflowDescription("HybridSelection", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

        String quoteString = "DNA385";
        Quote billingQuote = new Quote(quoteString,
                new org.broadinstitute.sequel.infrastructure.quote.Quote(quoteString,new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"NCI")))));
        projectPlan.setQuote(billingQuote);

        StartingSample startingSample = new BSPStartingSample(masterSample1, projectPlan);
        projectPlan.getStarters().add(startingSample);

        StartingSample startingSample2 = new BSPStartingSample(masterSample2, projectPlan);
        projectPlan.getStarters().add(startingSample2);

        //System.out.println("Quote :" + projectPlan.getQuoteDTO().getId());
        //System.out.println("Quote Alpha :" + projectPlan.getQuoteDTO().getAlphanumericId());
        Map<StartingSample, AliquotParameters> starterMap = new HashMap<StartingSample, AliquotParameters>();
        for (Starter starter : projectPlan.getStarters()) {
            starterMap.put((StartingSample)starter, new AliquotParameters(projectPlan, 1.9f, 1.6f));
        }

        BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
        bspSampleFactory.issueBSPPlatingRequest(starterMap, null, 0, null, null, null, "BSP Plating Test", "Illumina"
        , "sampath" , "ExomeExpress-LCSET-JIRA-111" , "BSP Plating from Exome Express");

        //BSPPlatingExportEntityBuilder bspExportEntityBuilder = new BSPPlatingExportEntityBuilder(projectPlan);
        //bspExportEntityBuilder.runTest();
    }

    public static class BSPPlatingExportEntityBuilder {

        private static final String ALIQUOT_LSID_PATTERN = "broadinstitute.org:bsp.prod.sample:Test Aliquot ";
        private ProjectPlan projectPlan = null;

        public BSPPlatingExportEntityBuilder(ProjectPlan projectPlan) {
            if (projectPlan == null) {
                throw new IllegalArgumentException("Invalid Project Plan");
            }
            this.projectPlan = projectPlan;
        }

        public void runTest() throws Exception {
            BSPPlatingReceipt bspReceipt = new BSPPlatingReceipt("WR-1234");
            if (projectPlan == null) {
                throw new IllegalArgumentException("Invalid Project plan ");
            }

            Iterator<Starter> stockItr = projectPlan.getStarters().iterator();

            String startingStock = null;
            while (stockItr.hasNext()) {
                startingStock = stockItr.next().getLabel();

                //TODO .. BSPAliquotWorkQueue & SampleSheet to be deprecated/deleted
                // request an aliquot from bsp
                AliquotParameters aliquotParameters = new AliquotParameters(projectPlan, 1.9f, 1.6f);

                //generate BSPPlatingRequests
                BSPPlatingRequest bspPlatingRequest = new BSPPlatingRequest(startingStock, aliquotParameters);
                bspPlatingRequest.setReceipt(bspReceipt);
                projectPlan.getPendingPlatingRequests().add(bspPlatingRequest);
            }

            Collection<Starter> starters = projectPlan.getStarters();
            Assert.assertNotNull(starters);
            Assert.assertEquals(starters.size() , 2, "Project Plan should have 2 starters");
            //TODO :- use IssueBSPPlatingRequest mock to get some aliquots
            //for now hardcode
            Map<String, StartingSample> aliquotSourceMap = new HashMap<String, StartingSample>();
            Collection<BSPPlatingRequest> bspPlatingRequests = bspReceipt.getPlatingRequests();
            String stockName = null;
            String aliquotLSID = null;
            StartingSample bspStock = null;
            for (BSPPlatingRequest bspRequest : bspPlatingRequests) {
                stockName = bspRequest.getSampleName();
                String[] stockNameChunks = stockName.split("-"); //assuming stockName is in format SM-99999
                if (stockNameChunks.length > 1) {
                    aliquotLSID = ALIQUOT_LSID_PATTERN + stockNameChunks[stockNameChunks.length - 1];
                } else {
                    aliquotLSID = ALIQUOT_LSID_PATTERN + stockName;
                }

                ProjectPlan projPlan = bspRequest.getAliquotParameters().getProjectPlan();
                Assert.assertNotNull(projPlan);
                Assert.assertNotNull(projPlan.getStarters());
                Assert.assertEquals(2, projPlan.getStarters().size());
                for (Starter starter : projPlan.getStarters()) {
                    if (starter.getLabel().equals(stockName)) {
                        bspStock = (StartingSample) starter;
                    }
                }

                //TODO ..don't new up BSPStartingSample .. get from project Plan
                aliquotSourceMap.put(aliquotLSID, bspStock);
            }

            BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
            List<LabVessel> bspAliquots = bspSampleFactory.receiveBSPAliquots(bspReceipt, aliquotSourceMap, null);

            //now iterate through these aliquots, do some asserts and see if we can navigate back to requests.
            Assert.assertNotNull(bspAliquots);
            Assert.assertEquals(bspAliquots.size(), starters.size() , "BSP Aliquot size should match Staters size");
            for (Starter aliquot : bspAliquots) {
                Assert.assertTrue(aliquot.getLabel().contains("Aliquot"));
                //check the source stock sample of each aliquot
                BSPSampleAuthorityTwoDTube bspAliquot = (BSPSampleAuthorityTwoDTube) aliquot;
                Project testProject = bspAliquot.getAllProjects().iterator().next();
                //Assert.assertEquals("BSPExportTestingProject", testProject.getProjectName());
                Assert.assertEquals(projectPlan.getProject().getProjectName(), testProject.getProjectName());
                //navigate from aliquot to ----> BSPStartingSample - !!
                StartingSample stockAliquot = bspAliquot.getSampleInstances().iterator().next().getStartingSample();
                Assert.assertNotNull(stockAliquot);
                ProjectPlan projPlan = stockAliquot.getRootProjectPlan();
                Assert.assertNotNull(projPlan);
                Collection<Starter> starterStocks = projPlan.getStarters();
                //should have starters
                Assert.assertNotNull(starterStocks);
                //Iterator<Starter> starterStocksIterator = starterStocks.iterator();
                Assert.assertEquals(2, starterStocks.size());
            }

            //iterate through Starters (BSPStartingSample) and make sure they all have aliquots
            Collection<Starter> starterStocks = projectPlan.getStarters();
            //should have starter
            Assert.assertNotNull(starterStocks);
            Iterator<Starter> starterStocksIterator = starterStocks.iterator();
            Assert.assertEquals(bspAliquots.size(), starterStocks.size());
            while (starterStocksIterator.hasNext()) {
                Starter starter = starterStocksIterator.next();
                LabVessel aliquot = projectPlan.getAliquot(starter);
                Assert.assertNotNull(aliquot);
                Assert.assertEquals(true, aliquot.getLabel().contains("Aliquot"));
                //assumed stock is in format SM-9999
                String[] stockChunks = starter.getLabel().split("-");
                String stockNum = null;
                if (stockChunks.length > 1) {
                    stockNum = stockChunks[stockChunks.length - 1];
                } else {
                    stockNum = stockName;
                }

                //aliquot label should contain stock number
                Assert.assertEquals(true, aliquot.getLabel().contains(stockNum));
            }

            //return bspAliquots ??
        }

    }

    //TODO
    //Test Factory that takes a GSSRSampleKitRequest (JAXB object) and builds required entity Map

    private GSSRSampleKitRequest buildJAXBRequest(Map<String, String> stockAliquotMap) {
        GSSRSampleKitRequest bspRequest = new GSSRSampleKitRequest();
        bspRequest.setGssrWorkGroup(SpfWorkGroupEnum.fromValue("Library Construction"));
        bspRequest.setPlatingRequestID("WR-1234");
        SampleKitBroadPI pi = new SampleKitBroadPI();
        pi.setFirstName("test");
        pi.setLastName("test");
        SampleKitRequestor requestor = new SampleKitRequestor();
        requestor.setFirstName("bsp");
        requestor.setLastName("bsp");
        SampleKitCollaboratorWithOrg collaborator = new SampleKitCollaboratorWithOrg();
        collaborator.setFirstName("John");
        collaborator.setLastName("Aquadro");
        collaborator.setOrganization("Broad Institute");
        bspRequest.setBroadPI(pi);
        bspRequest.setRequestor(requestor);
        bspRequest.setCollaborator(collaborator);
        bspRequest.setShipFrom(SpfShipFromEnum.BSP);
        bspRequest.setShippingMethod(SpfShippingMethodEnum.HAND_DELIVERED);

        RequestSampleSet requestSet = new RequestSampleSet();
        bspRequest.getRequestSampleSet().add(requestSet);
        //add some GSSR Samples
        Iterator<String> stockItr = stockAliquotMap.keySet().iterator();
        String stock = null;
        String aliquotLSID = null;
        while (stockItr.hasNext()) {
            stock = stockItr.next();
            aliquotLSID = stockAliquotMap.get(stock);
            GSSRSample bspAliquot = new GSSRSample();
            bspAliquot.setGssrBarcode(aliquotLSID);
            bspAliquot.setSampleSource(stock); //BSP is not yet setting this value !! need to lookup using SampleSearch

            requestSet.getGssrSample().add(bspAliquot);
        }

        return bspRequest;
    }

    private Map<String, StartingSample> buildAliquotSourceMap(GSSRSampleKitRequest request, ProjectPlan projectPlan) {

        Map<String, StartingSample> aliquotSourceMap = new HashMap<String, StartingSample>();
        List<GSSRSample> sampleList = request.getRequestSampleSet().iterator().next().getGssrSample();
        //assumes BSP request contains sampleSource which is the stock
        for (GSSRSample sample : sampleList) {
            if (sample.getGssrBarcode() != null && !sample.getGssrBarcode().isEmpty() &&
                    sample.getSampleSource() != null && !sample.getSampleSource().isEmpty()) {
                aliquotSourceMap.put(sample.getGssrBarcode(), new BSPStartingSample(sample.getSampleSource(), projectPlan, null));
            }
        }

        return aliquotSourceMap;
    }


}
