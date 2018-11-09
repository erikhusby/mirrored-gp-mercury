package org.broadinstitute.gpinformatics.mercury.control.labevent;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Test(groups = {TestGroups.DATABASE_FREE})
public class DenatureToDilutionHandlerTest extends BaseEventTest {


    private LabVessel denatureSource;
    private Date runDate;
    private QtpEntityBuilder qtpEntityBuilder;

    @Override
    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        super.setUp();
        expectedRouting = SystemRouter.System.MERCURY;

        final ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                                              new HashSet<>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                                                                                  String.valueOf(runDate.getTime()),
                                                                                  "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                                               picoPlatingEntityBuilder.getNormTubeFormation(),
                                               picoPlatingEntityBuilder.getNormalizationBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                                              exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                                              exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                                          libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                                          libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                                         hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                                         hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "1");
        denatureSource = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);


    }

    @AfterMethod(groups = {TestGroups.DATABASE_FREE})
    public void tearDown() throws Exception {

        runDate = null;
        qtpEntityBuilder = null;
        denatureSource = null;
    }


    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testSuccessfulDenatureTransfer() throws Exception {

        final String fctBatchName = "FCT-Test1";
        LabBatch fctBatch = new LabBatch(fctBatchName, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureSource, BigDecimal.TEN);

        Dilution dilution = new Dilution(fctBatchName).invoke();
        LabEvent dilutionTransferEntity = dilution.getDilutionTransferEntity();
        PlateCherryPickEvent dilutionEvent = dilution.getDilutionEvent();

        getLabEventFactory().getEventHandlerSelector().applyEventSpecificHandling(dilutionTransferEntity,
                                                                                  dilutionEvent);
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testDifferentFctTickets() throws Exception {
        final String fctBatchName = "FCT-Test2";
        LabBatch fctBatch = new LabBatch(fctBatchName, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureSource, BigDecimal.TEN);

        Dilution dilution = new Dilution(fctBatchName+"bad").invoke();
        LabEvent dilutionTransferEntity = dilution.getDilutionTransferEntity();
        PlateCherryPickEvent dilutionEvent = dilution.getDilutionEvent();

        try {
            getLabEventFactory().getEventHandlerSelector().applyEventSpecificHandling(dilutionTransferEntity,
                                                                                      dilutionEvent);
            Assert.fail("Different FCT tickets should have caused a failure");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testFctTicketWithDIfferentDilutionTube() throws Exception {
        final String fctBatchName = "FCT-Test3";
        LabBatch fctBatch = new LabBatch(fctBatchName, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureSource, BigDecimal.TEN);

        for (LabBatchStartingVessel startingVessel : fctBatch.getLabBatchStartingVessels()) {
            startingVessel.setDilutionVessel(new BarcodedTube("PreviousDilutionTube" + runDate.getTime()));
        }

        Dilution dilution = new Dilution(fctBatchName).invoke();
        LabEvent dilutionTransferEntity = dilution.getDilutionTransferEntity();
        PlateCherryPickEvent dilutionEvent = dilution.getDilutionEvent();

        try {
            getLabEventFactory().getEventHandlerSelector().applyEventSpecificHandling(dilutionTransferEntity,
                                                                                      dilutionEvent);
            Assert.fail("FCT ticket having a different dilution tube should have caused a failure");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testDilutionInDifferentFctTicket() throws Exception {
        final String fctBatchName = "FCT-Test4";
        LabBatch fctBatch = new LabBatch(fctBatchName, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureSource, BigDecimal.TEN);

        final String altFctTicketName = fctBatchName + "ALT";

        Dilution dilution = new Dilution(fctBatchName).invoke();
        LabEvent dilutionTransferEntity = dilution.getDilutionTransferEntity();
        PlateCherryPickEvent dilutionEvent = dilution.getDilutionEvent();

        LabBatch fctBatch2 = new LabBatch(altFctTicketName, LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell,
                new BarcodedTube(denatureSource.getLabel() + "ALT"), BigDecimal.TEN);

        fctBatch2.getLabBatchStartingVessels().iterator().next().setDilutionVessel(
                dilutionTransferEntity.getTargetVesselTubes().iterator().next());

        try {
            getLabEventFactory().getEventHandlerSelector().applyEventSpecificHandling(dilutionTransferEntity,
                                                                                      dilutionEvent);
            Assert.fail("Dilution registered to a Different FCT ticket should have caused a failure");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class Dilution {
        private String fctBatchName;
        private PlateCherryPickEvent dilutionEvent;
        private LabEvent dilutionTransferEntity;

        public Dilution(String fctBatchName) {
            this.fctBatchName = fctBatchName;
        }

        public PlateCherryPickEvent getDilutionEvent() {
            return dilutionEvent;
        }

        public LabEvent getDilutionTransferEntity() {
            return dilutionTransferEntity;
        }

        public Dilution invoke() {
            HiSeq2500JaxbBuilder dilutionBuilder = new HiSeq2500JaxbBuilder(getBettaLimsMessageTestFactory(),
                    "dilutionTest" + runDate.getTime(), Collections.singletonList( denatureSource.getLabel()),
                    qtpEntityBuilder.getDenatureRack().getRacksOfTubes().iterator().next().getLabel(), fctBatchName,
                    ProductionFlowcellPath.DILUTION_TO_FLOWCELL, denatureSource.getSampleInstanceCount(), null,
                    2).invoke();
            dilutionEvent = dilutionBuilder.getDilutionJaxb();
            dilutionTransferEntity = getLabEventFactory().buildFromBettaLims(dilutionEvent,
                    new HashMap<String, LabVessel>() {{
                        BarcodedTube denatureTube = qtpEntityBuilder.getDenatureRack().getContainerRole()
                                .getVesselAtPosition(VesselPosition.A01);
                        put(denatureTube.getLabel(), denatureTube);
                        put(qtpEntityBuilder.getDenatureRack().getLabel(), qtpEntityBuilder.getDenatureRack());
                    }});
            return this;
        }
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testNovaSeqMultipleFcts() throws Exception {
        final int numberFcts = 3;
        final int numberLanesPerSample = 2;

        // This test verifies that the handler can find the starting tube for a NovaSeq dilution tube
        // when the FCT was booked using the pool tube, so that there are three transfers to traverse
        // from source to dilution instead of two.
        Assert.assertEquals(qtpEntityBuilder.getPoolTubes().size(), 1);
        BarcodedTube poolSource = qtpEntityBuilder.getPoolTubes().iterator().next();
        Set<LabBatchStartingVessel> startingVessels = new HashSet<>();
        Set<String> dilutionTubeBarcodes = new HashSet<>();

        // To simulate the actual nova seq protocol, multiple flowcells are made.
        // Each FCT booking uses the same starting tube and draws sample from the same denature tube.
        for (int i = 0; i < numberFcts; ++i) {
            String fctBatchName = "FCT-Test5" + i;
            LabBatch fctBatch = new LabBatch(fctBatchName, LabBatch.LabBatchType.FCT,
                    IlluminaFlowcell.FlowcellType.NovaSeqFlowcell, poolSource, BigDecimal.TEN);
            startingVessels.addAll(fctBatch.getLabBatchStartingVessels());

            // Setting DILUTION_TO_FLOWCELL causes the builder to put the FCT metadata tag on the lab event.
            HiSeq2500JaxbBuilder builder = new HiSeq2500JaxbBuilder(getBettaLimsMessageTestFactory(),
                    i + "_" + runDate.getTime(), Collections.singletonList(denatureSource.getLabel()),
                    qtpEntityBuilder.getDenatureRack().getRacksOfTubes().iterator().next().getLabel(),
                    fctBatch.getBatchName(), ProductionFlowcellPath.DILUTION_TO_FLOWCELL,
                    denatureSource.getSampleInstanceCount(), null, numberLanesPerSample).invoke();
            Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
            mapBarcodeToVessel.put(denatureSource.getLabel(), denatureSource);
            mapBarcodeToVessel.put(qtpEntityBuilder.getDenatureRack().getLabel(), qtpEntityBuilder.getDenatureRack());
            LabEvent event = getLabEventFactory().buildFromBettaLims(builder.getDilutionJaxb(), mapBarcodeToVessel);
            LabVessel dilutionTube = event.getTargetVesselTubes().iterator().next();
            dilutionTubeBarcodes.add(dilutionTube.getLabel());
            fctBatch.getLabBatchStartingVessels().iterator().next().setDilutionVessel(dilutionTube);
            getLabEventFactory().getEventHandlerSelector()
                    .applyEventSpecificHandling(event, builder.getDilutionJaxb());
        }
        Assert.assertEquals(dilutionTubeBarcodes.size(), numberFcts);
        Assert.assertEquals(startingVessels.size(), numberFcts * numberLanesPerSample);
        Assert.assertTrue(startingVessels.stream().
                allMatch(startingVessel -> startingVessel.getLabVessel().getLabel().equals(poolSource.getLabel())));
    }
}
