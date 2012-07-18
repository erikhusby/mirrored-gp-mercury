package org.broadinstitute.sequel.test.entity.bsp;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.sequel.entity.billing.Quote;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.queue.AliquotParameters;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.infrastructure.bsp.plating.*;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.Funding;
import org.broadinstitute.sequel.infrastructure.quote.FundingLevel;
import org.broadinstitute.sequel.infrastructure.quote.QuoteFunding;
import org.broadinstitute.sequel.integration.ContainerTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.*;

import static org.broadinstitute.sequel.TestGroups.EXTERNAL_INTEGRATION;

public class BSPPlatingTest extends ContainerTest {

    public static final String masterSample1 = "SM-1111";
    public static final String masterSample2 = "SM-2222";

    @Inject
    private Log log;

    //@Inject
    BSPPlatingRequestService platingService;

    public BSPPlatingTest() {
        log = LogFactory.getLog(BSPPlatingTest.class);
    }

    @Test(groups = {EXTERNAL_INTEGRATION}, enabled = true)
    public void testIssueBSPPlating() throws Exception {

        platingService = new BSPPlatingRequestServiceStub();

        BasicProject project = new BasicProject("BSPPlatingTestingProject", new JiraTicket());
        // BasicProjectPlan
        BasicProjectPlan projectPlan = new BasicProjectPlan(
                project,
                "ExomeExpressPlan1",
                new WorkflowDescription("HybridSelection", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

        String quoteString = "DNA385";
        Quote billingQuote = new Quote(quoteString,
                new org.broadinstitute.sequel.infrastructure.quote.Quote(quoteString, new QuoteFunding(new FundingLevel("100", new Funding(Funding.FUNDS_RESERVATION, "NCI")))));
        projectPlan.setQuote(billingQuote);

        StartingSample startingSample = new BSPStartingSample(masterSample1, projectPlan);
        projectPlan.getStarters().add(startingSample);

        StartingSample startingSample2 = new BSPStartingSample(masterSample2, projectPlan);
        projectPlan.getStarters().add(startingSample2);

        Map<StartingSample, AliquotParameters> starterMap = new HashMap<StartingSample, AliquotParameters>();
        for (Starter starter : projectPlan.getStarters()) {
            starterMap.put((StartingSample) starter, new AliquotParameters(projectPlan, 1.9f, 1.6f));
        }

        BSPSampleFactory bspSampleFactory = new BSPSampleFactory();

        List<BSPPlatingRequest> bspRequests = bspSampleFactory.buildBSPPlatingRequests(starterMap);
        Assert.assertNotNull(bspRequests);
        Assert.assertEquals(starterMap.keySet().size(), bspRequests.size());

        BSPPlatingRequestOptions defaultOptions = platingService.getBSPPlatingRequestDefaultOptions();
        //set PlatformAndProcess, notificationList
        defaultOptions.setNotificationList("sampath@broadinstitute.org");
        //defaultOptions.setNotificationList("projects@broadinstitute.org");
        defaultOptions.setPlatformAndProcess(BSPPlatingRequestOptions.PlatformAndProcess.ILLUMINA_HYBRID_SELECTION_WGS_FRAGMENT_180BP); //TODO.. pass this

        String platingRequestName = "TEST BSP PR-1";
        String label = platingRequestName;
        String comments = "BSP Plating Request test from ExomeExpress";
        List<ControlWell> controlWells = new ArrayList<ControlWell>();
        //TODO .. test add controls
        //controlWells = bspSampleFactory.buildControlWells(null, projectPlan, 1, 0.5F, "DNA3DY", "DNA274");
        String technology = "Solexa";
        BSPPlatingRequestResult platingResult = platingService.issueBSPPlatingRequest(defaultOptions, bspRequests, controlWells, "sampath", platingRequestName, comments, technology, label);
        Assert.assertNotNull(platingResult);
    }

    @Test(groups = {EXTERNAL_INTEGRATION}, enabled = false)
    public void testExport() throws Exception {

        BasicProject project = new BasicProject("BSPExportTestingProject", new JiraTicket());
        // BasicProjectPlan
        BasicProjectPlan projectPlan = new BasicProjectPlan(
                project,
                "ExomeExpressPlan1",
                new WorkflowDescription("HybridSelection", null, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

        String quoteString = "DNA385";
        Quote billingQuote = new org.broadinstitute.sequel.entity.billing.Quote(quoteString,
                new org.broadinstitute.sequel.infrastructure.quote.Quote(quoteString, new QuoteFunding(new FundingLevel("100", new Funding(Funding.FUNDS_RESERVATION, "NCI")))));
        projectPlan.setQuote(billingQuote);

        StartingSample startingSample = new BSPStartingSample(masterSample1, projectPlan);
        projectPlan.getStarters().add(startingSample);

        StartingSample startingSample2 = new BSPStartingSample(masterSample2, projectPlan);
        projectPlan.getStarters().add(startingSample2);

        BSPPlatingReceipt bspReceipt = buildTestReceipt(projectPlan);
        runBSPExportTest(bspReceipt, projectPlan);
    }

    private BSPPlatingReceipt buildTestReceipt(ProjectPlan projectPlan) {
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

        return bspReceipt;
    }

    public static void runBSPExportTest(BSPPlatingReceipt bspReceipt, ProjectPlan projectPlan) throws Exception {
        String ALIQUOT_LSID_PATTERN = "broadinstitute.org:bsp.prod.sample:Test Aliquot ";

        if (bspReceipt == null) {
            throw new IllegalArgumentException("Invalid BSP Plating Receipt ");
        }
        if (projectPlan == null) {
            throw new IllegalArgumentException("Invalid Project Plan ");
        }

        //TODO .. asserts for size of 2 should be removed and check with LabBatch size.
        //LabBatch ?? projectPlan can have many starters .. but batched .. so probably plating should be by batch
        projectPlan.getPendingPlatingRequests().addAll(bspReceipt.getPlatingRequests());
        Collection<Starter> starters = projectPlan.getStarters();
        Assert.assertNotNull(starters);
        Assert.assertEquals(starters.size(), 2, "Project Plan should have 2 starters");
        Assert.assertEquals(starters.size(), bspReceipt.getPlatingRequests().size(), "Project Plan should have same starters as BSP Plating Receipt");
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

            aliquotSourceMap.put(aliquotLSID, bspStock);
        }

        BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
        List<LabVessel> bspAliquots = bspSampleFactory.receiveBSPAliquots(bspReceipt, aliquotSourceMap, null);

        //now iterate through these aliquots, do some asserts and see if we can navigate back to requests.
        Assert.assertNotNull(bspAliquots);
        Assert.assertEquals(bspAliquots.size(), starters.size(), "BSP Aliquot size should match Staters size");
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

