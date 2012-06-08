package org.broadinstitute.sequel.test;

import org.broadinstitute.sequel.boundary.DirectedPass;
import org.broadinstitute.sequel.boundary.GSSRSampleKitRequest;
import org.broadinstitute.sequel.boundary.zims.IlluminaRunResource;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * A container free test of Exome Express
 */
public class ExomeExpressEndToEndTest {

    @Test(groups = {DATABASE_FREE})
    public void testAll() {
        // PASS with quote IDs, price items (need PMBridge 2 for price items)
        new DirectedPass();
        // factory or something to convert from JAX-WS DTOs to entities (or refer to Squid PASS)
        // Check volume and concentration?  Or expose web services to allow PMBridge to check
        // labBatch
        // Project
        BasicProject project = new BasicProject("ExomeExpressProject1", new JiraTicket());
        // ProjectPlan
        HashMap<LabEventName, PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();
        ProjectPlan projectPlan = new ProjectPlan(
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
        IlluminaRunResource illuminaRunResource;
    }
}
