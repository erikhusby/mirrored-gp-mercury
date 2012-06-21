package org.broadinstitute.sequel.test;

import org.broadinstitute.sequel.boundary.DirectedPass;
import org.broadinstitute.sequel.boundary.GSSRSampleKitRequest;
import org.broadinstitute.sequel.boundary.designation.LibraryRegistrationSOAPService;
import org.broadinstitute.sequel.boundary.squid.IndexPosition;
import org.broadinstitute.sequel.boundary.squid.LibraryMolecularIndex;
import org.broadinstitute.sequel.boundary.squid.RegistrationSample;
import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.boundary.squid.SequencingTechnology;
import org.broadinstitute.sequel.control.labevent.LabEventFactory;
import org.broadinstitute.sequel.control.labevent.LabEventHandler;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.reagent.MolecularIndex;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.sequel.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularState;
import org.broadinstitute.sequel.entity.vessel.RackOfTubes;
import org.broadinstitute.sequel.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.sequel.entity.vessel.VesselPosition;
import org.broadinstitute.sequel.infrastructure.jira.issue.CreateIssueRequest;
import org.broadinstitute.sequel.infrastructure.quote.PriceItem;
import org.seleniumhq.jetty7.util.statistic.SampleStatistic;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.sequel.TestGroups.DATABASE_FREE;

/**
 * A container free test of Exome Express
 */
public class ExomeExpressEndToEndTest {


    @Inject
    private DirectedPass directedPass;

    @Inject
    LibraryRegistrationSOAPService registrationSOAPService;

    // Assuming the jndi-config branch were to be merged:
    //
    // @Inject
    // PassService passService;

    @Test(groups = {DATABASE_FREE}, enabled = false)
    public void testAll() throws Exception {

        // unconditionally forward all PASSes to Squid for storage
        // passService.storePass(directedPass);

        // if this is an EE pass take it through the SequeL process:
        if (directedPass.isExomeExpress()) {
            // PASS with quote IDs, price items (need PMBridge 2 for price items)

            // factory or something to convert from JAX-WS DTOs to entities (or refer to Squid PASS)
            // Check volume and concentration?  Or expose web services to allow PMBridge to check
            // labBatch
            // Project

            //TODO SGM: change this to PassBackedProjectPlan
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


            //TODO SGM:  START Move the following code to a conversion method (include TwoDBarcodedTube and PassBackedProjectPlan
            final SequelLibrary registerLibrary = new SequelLibrary();
            registerLibrary.setLibraryName(currEntry.getLabCentricName());

            List<MolecularState.STRANDEDNESS> strandednessesState = new ArrayList<MolecularState.STRANDEDNESS>();

            for(SampleInstance currSample:currEntry.getSampleInstances()) {
                final RegistrationSample sampleInstance = new RegistrationSample();
                sampleInstance.setBspContextReference(currSample.getStartingSample().getSampleName());
                sampleInstance.setTechnology(SequencingTechnology.ILLUMINA);

                for(Reagent sampleReagent:currSample.getReagents()) {
                    if(sampleReagent instanceof MolecularIndexReagent) {
                        for(Map.Entry<MolecularIndexingScheme.PositionHint,MolecularIndex> currScheme:((MolecularIndexReagent) sampleReagent).getMolecularIndexingScheme().getIndexes().entrySet()) {
                            LibraryMolecularIndex newIndex = new LibraryMolecularIndex();
                            newIndex.setMolecularBarcode(currScheme.getValue().getSequence());
                            if(currScheme.getKey() instanceof MolecularIndexingScheme.IlluminaPositionHint) {
                                switch ((MolecularIndexingScheme.IlluminaPositionHint) currScheme.getKey()) {
                                    case P5:
                                        newIndex.setPositionHint(IndexPosition.ILLUMINA_P_5);
                                        break;
                                    case P7:
                                        newIndex.setPositionHint(IndexPosition.ILLUMINA_P_7);
                                        break;
                                    case IS1:
                                        newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_1);
                                        break;
                                    case IS2:
                                        newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_2);
                                        break;
                                    case IS3:
                                        newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_3);
                                        break;
                                    case IS4:
                                        newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_4);
                                        break;
                                    case IS5:
                                        newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_5);
                                        break;
                                    case IS6:
                                        newIndex.setPositionHint(IndexPosition.ILLUMINA_IS_6);
                                        break;
                                }
                            } else {
                                throw new RuntimeException("Illumina is the only Scheme technology allowed now for Exome");
                            }

                            sampleInstance.getMolecularIndexes().add(newIndex);
                        }
                    }
                }


                strandednessesState.add(currSample.getMolecularState().getStrand());
                Assert.assertFalse(strandednessesState.size() > 1,
                                   "There should not be a mix of single and double stranded samples in this library");

                registerLibrary.getSamples().add(sampleInstance);
            }
            //TODO SGM:  END Move the following code to a conversion method (include TwoDBarcodedTube and PassBackedProjectPlan


            registerLibrary.setSingleStrandInd(MolecularState.STRANDEDNESS.SINGLE_STRANDED.equals(
                    strandednessesState.get(0)));

            registrationSOAPService.registerSequeLLibrary(registerLibrary);

//            registrationSOAPService.registerForDesignation(registerLibrary.getLibraryName(), );



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
