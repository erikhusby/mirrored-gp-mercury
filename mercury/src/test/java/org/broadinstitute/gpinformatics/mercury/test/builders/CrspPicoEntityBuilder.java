package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds entity graph for CRSP Pico messages.
 */
public class CrspPicoEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String testPrefix;
    private final String rackBarcode;
    private final Map<String, BarcodedTube> mapBarcodeToTube;

    private LabEvent initialTareEntity;
    private LabEvent weightMeasurementEntity;
    private LabEvent volumeAdditionEntity;
    private LabEvent fingerprintingAliquotEntity;

    public CrspPicoEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory, LabEventHandler labEventHandler, String testPrefix, String rackBarcode,
            Map<String, BarcodedTube> mapBarcodeToTube) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.testPrefix = testPrefix;
        this.rackBarcode = rackBarcode;
        this.mapBarcodeToTube = mapBarcodeToTube;
    }

    public CrspPicoEntityBuilder invoke() {
        List<String> tubeBarcodes = new ArrayList<>(mapBarcodeToTube.keySet());
        CrspPicoJaxbBuilder crspPicoJaxbBuilder = new CrspPicoJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                rackBarcode, tubeBarcodes).invoke();

        initialTareEntity = labEventFactory.buildFromBettaLimsRackEventDbFree(crspPicoJaxbBuilder.getInitialTareJaxb(),
                null, mapBarcodeToTube, null);
        labEventHandler.processEvent(initialTareEntity);
        TubeFormation initialRack = (TubeFormation) initialTareEntity.getTargetLabVessels().iterator().next();

        weightMeasurementEntity = labEventFactory.buildFromBettaLimsRackEventDbFree(
                crspPicoJaxbBuilder.getWeightMeasurementJaxb(), initialRack, mapBarcodeToTube, null);
        labEventHandler.processEvent(weightMeasurementEntity);

        volumeAdditionEntity = labEventFactory.buildFromBettaLimsRackEventDbFree(
                crspPicoJaxbBuilder.getVolumeAdditionJaxb(), initialRack, mapBarcodeToTube, null);
        labEventHandler.processEvent(volumeAdditionEntity);

        crspPicoJaxbBuilder.getInitialPicoTransfer1();
        crspPicoJaxbBuilder.getInitialPicoTransfer2();

        crspPicoJaxbBuilder.getInitialPicoBufferAddition1();
        crspPicoJaxbBuilder.getInitialPicoBufferAddition2();

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
//        mapBarcodeToVessel.put();
        fingerprintingAliquotEntity = labEventFactory.buildFromBettaLims(
                crspPicoJaxbBuilder.getFingerprintingAliquotJaxb(), mapBarcodeToVessel);

        crspPicoJaxbBuilder.getFingerprintingPlateSetup();

        crspPicoJaxbBuilder.getShearingAliquot();

        return this;
    }
}
