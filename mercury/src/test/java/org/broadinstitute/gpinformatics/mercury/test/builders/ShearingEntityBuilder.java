package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for Shearing events
 */
public class ShearingEntityBuilder {
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

    public ShearingEntityBuilder(Map<String, BarcodedTube> mapBarcodeToTube, TubeFormation preflightRack,
                                 BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                 LabEventFactory labEventFactory,
                                 LabEventHandler labEventHandler, String rackBarcode, String testPrefix) {
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

    public ShearingEntityBuilder invoke() {
        ShearingJaxbBuilder shearingJaxbBuilder = new ShearingJaxbBuilder(bettaLimsMessageTestFactory,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, rackBarcode).invoke();
        shearCleanPlateBarcode = shearingJaxbBuilder.getShearCleanPlateBarcode();

        // ShearingTransfer
        LabEventTest.validateWorkflow("ShearingTransfer", mapBarcodeToTube.values());
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(preflightRack.getLabel(), preflightRack);
        for (BarcodedTube barcodedTube : preflightRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLims(
                shearingJaxbBuilder.getShearingTransferEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(shearingTransferEventEntity);
        // asserts
        shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                                   "Wrong number of sample instances");

        // PostShearingTransferCleanup
        LabEventTest.validateWorkflow("PostShearingTransferCleanup", shearingPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(shearingPlate.getLabel(), shearingPlate);
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLims(
                shearingJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(postShearingTransferCleanupEntity);
        // asserts
        shearingCleanupPlate =
                (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingCleanupPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");
        Set<SampleInstanceV2> sampleInstancesInWell =
                shearingCleanupPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
        Assert.assertEquals(sampleInstancesInWell.iterator().next().getRootOrEarliestMercurySampleName(),
                mapBarcodeToTube.values().iterator().next().getSampleInstancesV2().iterator().next()
                        .getRootOrEarliestMercurySampleName(), "Wrong sample");

        // ShearingQC
        LabEventTest.validateWorkflow("ShearingQC", shearingCleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(shearingCleanupPlate.getLabel(), shearingCleanupPlate);
        LabEvent shearingQcEntity = labEventFactory.buildFromBettaLims(shearingJaxbBuilder.getShearingQcEventJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(shearingQcEntity);
        return this;
    }

}
