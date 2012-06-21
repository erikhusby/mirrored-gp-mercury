package org.broadinstitute.sequel.test;

import junit.framework.Assert;
import org.broadinstitute.sequel.boundary.DirectedPass;
import org.broadinstitute.sequel.boundary.GSSRSampleKitRequest;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.*;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.test.entity.bsp.BSPSampleExportTest;
import org.testng.annotations.Test;

import java.util.*;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * A container free test of Exome Express
 */
public class ExomeExpressEndToEndTest {

    @Test(groups = {DATABASE_FREE}, enabled = false)
    public void testAll() throws Exception {
        // PASS with quote IDs, price items (need PMBridge 2 for price items)
        DirectedPass sourcePass = null;
        // factory or something to convert from JAX-WS DTOs to entities (or refer to Squid PASS)
        // Check volume and concentration?  Or expose web services to allow PMBridge to check
        // labBatch
        // Project
        BasicProject project = new BasicProject("ExomeExpressProject1", new JiraTicket());
        String runName = "theRun";
        String laneNumber = "3";
        // BasicProjectPlan
        HashMap<LabEventName, PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        BasicProjectPlan projectPlan = new BasicProjectPlan(
                project,
                "ExomeExpressPlan1",
                new WorkflowDescription("HybridSelection", billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));
        // Auto-create work request in Squid, for designation?
        // JIRA ticket
        new JiraTicket();
        // Plating request to BSP
        // BSP Client mock to get receipt?
        // Plating export from BSP
        new GSSRSampleKitRequest();
        //Test BSP Plating EXPORT
        //StartingSamples
        List<String> startingStockSamples = new ArrayList<String>();
        startingStockSamples.add(BSPSampleExportTest.masterSample1);
        startingStockSamples.add(BSPSampleExportTest.masterSample2);
        BSPSampleExportTest.BSPPlatingExportEntityBuilder bspExportEntityBuilder = new BSPSampleExportTest.BSPPlatingExportEntityBuilder(projectPlan, startingStockSamples);
        bspExportEntityBuilder.runTest();
        //bspPlatingReceipt.getPlatingRequests().iterator().next().
        Collection<Starter> starters = projectPlan.getStarters();
        Map<String, LabVessel> stockSampleAliquotMap = new HashMap<String, LabVessel>();
        for (Starter starter : starters) {
            LabVessel aliquot = projectPlan.getAliquot(starter);
            Assert.assertNotNull(aliquot);
            stockSampleAliquotMap.put(starter.getLabel(), aliquot);
        }

        //pass this stockSampleAliquotMap to LabEvent !!

        // factory to convert to entities
        // Receive plastic through kiosk
        // web service callable from Squid kiosk
        // Hybrid Selection Messaging (both systems?)
        // deck query for barcodes
        // (deck query for workflow)
        // deck sends message, check workflow
        LabEventFactory labEventFactory = new LabEventFactory();
        LabEventHandler labEventHandler = new LabEventHandler();
        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new HashMap<String, TwoDBarcodedTube>();
        LabEventTest.PreFlightEntityBuilder preFlightEntityBuilder = new LabEventTest.PreFlightEntityBuilder(
                projectPlan.getWorkflowDescription(), bettaLimsMessageFactory, labEventFactory, labEventHandler,
                mapBarcodeToTube);//.invoke();

        LabEventTest.ShearingEntityBuilder shearingEntityBuilder = new LabEventTest.ShearingEntityBuilder(
                projectPlan.getWorkflowDescription(), mapBarcodeToTube, bettaLimsMessageFactory, labEventFactory,
                labEventHandler, preFlightEntityBuilder.getRackBarcode()).invoke();

        LabEventTest.LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = new LabEventTest.LibraryConstructionEntityBuilder(
                projectPlan.getWorkflowDescription(), bettaLimsMessageFactory, labEventFactory, labEventHandler,
                shearingEntityBuilder.getShearingCleanupPlate(), shearingEntityBuilder.getShearCleanPlateBarcode(),
                shearingEntityBuilder.getShearingPlate()).invoke();

        LabEventTest.HybridSelectionEntityBuilder hybridSelectionEntityBuilder = new LabEventTest.HybridSelectionEntityBuilder(
                projectPlan.getWorkflowDescription(), bettaLimsMessageFactory, labEventFactory, labEventHandler,
                libraryConstructionEntityBuilder.getPondRegRack(), libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                libraryConstructionEntityBuilder.getPondRegTubeBarcodes()).invoke();

        new LabEventTest.QtpEntityBuilder(projectPlan.getWorkflowDescription(), bettaLimsMessageFactory, labEventFactory, labEventHandler,
                hybridSelectionEntityBuilder.getNormCatchRack(), hybridSelectionEntityBuilder.getNormCatchRackBarcode(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(), hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes()).invoke();

        // LC metrics - upload page?
        // LabVessel.addMetric?
        // Post "work done" to Quote Server
        // MockQuoteService.registerNewWork
        // Designation in Squid (7 lanes Squid + 1 lane SequeL)
        // Call Squid web service to add to queue (lanes, read length)
        // ZIMS
        /*
        IlluminaRunResource illuminaRunResource = new IlluminaRunResource();

        ZimsIlluminaRun zimsRun = illuminaRunResource.getRun(runName);

        assertNotNull(zimsRun);
        boolean foundLane = false;
        boolean foundSample = false;
        for (ZimsIlluminaChamber zimsLane : zimsRun.getLanes()) {
            if (laneNumber.equals(zimsLane)) {
                foundLane = true;
                Collection<LibraryBean> libraries = zimsLane.getLibraries();
                assertFalse(libraries.isEmpty());
                for (LibraryBean library : libraries) {
                    assertEquals(library.getProject(),sourcePass.getResearchProject());
                    // todo how to get from pass bait set id to bait name?
                    assertEquals(library.getBaitSetName(),sourcePass.getBaitSetID());
                    // todo how to get from pass organism id to organism name?
                    assertEquals(library.getOrganism(), sourcePass.getProjectInformation().getOrganismID());
                    for (Sample sample : sourcePass.getSampleDetailsInformation().getSample()) {
                        // todo probably wrong, not sure whether the sample id is lsid or stock id
                        if (library.getLsid().equals(sample)) {
                            foundSample = true;
                        }
                    }
                    assertTrue(foundSample);
                    // todo single sample ancestor comparison
                }
            }
        }
        assertTrue(foundLane);
        */
    }
}
