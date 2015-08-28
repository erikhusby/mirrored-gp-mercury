package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
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
 * Builds entity graph for Ribo Plating Pico events
 */
public class CrspRiboPlatingEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final String rackBarcode;
    private final String testPrefix;
    private LabEvent riboMicrofluorEntity;
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

    public CrspRiboPlatingEntityBuilder invoke() {
        CrspRiboPlatingJaxbBuilder crspRiboPlatingJaxbBuilder = new CrspRiboPlatingJaxbBuilder(rackBarcode,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, bettaLimsMessageTestFactory).invoke();

        //Bucket
        LabEventTest.validateWorkflow(LabEventType.TRU_SEQ_STRAND_SPECIFIC_BUCKET.getName(), mapBarcodeToTube.values());
        LabEvent riboPlatingBucket = labEventFactory.buildFromBettaLimsRackEventDbFree(
                crspRiboPlatingJaxbBuilder.getTruSeqStrandSpecificBucket(), null, mapBarcodeToTube, null);
        labEventHandler.processEvent(riboPlatingBucket);

        //PolyATSAliquot
        LabEventTest.validateWorkflow(LabEventType.POLY_A_TS_ALIQUOT.getName(), mapBarcodeToTube.values());
        Map<String, LabVessel> mapBarcodeToVessel = new LinkedHashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
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
        for (ReceptacleType receptacleType : crspRiboPlatingJaxbBuilder.getRiboMicrofluorEventJaxb().getSourcePositionMap()
                .getReceptacle()) {
            BarcodedTube barcodedTube = mapBarcodeToTube.get(receptacleType.getBarcode());
            if (barcodedTube != null) {
                mapBarcodeToVessel.put(receptacleType.getBarcode(), barcodedTube);
            }
        }

        riboMicrofluorEntity = labEventFactory.buildFromBettaLims(
                crspRiboPlatingJaxbBuilder.getRiboMicrofluorEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(riboMicrofluorEntity);

        // asserts
        riboMicrofluorPlate = (StaticPlate) riboMicrofluorEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(riboMicrofluorPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

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
