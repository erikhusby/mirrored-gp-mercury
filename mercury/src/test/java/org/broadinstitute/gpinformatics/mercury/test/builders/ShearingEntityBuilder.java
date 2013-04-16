package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for Shearing events
 */
public class ShearingEntityBuilder {
    private final Map<String, TwoDBarcodedTube> mapBarcodeToTube;
    private       TubeFormation                 preflightRack;
    private final BettaLimsMessageTestFactory   bettaLimsMessageTestFactory;
    private final LabEventFactory               labEventFactory;
    private final LabEventHandler               labEventHandler;
    private final String rackBarcode;
    private String shearPlateBarcode;
    private       StaticPlate                   shearingPlate;
    private String shearCleanPlateBarcode;
    private StaticPlate shearingCleanupPlate;
    private String testPrefix;

    public ShearingEntityBuilder(Map<String, TwoDBarcodedTube> mapBarcodeToTube, TubeFormation preflightRack,
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
                new ArrayList<String>(
                        mapBarcodeToTube.keySet()), testPrefix,
                rackBarcode).invoke();
        shearPlateBarcode = shearingJaxbBuilder.getShearPlateBarcode();
        shearCleanPlateBarcode = shearingJaxbBuilder.getShearCleanPlateBarcode();

        // ShearingTransfer
        LabEventTest.validateWorkflow("ShearingTransfer", mapBarcodeToTube.values());
        LabEvent shearingTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(
                shearingJaxbBuilder.getShearingTransferEventJaxb(), preflightRack, null);
        labEventHandler.processEvent(shearingTransferEventEntity);
        // asserts
        shearingPlate = (StaticPlate) shearingTransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                                   "Wrong number of sample instances");

        // PostShearingTransferCleanup
        LabEventTest.validateWorkflow("PostShearingTransferCleanup", shearingPlate);
        LabEvent postShearingTransferCleanupEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                shearingJaxbBuilder.getPostShearingTransferCleanupEventJaxb(), shearingPlate, null);
        labEventHandler.processEvent(postShearingTransferCleanupEntity);
        // asserts
        shearingCleanupPlate =
                (StaticPlate) postShearingTransferCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(shearingCleanupPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");
        Set<SampleInstance> sampleInstancesInWell =
                shearingCleanupPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
        Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
        Assert.assertEquals(sampleInstancesInWell.iterator().next().getStartingSample().getSampleKey(),
                mapBarcodeToTube.values().iterator().next().getSampleInstances().iterator().next()
                        .getStartingSample().getSampleKey(), "Wrong sample");

        // ShearingQC
        LabEventTest.validateWorkflow("ShearingQC", shearingCleanupPlate);
        LabEvent shearingQcEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(
                shearingJaxbBuilder.getShearingQcEventJaxb(), shearingCleanupPlate, null);
        labEventHandler.processEvent(shearingQcEntity);
        return this;
    }

}
