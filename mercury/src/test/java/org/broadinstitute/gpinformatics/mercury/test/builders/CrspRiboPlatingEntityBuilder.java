package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
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

/**
 * Builds entity graph for Ribo Plating Pico events
 */
public class CrspRiboPlatingEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final String rackBarcode;
    private final String testPrefix;
    private LabEvent initialRiboTransfer1;
    private LabEvent initialRiboTransfer2;
    private LabEvent riboMicrofluorEntityA2;
    private LabEvent riboMicrofluorEntityB1;
    private StaticPlate riboMicrofluorPlate;
    private LabEvent polyAAliquotSpikeEvent;
    private LabEvent polyATSAliquot;
    private Map<String,BarcodedTube> polyAAliquotBarcodedTubeMap;
    private String polyAAliquotRackBarcode;
    private TubeFormation polyAAliquotTubeFormation;

    public CrspRiboPlatingEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                        LabEventFactory labEventFactory,
                                        LabEventHandler labEventHandler, Map<String, BarcodedTube> mapBarcodeToTube,
                                        String rackBarcode, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.rackBarcode = rackBarcode;
        this.testPrefix = testPrefix;
    }

    public LabEvent getPolyATSAliquot() {
        return polyATSAliquot;
    }

    public Map<String, BarcodedTube> getPolyAAliquotBarcodedTubeMap() {
        return polyAAliquotBarcodedTubeMap;
    }

    public String getPolyAAliquotRackBarcode() {
        return polyAAliquotRackBarcode;
    }

    public TubeFormation getPolyAAliquotTubeFormation() {
        return polyAAliquotTubeFormation;
    }

    public LabEvent getInitialRiboTransfer1() {
        return initialRiboTransfer1;
    }

    public LabEvent getInitialRiboTransfer2() {
        return initialRiboTransfer2;
    }

    public CrspRiboPlatingEntityBuilder invoke() {
        CrspRiboPlatingJaxbBuilder crspRiboPlatingJaxbBuilder = new CrspRiboPlatingJaxbBuilder(rackBarcode,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, bettaLimsMessageTestFactory).invoke();

        Map<String, LabVessel> mapBarcodeToVessel = new LinkedHashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);

        //RiboTransfer
        LabEventTest.validateWorkflow("RiboTransfer", mapBarcodeToTube.values());
        initialRiboTransfer1 = labEventFactory.buildFromBettaLims(crspRiboPlatingJaxbBuilder.getInitialRiboTransfer1(),
                mapBarcodeToVessel);
        StaticPlate initialRiboPlate1 = (StaticPlate) initialRiboTransfer1.getTargetLabVessels().iterator().next();
        initialRiboTransfer2 = labEventFactory.buildFromBettaLims(crspRiboPlatingJaxbBuilder.getInitialRiboTransfer2(),
                mapBarcodeToVessel);
        StaticPlate initialRiboPlate2 = (StaticPlate) initialRiboTransfer2.getTargetLabVessels().iterator().next();

        labEventFactory.buildFromBettaLimsPlateEventDbFree(crspRiboPlatingJaxbBuilder.getInitialRiboBufferAddition1(),
                initialRiboPlate1);
        labEventFactory.buildFromBettaLimsPlateEventDbFree(crspRiboPlatingJaxbBuilder.getInitialRiboBufferAddition2(),
                initialRiboPlate2);

        //PolyATSAliquot
        LabEventTest.validateWorkflow(LabEventType.POLY_A_TS_ALIQUOT.getName(), mapBarcodeToTube.values());
        polyATSAliquot = labEventFactory.buildFromBettaLims(
                crspRiboPlatingJaxbBuilder.getPolyATSAliquot(), mapBarcodeToVessel);

        polyAAliquotRackBarcode = crspRiboPlatingJaxbBuilder.getPolyAAliquotRackBarcode();
        polyAAliquotTubeFormation = (TubeFormation) polyATSAliquot.getTargetLabVessels().iterator().next();
        polyAAliquotBarcodedTubeMap =
                new LinkedHashMap<>(polyAAliquotTubeFormation.getContainerRole().getContainedVessels().size());

        for (VesselPosition vesselPosition : polyAAliquotTubeFormation.getVesselGeometry().getVesselPositions()) {
            BarcodedTube vesselAtPosition =
                    polyAAliquotTubeFormation.getContainerRole().getVesselAtPosition(vesselPosition);
            if (vesselAtPosition != null) {
                polyAAliquotBarcodedTubeMap.put(vesselAtPosition.getLabel(), vesselAtPosition);
            }
        }

        //RiboMicrofluorTransfer
        TubeFormation polyAAliquotTS =
                (TubeFormation) polyATSAliquot.getTargetLabVessels().iterator().next();
        LabEventTest.validateWorkflow(LabEventType.RIBO_MICROFLUOR_TRANSFER.getName(), polyAAliquotTS);
        mapBarcodeToVessel.clear();
        for (BarcodedTube barcodedTube : polyAAliquotTS.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        for (ReceptacleType receptacleType : crspRiboPlatingJaxbBuilder.getRiboMicrofluorEventJaxbA2().getSourcePositionMap()
                .getReceptacle()) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(receptacleType.getBarcode());
            if (barcodedTube != null) {
                mapBarcodeToVessel.put(receptacleType.getBarcode(), barcodedTube);
            }
        }

        riboMicrofluorEntityA2 = labEventFactory.buildFromBettaLims(
                crspRiboPlatingJaxbBuilder.getRiboMicrofluorEventJaxbA2(), mapBarcodeToVessel);
        labEventHandler.processEvent(riboMicrofluorEntityA2);

        // asserts
        riboMicrofluorPlate = (StaticPlate) riboMicrofluorEntityA2.getTargetLabVessels().iterator().next();
        Assert.assertEquals(riboMicrofluorPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        riboMicrofluorEntityB1 = labEventFactory.buildFromBettaLims(
                crspRiboPlatingJaxbBuilder.getRiboMicrofluorEventJaxbB1(), mapBarcodeToVessel);
        labEventHandler.processEvent(riboMicrofluorEntityB1);

        //RiboBufferAddition
        LabEventTest.validateWorkflow(LabEventType.RIBO_BUFFER_ADDITION.getName(), riboMicrofluorPlate);
        LabEvent riboBufferAdditionEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                crspRiboPlatingJaxbBuilder.getRiboBufferAdditionJaxb(), riboMicrofluorPlate);
        labEventHandler.processEvent(riboBufferAdditionEvent);

        //PolyATSAliquotSpike
        LabEventTest.validateWorkflow(LabEventType.POLY_A_TS_ALIQUOT_SPIKE.getName(), polyAAliquotTS);
        polyAAliquotSpikeEvent = labEventFactory.buildFromBettaLimsRackEventDbFree(
                crspRiboPlatingJaxbBuilder.getPolyASpikeJaxb(), polyAAliquotTS, mapBarcodeToTube, null);
        labEventHandler.processEvent(polyAAliquotSpikeEvent);

        return this;
    }
}
