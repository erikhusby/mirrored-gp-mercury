package org.broadinstitute.sequel.test;

import org.broadinstitute.sequel.boundary.AbstractPass;
import org.broadinstitute.sequel.boundary.BaitSet;
import org.broadinstitute.sequel.boundary.BaitSetListResult;
import org.broadinstitute.sequel.boundary.DirectedPass;
import org.broadinstitute.sequel.boundary.GSSRSampleKitRequest;
import org.broadinstitute.sequel.boundary.designation.LibraryRegistrationSOAPService;
import org.broadinstitute.sequel.boundary.designation.RegistrationJaxbConverter;
import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.bsp.EverythingYouAskForYouGetAndItsHuman;
import org.broadinstitute.sequel.boundary.pass.PassTestDataProducer;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.PassBackedProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.sequel.infrastructure.quote.MockQuoteService;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.broadinstitute.sequel.test.entity.bsp.BSPSampleExportTest;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.*;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * A container free test of Exome Express
 */
public class ExomeExpressEndToEndTest {


    // if this test was running in a container the test data might be injected, though that would really only work well
    // in the one test per class scenario, or at most one test per PASS type per class...

    @Inject
    LibraryRegistrationSOAPService registrationSOAPService;

    /*
        Temporarily adding from ProjectPlanFromPassTest to move test case content along.
     */
    private final long BAIT_ID = 5;
    private final String BAIT_DESIGN_NAME = "interesting genes";

    // @Inject
    // @TestData
    // private DirectedPass directedPass;

    // Assuming the jndi-config branch were to be merged for a container version of this test:
    //
    // @Inject
    // PassService passService;

    // for non-container test:
    //
    // PassService passService = new PassServiceStub();



    @Test(groups = {DATABASE_FREE}, enabled = false)
    public void testAll() throws Exception {

        DirectedPass directedPass = PassTestDataProducer.instance().produceDirectedPass();

        // unconditionally forward all PASSes to Squid for storage
        // passService.storePass(directedPass);

        // if this is an EE pass take it through the SequeL process:
        if (directedPass.isExomeExpress()) {
            // PASS with quote IDs, price items (need PMBridge 2 for price items)



            // factory or something to convert from JAX-WS DTOs to entities (or refer to Squid PASS)
            /*
                Temporarily instantiating an empty pass to set up call to create passBackedProjectPlan.  Fill in with
                the factory call mentioned.
             */
            AbstractPass testPass = new AbstractPass();


            // Check volume and concentration?  Or expose web services to allow PMBridge to check
            // labBatch
            // Project

            //TODO SGM: change this to PassBackedProjectPlan
            BasicProject project = new BasicProject("ExomeExpressProject1", new JiraTicket());
            String runName = "theRun";

            //SGM:  This "Lane Number" is most likely not needed.  Retrieving Number of lanes from the Project Plan Details
//            String laneNumber = "3";

            // BasicProjectPlan
            HashMap<LabEventName, PriceItem> billableEvents = new HashMap<LabEventName, PriceItem>();

//            BasicProjectPlan projectPlan = new BasicProjectPlan(
//                    project,
//                    "ExomeExpressPlan1",
//                    new WorkflowDescription("HybridSelection", billableEvents, CreateIssueRequest.Fields.Issuetype.Whole_Exome_HybSel));

            /*
                Temporarily adding from ProjectPlanFromPassTest to move test case content along.
             */

            BSPSampleDataFetcher bspDataFetcher = new BSPSampleDataFetcher(new EverythingYouAskForYouGetAndItsHuman());
            BaitSetListResult baitsCache = new BaitSetListResult();
            BaitSet baitSet = new BaitSet();
            baitSet.setDesignName(BAIT_DESIGN_NAME);
            baitSet.setId(BAIT_ID);
            baitsCache.getBaitSetList().add(baitSet);

            PassBackedProjectPlan projectPlan = new PassBackedProjectPlan(testPass,bspDataFetcher,new MockQuoteService(),baitsCache);



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
            try {
                bspExportEntityBuilder.runTest();
            } catch (Exception e) {
                Assert.fail("Failed in BSP export test " + e.getMessage());
            }
            //bspPlatingReceipt.getPlatingRequests().iterator().next().
            Collection<Starter> starters = projectPlan.getStarters();
            Map<String, LabVessel> stockSampleAliquotMap = new HashMap<String, LabVessel>();
            for (Starter starter : starters) {
                LabVessel aliquot = projectPlan.getAliquot(starter);
                Assert.assertNotNull(aliquot);
                stockSampleAliquotMap.put(starter.getLabel(), aliquot);
            }

            // factory to convert to entities
            // Receive plastic through kiosk
            // web service callable from Squid kiosk
            // Hybrid Selection Messaging (both systems?)  UPDATE:  SGM: plan to do Hybrid Selection Messaging to just
            //SequeL for Exome Express

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

            LabEventTest.QtpEntityBuilder capturedBuilder = new LabEventTest.QtpEntityBuilder(projectPlan.getWorkflowDescription(), bettaLimsMessageFactory, labEventFactory, labEventHandler,
                    hybridSelectionEntityBuilder.getNormCatchRack(), hybridSelectionEntityBuilder.getNormCatchRackBarcode(),
                    hybridSelectionEntityBuilder.getNormCatchBarcodes(), hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes());

            capturedBuilder.invoke();

            RackOfTubes poolingResult = capturedBuilder.getDenatureRack();

            // LC metrics - upload page?
            // LabVessel.addMetric?
            // Post "work done" to Quote Server
            // MockQuoteService.registerNewWork


            final TwoDBarcodedTube currEntry = poolingResult.getVesselContainer().getVesselAtPosition(VesselPosition.A01);

            final SequelLibrary registerLibrary = RegistrationJaxbConverter.squidify(currEntry);


            registrationSOAPService.registerSequeLLibrary(registerLibrary);

            registrationSOAPService.registerForDesignation(registerLibrary.getLibraryName(), projectPlan, true);



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
}
