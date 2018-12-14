package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for Shearing events
 */
public class ExomeExpressShearingEntityBuilder {
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private       TubeFormation                 preflightRack;
    private final BettaLimsMessageTestFactory   bettaLimsMessageTestFactory;
    private final LabEventFactory               labEventFactory;
    private final LabEventHandler               labEventHandler;
    private final String rackBarcode;
    private       StaticPlate                   shearingPlate;
    private String shearCleanPlateBarcode;
    private StaticPlate shearingCleanupPlate;
    private String testPrefix;

    public ExomeExpressShearingEntityBuilder(Map<String, BarcodedTube> mapBarcodeToTube,
                                             TubeFormation preflightRack,
                                             BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                             LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                             String rackBarcode, String testPrefix) {
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.preflightRack = preflightRack;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.rackBarcode = rackBarcode;
        this.testPrefix = testPrefix;
    }

    public StaticPlate getShearingPlate() {
        return shearingPlate;
    }

    public String getShearCleanPlateBarcode() {
        return shearCleanPlateBarcode;
    }

    public StaticPlate getShearingCleanupPlate() {
        return shearingCleanupPlate;
    }

    public ExomeExpressShearingEntityBuilder invoke() {
        ExomeExpressShearingJaxbBuilder exomeExpressShearingJaxbBuilder = new ExomeExpressShearingJaxbBuilder(
                bettaLimsMessageTestFactory, new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, rackBarcode)
                .invoke();

        shearCleanPlateBarcode = exomeExpressShearingJaxbBuilder.getShearCleanPlateBarcode();

//            validateWorkflow(LabEventType.SHEARING_BUCKET.getName(), mapBarcodeToTube.values());
//            LabEvent shearingBucketEntity =
//                    labEventFactory.buildFromBettaLimsRackEventDbFree(
//                            exomeExpressShearingJaxbBuilder.getExExShearingBucket(), null, mapBarcodeToTube, null);
//            labEventHandler.processEvent(shearingBucketEntity);

        LabEventTest.validateWorkflow("ShearingTransfer", mapBarcodeToTube.values());
        Map<String, LabVessel> mapBarcodeToVessel = new LinkedHashMap<>();
        if (preflightRack != null) {
            mapBarcodeToVessel.put(preflightRack.getLabel(), preflightRack);
        }
        for (BarcodedTube barcodedTube : mapBarcodeToTube.values()) {
            // The calculation of singleInferredBucketedBatch for routing purposes should not carry over when sample
            // instances have multiple bucket entries.  If it is permitted, it may cause a rework tube just coming
            // out of a bucket to keep its old LCSET (GPLIM-3313).
            barcodedTube.clearCaches();
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLims(
                exomeExpressShearingJaxbBuilder.getShearTransferEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(shearingTransferEventEntity);
        // asserts
        shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
        int totalSampleInstanceCount = 0;
        int tubeSampleInstanceCount = 0;
        for (LabVessel labVessel : mapBarcodeToTube.values()) {
            totalSampleInstanceCount += labVessel.getSampleInstanceCount();
            if (tubeSampleInstanceCount == 0) {
                tubeSampleInstanceCount = labVessel.getSampleInstanceCount();
            }
        }
        Assert.assertEquals(shearingPlate.getSampleInstancesV2().size(), totalSampleInstanceCount,
                "Wrong number of sample instances");

        // Covaris Load
//            validateWorkflow("CovarisLoaded", shearingPlate);
        LabEventTest.validateWorkflow("CovarisLoaded", mapBarcodeToTube.values());
        LabEvent covarisLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                            exomeExpressShearingJaxbBuilder.getCovarisLoadEventJaxb(), shearingPlate);
        labEventHandler.processEvent(covarisLoadedEntity);
        // asserts

        Set<SampleInstanceV2> sampleInstancesInWell =
                shearingPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInWell.size(), tubeSampleInstanceCount,
                "Wrong number of sample instances in well");

        // PostShearingTransferCleanup
        LabEventTest.validateWorkflow("PostShearingTransferCleanup", shearingPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(shearingPlate.getLabel(), shearingPlate);
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLims(
                exomeExpressShearingJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(postShearingTransferCleanupEntity);
        // asserts
        shearingCleanupPlate =
                (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingCleanupPlate.getSampleInstancesV2().size(), totalSampleInstanceCount,
                "Wrong number of sample instances");
        Set<SampleInstanceV2> sampleInstancesInWell2 =
                shearingCleanupPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInWell2.size(), tubeSampleInstanceCount,
                "Wrong number of sample instances in well");

        // ShearingQC
        LabEventTest.validateWorkflow("ShearingQC", shearingCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(shearingCleanupPlate.getLabel(), shearingCleanupPlate);
        LabEvent shearingQcEntity = labEventFactory.buildFromBettaLims(
                exomeExpressShearingJaxbBuilder.getShearingQcEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(shearingQcEntity);
        return this;
    }

}
