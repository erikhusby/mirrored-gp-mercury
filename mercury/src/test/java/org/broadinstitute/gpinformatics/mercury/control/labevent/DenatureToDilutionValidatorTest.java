package org.broadinstitute.gpinformatics.mercury.control.labevent;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.AbstractEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500JaxbBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class DenatureToDilutionValidatorTest extends BaseEventTest {


    private LabVessel denatureSource;
    private Date runDate;
    private QtpEntityBuilder qtpEntityBuilder;

    @Override
    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        super.setUp();

        final ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Long pdoId = 9202938094820L;
        AthenaClientServiceStub.addProductOrder(productOrder);
        runDate = new Date();
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflowName("Exome Express");

        //Build Event History
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube, productOrder,
                workflowBatch, null, String.valueOf(runDate.getTime()), "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(productOrder, picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
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
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(), "Exome Express", "1");
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
        LabBatch fctBatch =
                new LabBatch(fctBatchName, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);

        HiSeq2500JaxbBuilder dilutionBuilder =
                new HiSeq2500JaxbBuilder(getBettaLimsMessageTestFactory(), "dilutionTest" + runDate.getTime(),
                        denatureSource.getLabel(), qtpEntityBuilder.getDenatureRack().getLabel(), fctBatchName)
                        .invoke();
        PlateCherryPickEvent dilutionEvent = dilutionBuilder.getDilutionJaxb();
        LabEvent dilutionTransferEntity =
                getLabEventFactory().buildFromBettaLims(dilutionEvent, new HashMap<String, LabVessel>() {{
                    put(qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01)
                            .getLabel(),
                            qtpEntityBuilder.getDenatureRack().getContainerRole()
                                    .getVesselAtPosition(VesselPosition.A01));
                    put(qtpEntityBuilder.getDenatureRack().getLabel(), qtpEntityBuilder.getDenatureRack());
                }});

        AbstractEventHandler.applyEventSpecificHandling(dilutionTransferEntity, dilutionEvent);
    }

    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testDifferentFctTickets() throws Exception {
        final String fctBatchName = "FCT-Test1";
        LabBatch fctBatch =
                new LabBatch(fctBatchName, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);

        HiSeq2500JaxbBuilder dilutionBuilder =
                new HiSeq2500JaxbBuilder(getBettaLimsMessageTestFactory(), "dilutionTest" + runDate.getTime(),
                        denatureSource.getLabel(), qtpEntityBuilder.getDenatureRack().getLabel(), fctBatchName+"bad")
                        .invoke();
        PlateCherryPickEvent dilutionEvent = dilutionBuilder.getDilutionJaxb();
        LabEvent dilutionTransferEntity =
                getLabEventFactory().buildFromBettaLims(dilutionEvent, new HashMap<String, LabVessel>() {{
                    put(qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01)
                            .getLabel(),
                            qtpEntityBuilder.getDenatureRack().getContainerRole()
                                    .getVesselAtPosition(VesselPosition.A01));
                    put(qtpEntityBuilder.getDenatureRack().getLabel(), qtpEntityBuilder.getDenatureRack());
                }});

        try {
            AbstractEventHandler.applyEventSpecificHandling(dilutionTransferEntity, dilutionEvent);
            Assert.fail("Different FCT tickets should have caused a failure");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @Test(groups = {TestGroups.DATABASE_FREE})
    public void testFctTicketWithDIfferentDilutionTube() throws Exception {
        final String fctBatchName = "FCT-Test1";
        LabBatch fctBatch =
                new LabBatch(fctBatchName, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);

        for(LabBatchStartingVessel startingVessel: fctBatch.getLabBatchStartingVessels()){
            startingVessel.setDilutionVessel(new TwoDBarcodedTube("PreviousDilutionTube"+runDate.getTime()));
        }

        HiSeq2500JaxbBuilder dilutionBuilder =
                new HiSeq2500JaxbBuilder(getBettaLimsMessageTestFactory(), "dilutionTest" + runDate.getTime(),
                        denatureSource.getLabel(), qtpEntityBuilder.getDenatureRack().getLabel(), fctBatchName)
                        .invoke();
        PlateCherryPickEvent dilutionEvent = dilutionBuilder.getDilutionJaxb();
        LabEvent dilutionTransferEntity =
                getLabEventFactory().buildFromBettaLims(dilutionEvent, new HashMap<String, LabVessel>() {{
                    put(qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01)
                            .getLabel(),
                            qtpEntityBuilder.getDenatureRack().getContainerRole()
                                    .getVesselAtPosition(VesselPosition.A01));
                    put(qtpEntityBuilder.getDenatureRack().getLabel(), qtpEntityBuilder.getDenatureRack());
                }});

        try {
            AbstractEventHandler.applyEventSpecificHandling(dilutionTransferEntity, dilutionEvent);
            Assert.fail("FCT ticket having a different dilution tube should have caused a failure");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
