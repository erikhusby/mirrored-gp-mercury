package org.broadinstitute.sequel.test.entity.bsp;

import org.broadinstitute.sequel.boundary.squid.*;
import org.broadinstitute.sequel.boundary.squid.PriceItem;
import org.broadinstitute.sequel.control.dao.bsp.BSPSampleFactory;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.queue.AliquotParameters;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.BSPStockSample;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.infrastructure.jira.DummyJiraService;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.*;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

public class BSPSampleExportTest {

    private String masterSample1 = "SM-1111";
    private String masterSample2 = "SM-2222";

    private String aliquot1LSID = "broadinstitute.org:bsp.prod.sample:Test Aliquot 1";
    private String aliquot2LSID = "broadinstitute.org:bsp.prod.sample:Test Aliquot 2";

    @Test(groups = {DATABASE_FREE})
    public void testExport() throws Exception {

        //need to build a JAXB object
        GSSRSampleKitRequest bspRequest = createKitLevelData();
        bspRequest.setSampleKitMaterialType(SpfMaterialTypeEnum.GENOMIC_DNA);
        RequestSampleSet sampleSet = new RequestSampleSet();

        GSSRSample sample = new GSSRSample();
        sample.setSampleSource(masterSample1);
        SpfSampleOrganismAttributes orgAttributes = new SpfSampleOrganismAttributes();
        orgAttributes.setLSId(aliquot1LSID);
        sample.setSampleOrganismAttributes(orgAttributes);
        sampleSet.getGssrSample().add(sample);

        GSSRSample sample2 = new GSSRSample();
        sample2.setSampleSource(masterSample2);
        SpfSampleOrganismAttributes orgAttributes2 = new SpfSampleOrganismAttributes();
        orgAttributes2.setLSId(aliquot2LSID);
        sample2.setSampleOrganismAttributes(orgAttributes2);
        sampleSet.getGssrSample().add(sample2);

        bspRequest.getRequestSampleSet().add(sampleSet);
        //build a BSPPlatingReceipt;

        //build PassSamples
        //stat from PASS JAXB object
        //DirectedPass directedPass = new DirectedPass();
        Sample passSample1 = new Sample();
        passSample1.setBspSampleID(masterSample1);
        Sample passSample2 = new Sample();
        passSample2.setBspSampleID(masterSample2);
        Map<String, Sample> aliquotPassSourceMap = new HashMap<String, Sample>();
        aliquotPassSourceMap.put(aliquot1LSID, passSample1);
        aliquotPassSourceMap.put(aliquot2LSID, passSample2);

        BSPPlatingReceipt bspPlatingReceipt = buildBSPPlatingReceipt();
        BasicProjectPlan projectPlan  = (BasicProjectPlan)bspPlatingReceipt.getPlatingRequests().iterator().next().getAliquotParameters().getProjectPlan();

        Map<String, BSPStartingSample> aliquotSourceMap = new HashMap<String, BSPStartingSample>();

        //should be two starters in projectPlan
        Collection<Starter> starters = projectPlan.getStarters();
        Assert.assertNotNull(starters);
        Assert.assertEquals(2, starters.size());
        Iterator<Starter> starterIterator = starters.iterator();
        BSPStartingSample startingSample1 = (BSPStartingSample)starterIterator.next();
        BSPStartingSample startingSample2 = (BSPStartingSample)starterIterator.next();

        aliquotSourceMap.put(aliquot1LSID, startingSample1);
        aliquotSourceMap.put(aliquot2LSID, startingSample2);

        BSPSampleFactory bspSampleFactory = new BSPSampleFactory();
        List<Starter> bspAliquots = bspSampleFactory.receiveBSPAliquots(bspPlatingReceipt, aliquotPassSourceMap, aliquotSourceMap, null);

        //now iterate through these aliquots, do some asserts and see if we can navigate back to requests.
        Assert.assertNotNull(bspAliquots);
        Assert.assertEquals(bspAliquots.size(), 2);
        for (Starter aliquot : bspAliquots) {
            Assert.assertTrue(aliquot.getLabel().contains("Aliquot"));
            //check the source stock sample of each aliquot
            BSPSampleAuthorityTwoDTube bspAliquot = (BSPSampleAuthorityTwoDTube)aliquot;
            Project project = bspAliquot.getAllProjects().iterator().next();
            Assert.assertEquals("BSPExportTestingProject", project.getProjectName());
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
        Assert.assertEquals(2, starterStocks.size());
        while (starterStocksIterator.hasNext()) {
            Starter starter = starterStocksIterator.next();
            LabVessel aliquot = projectPlan.getAliquot(starter);
            Assert.assertNotNull(aliquot);
            Assert.assertEquals(true, aliquot.getLabel().contains("Aliquot"));
//            if (masterSample1.equals(starter.getLabel()) ) {
//                Assert.assertEquals(true, projectPlan.getAliquot(starter).getLabel().contains("Aliquot 1"));
//            }
//            if (masterSample2.equals(starter.getLabel()) ) {
//                Assert.assertEquals(true, projectPlan.getAliquot(starter).getLabel().contains("Aliquot 2"));
//            }
        }

    }

    private SampleList buildTestPASSSampleList() {

        //stat from PASS JAXB object
        DirectedPass directedPass = new DirectedPass();
        Sample passSample1 = new Sample();
        passSample1.setBspSampleID("SM-1111");

        Sample passSample2 = new Sample();
        passSample2.setBspSampleID("SM-2222");

        SampleList passSampleList = new SampleList();
        passSampleList.getSample().add(passSample1);
        passSampleList.getSample().add(passSample2);
        //TODO ... add some 188 samples so that we can make 2 LabBatches


        return passSampleList;

    }


    private GSSRSampleKitRequest createKitLevelData() {
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
        return bspRequest;
    }

    private BSPPlatingReceipt buildBSPPlatingReceipt() {
        // Project and workflow
        Map<LabEventName, org.broadinstitute.sequel.infrastructure.quote.PriceItem> billableEvents = new HashMap<LabEventName, org.broadinstitute.sequel.infrastructure.quote.PriceItem>();
        Project project = new BasicProject("BSPExportTestingProject", new JiraTicket(new DummyJiraService(),"TP-0","0"));
        WorkflowDescription workflowDescription = new WorkflowDescription("HS", billableEvents,
                CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel);
        BasicProjectPlan projectPlan = new BasicProjectPlan(project,"To test BSP export", workflowDescription);

        BSPStartingSample startingSample1 = new BSPStartingSample(masterSample1, projectPlan);
        BSPStartingSample startingSample2 = new BSPStartingSample(masterSample2, projectPlan);
        projectPlan.getStarters().add(startingSample1);
        projectPlan.getStarters().add(startingSample2);


        //TODO .. BSPAliquotWorkQueue & SampleSheet to be deprecated/deleted
        // request an aliquot from bsp
        AliquotParameters aliquotParameters1 = new AliquotParameters(projectPlan, 0.9f, 0.6f);
        AliquotParameters aliquotParameters2 = new AliquotParameters(projectPlan, 1.9f, 2.6f);

        //generate BSPPlatingRequests
        BSPPlatingRequest bspPlatingRequest1 = new BSPPlatingRequest(masterSample1, aliquotParameters1);
        BSPPlatingRequest bspPlatingRequest2 = new BSPPlatingRequest(masterSample2, aliquotParameters2);
        projectPlan.getPendingPlatingRequests().add(bspPlatingRequest1);
        projectPlan.getPendingPlatingRequests().add(bspPlatingRequest2);

        //assume a BSPPlatingRequest is issued
        //TODO .. BSPAliquotWorkQueue is deprecated/deleted .. IssueBSPPlatingRequest .. NOT YET implemented ..
        BSPPlatingReceipt bspReceipt = new BSPPlatingReceipt("WR-1234");
        bspPlatingRequest1.setReceipt(bspReceipt);
        bspPlatingRequest2.setReceipt(bspReceipt);

        //BSPPlatingReceipt project1PlatingReceipt = projectPlan.getPendingPlatingRequests().iterator().next().getReceipt();

        return bspReceipt;

    }


}
