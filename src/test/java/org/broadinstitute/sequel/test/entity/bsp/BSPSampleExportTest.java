package org.broadinstitute.sequel.test.entity.bsp;

import org.broadinstitute.sequel.boundary.*;
import org.broadinstitute.sequel.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.queue.AliquotParameters;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

public class BSPSampleExportTest {

    public static final String masterSample1 = "SM-1111";
    public static final String masterSample2 = "SM-2222";

    //private String aliquot1LSID = "broadinstitute.org:bsp.prod.sample:Test Aliquot 1111";
    //private String aliquot2LSID = "broadinstitute.org:bsp.prod.sample:Test Aliquot 2222";

    @Test(groups = {DATABASE_FREE})
    public void testExport() throws Exception {

        //StartingSamples
        List<String> startingStockSamples = new ArrayList<String>();
        startingStockSamples.add(masterSample1);
        startingStockSamples.add(masterSample2);

        BasicProject project = new BasicProject("BSPExportTestingProject", new JiraTicket());
        // BasicProjectPlan
        HashMap<LabEventName, org.broadinstitute.sequel.infrastructure.quote.PriceItem> billableEvents = new HashMap<LabEventName, org.broadinstitute.sequel.infrastructure.quote.PriceItem>();
        BasicProjectPlan projectPlan = new BasicProjectPlan(
                project,
                "ExomeExpressPlan1",
                new WorkflowDescription("HybridSelection", billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

        BSPPlatingExportEntityBuilder bspExportEntityBuilder = new BSPPlatingExportEntityBuilder(projectPlan, startingStockSamples);
        bspExportEntityBuilder.runTest();

    }


    public static class BSPPlatingExportEntityBuilder {

        private static final String ALIQUOT_LSID_PATTERN = "broadinstitute.org:bsp.prod.sample:Test Aliquot ";
        private ProjectPlan projectPlan = null;
        List<String> startingStockSamples = null;
        //List<LabVessel> bspAliquots = null;

        public BSPPlatingExportEntityBuilder(ProjectPlan projectPlan, List<String> startingSamples) {
            if (projectPlan == null) {
                throw new IllegalArgumentException("Invalid Project Plan");
            }
            if (startingSamples == null || startingSamples.isEmpty()) {
                throw new IllegalArgumentException("Invalid Starting Samples");
            }
            this.projectPlan = projectPlan;
            this.startingStockSamples = startingSamples;
        }

        public void runTest() throws Exception {
            BSPPlatingReceipt bspReceipt = new BSPPlatingReceipt("WR-1234");
            if (projectPlan == null) {
                throw new IllegalArgumentException("Invalid Project plan ");
            }

            if (startingStockSamples == null || startingStockSamples.isEmpty()) {
                throw new IllegalArgumentException("Invalid Starting stocks and Aliquots ");
            }

            //Iterator<String> stockItr = stocksAndAliquots.keySet().iterator();
            Iterator<String> stockItr = startingStockSamples.iterator();
            String startingStock = null;
            while (stockItr.hasNext()) {
                startingStock = stockItr.next();
                StartingSample startingSample = new BSPStartingSample(startingStock, projectPlan);
                projectPlan.getStarters().add(startingSample);

                //TODO .. BSPAliquotWorkQueue & SampleSheet to be deprecated/deleted
                // request an aliquot from bsp
                AliquotParameters aliquotParameters = new AliquotParameters(projectPlan, 0.9f, 0.6f);

                //generate BSPPlatingRequests
                BSPPlatingRequest bspPlatingRequest = new BSPPlatingRequest(startingStock, aliquotParameters);
                bspPlatingRequest.setReceipt(bspReceipt);
                projectPlan.getPendingPlatingRequests().add(bspPlatingRequest);
            }

            Collection<Starter> starters = projectPlan.getStarters();
            Assert.assertNotNull(starters);
            Assert.assertEquals(2, starters.size());
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
            Assert.assertEquals(bspAliquots.size(), 2);
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
