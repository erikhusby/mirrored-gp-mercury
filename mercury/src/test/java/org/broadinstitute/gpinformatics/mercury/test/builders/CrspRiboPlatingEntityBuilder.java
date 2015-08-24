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
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds entity graph for Ribo Plating Pico events
 */
public class CrspRiboPlatingEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final TubeFormation inputRack;
    private final String rackBarcode;
    private final String testPrefix;
    private LabEvent riboDilutionTransferEventEntity;
    private StaticPlate riboDilutionPlate;
    private LabEvent riboMicrofluorEntity;
    private StaticPlate riboMicrofluorPlate;
    private LabEvent initialNormalizationEntity;
    private TubeFormation polyAAliquotRack;

    public CrspRiboPlatingEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                        LabEventFactory labEventFactory,
                                        LabEventHandler labEventHandler, Map<String, BarcodedTube> mapBarcodeToTube,
                                        TubeFormation inputRack,
                                        String rackBarcode, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.inputRack = inputRack;
        this.rackBarcode = rackBarcode;
        this.testPrefix = testPrefix;
    }

    public CrspRiboPlatingEntityBuilder invoke() {
        CrspRiboPlatingJaxbBuilder crspRiboPlatingJaxbBuilder = new CrspRiboPlatingJaxbBuilder(rackBarcode,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix, bettaLimsMessageTestFactory).invoke();

        // RiboDilutionTransfer
        LabEventTest.validateWorkflow(LabEventType.RIBO_DILUTION_TRANSFER.getName(), mapBarcodeToTube.values());
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(inputRack.getLabel(), inputRack);
        for (BarcodedTube barcodedTube : inputRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        riboDilutionTransferEventEntity = labEventFactory.buildFromBettaLims(
                crspRiboPlatingJaxbBuilder.getRiboDilutionEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(riboDilutionTransferEventEntity);

        // asserts
        riboDilutionPlate = (StaticPlate) riboDilutionTransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(riboDilutionPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        //RiboMicrofluorTransfer
        LabEventTest.validateWorkflow(LabEventType.RIBO_MICROFLUOR_TRANSFER.getName(), riboDilutionPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(riboDilutionPlate.getLabel(), riboDilutionPlate);
        riboMicrofluorEntity = labEventFactory.buildFromBettaLims(
                crspRiboPlatingJaxbBuilder.getRiboMicrofluorEventJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(riboMicrofluorEntity);
        riboMicrofluorPlate = (StaticPlate) riboMicrofluorEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(riboMicrofluorPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        //RiboBufferAddition
        LabEventTest.validateWorkflow(LabEventType.RIBO_BUFFER_ADDITION.getName(), riboMicrofluorPlate);
        LabEvent riboBufferAdditionEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                crspRiboPlatingJaxbBuilder.getRiboBufferAdditionJaxb(), riboMicrofluorPlate);
        labEventHandler.processEvent(riboBufferAdditionEvent);

        //InitialNormalization
        LabEventTest.validateWorkflow(LabEventType.INITIAL_NORMALIZATION.getName(), mapBarcodeToTube.values());
        initialNormalizationEntity = labEventFactory.buildFromBettaLimsRackEventDbFree(
                crspRiboPlatingJaxbBuilder.getInitialNormalizationJaxb(), inputRack, mapBarcodeToTube, null);
        labEventHandler.processEvent(initialNormalizationEntity);

        //PolyATSAliquot
        LabEventTest.validateWorkflow(LabEventType.POLY_A_TS_ALIQUOT.getName(), mapBarcodeToTube.values());
        mapBarcodeToVessel.clear();
        for (ReceptacleType receptacleType :
                crspRiboPlatingJaxbBuilder.getPolyATSAliquot().getSourcePositionMap().getReceptacle()) {
            mapBarcodeToVessel.put(receptacleType.getBarcode(), mapBarcodeToTube.get(receptacleType.getBarcode()));
        }

        LabEvent polyATSAliquot = labEventFactory.buildFromBettaLims(crspRiboPlatingJaxbBuilder.getPolyATSAliquot(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(polyATSAliquot);
        polyAAliquotRack = (TubeFormation) polyATSAliquot.getTargetLabVessels().iterator().next();

        Assert.assertEquals(polyAAliquotRack.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        return this;
    }
}
