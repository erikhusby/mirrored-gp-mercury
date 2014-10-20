package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.LimsQueries;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ConcentrationAndVolumeAndWeightType;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.math.BigDecimal;
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
    private LabEvent fingerprintingPlateSetupEntity;
    private LabEvent shearingAliquotEntity;
    private LabEvent initialPicoTransfer1;
    private LabEvent initialPicoTransfer2;
    private LabEvent fingerprintingPicoTransfer1;
    private LabEvent fingerprintingPicoTransfer2;
    private LabEvent shearingPicoTransfer1;
    private LabEvent shearingPicoTransfer2;

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
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        CrspPicoJaxbBuilder crspPicoJaxbBuilder = new CrspPicoJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                rackBarcode, tubeBarcodes).invoke();

        // InitialTare
        LabEventTest.validateWorkflow("InitialTare", mapBarcodeToTube.values());
        initialTareEntity = labEventFactory.buildFromBettaLimsRackEventDbFree(crspPicoJaxbBuilder.getInitialTareJaxb(),
                null, mapBarcodeToTube, null);
        labEventHandler.processEvent(initialTareEntity);
        TubeFormation initialRack = (TubeFormation) initialTareEntity.getInPlaceLabVessel();

        LimsQueries limsQueries = new LimsQueries(null, null, null);
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        Map<String, ConcentrationAndVolumeAndWeightType> mapBarcodeToConcVolDto =
                limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(mapBarcodeToVessel);
        ConcentrationAndVolumeAndWeightType concVolDto = mapBarcodeToConcVolDto.get("R1");
        Assert.assertEquals(concVolDto.getWeight(), new BigDecimal("0.63"));
        Assert.assertNull(concVolDto.getConcentration());
        Assert.assertNull(concVolDto.getVolume());

        // WeightMeasurement
        weightMeasurementEntity = labEventFactory.buildFromBettaLimsRackEventDbFree(
                crspPicoJaxbBuilder.getWeightMeasurementJaxb(), initialRack, mapBarcodeToTube, null);
        labEventHandler.processEvent(weightMeasurementEntity);

        // VolumeAddition
        volumeAdditionEntity = labEventFactory.buildFromBettaLimsRackEventDbFree(
                crspPicoJaxbBuilder.getVolumeAdditionJaxb(), initialRack, mapBarcodeToTube, null);
        labEventHandler.processEvent(volumeAdditionEntity);

        // Initial PicoTransfer
        mapBarcodeToVessel.put(initialRack.getLabel(), initialRack);
        initialPicoTransfer1 = labEventFactory.buildFromBettaLims(crspPicoJaxbBuilder.getInitialPicoTransfer1(),
                mapBarcodeToVessel);
        StaticPlate initialPicoPlate1 = (StaticPlate) initialPicoTransfer1.getTargetLabVessels().iterator().next();
        initialPicoTransfer2 = labEventFactory.buildFromBettaLims(crspPicoJaxbBuilder.getInitialPicoTransfer2(),
                mapBarcodeToVessel);
        StaticPlate initialPicoPlate2 = (StaticPlate) initialPicoTransfer1.getTargetLabVessels().iterator().next();

        labEventFactory.buildFromBettaLimsPlateEventDbFree(crspPicoJaxbBuilder.getInitialPicoBufferAddition1(),
                initialPicoPlate1);
        labEventFactory.buildFromBettaLimsPlateEventDbFree(crspPicoJaxbBuilder.getInitialPicoBufferAddition2(),
                initialPicoPlate2);

        // FingerprintingAliquot
        mapBarcodeToVessel.clear();
        // todo jmt make this more selective
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        fingerprintingAliquotEntity = labEventFactory.buildFromBettaLims(
                crspPicoJaxbBuilder.getFingerprintingAliquotJaxb(), mapBarcodeToVessel);
        mapBarcodeToConcVolDto = limsQueries.fetchConcentrationAndVolumeAndWeightForTubeBarcodes(mapBarcodeToVessel);
        concVolDto = mapBarcodeToConcVolDto.get("R1");
        Assert.assertEquals(concVolDto.getWeight(), new BigDecimal("0.63"));
        Assert.assertNull(concVolDto.getConcentration());
        Assert.assertEquals(concVolDto.getVolume(), new BigDecimal("51.00"));

        // FP PicoTransfer
        mapBarcodeToVessel.clear();
        TubeFormation fpAliquotSourceTf =
                (TubeFormation) fingerprintingAliquotEntity.getSourceLabVessels().iterator().next();
        TubeFormation fpAliquotTargetTf =
                (TubeFormation) fingerprintingAliquotEntity.getTargetLabVessels().iterator().next();
        for (BarcodedTube barcodedTube : fpAliquotTargetTf.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        // todo jmt make this more selective
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        fingerprintingPicoTransfer1 = labEventFactory.buildFromBettaLims(
                crspPicoJaxbBuilder.getFingerprintingPicoTransfer1(), mapBarcodeToVessel);
        StaticPlate fpPicoPlate1 = (StaticPlate) fingerprintingPicoTransfer1.getTargetLabVessels().iterator().next();
        fingerprintingPicoTransfer2 = labEventFactory.buildFromBettaLims(
                crspPicoJaxbBuilder.getFingerprintingPicoTransfer2(), mapBarcodeToVessel);
        StaticPlate fpPicoPlate2 = (StaticPlate) fingerprintingPicoTransfer2.getTargetLabVessels().iterator().next();

        labEventFactory.buildFromBettaLimsPlateEventDbFree(crspPicoJaxbBuilder.getFingerprintingPicoBufferAddition1(),
                fpPicoPlate1);
        labEventFactory.buildFromBettaLimsPlateEventDbFree(crspPicoJaxbBuilder.getFingerprintingPicoBufferAddition2(),
                fpPicoPlate2);

        // FingerprintingPlateSetup
        fingerprintingPlateSetupEntity = labEventFactory.buildFromBettaLims(
                crspPicoJaxbBuilder.getFingerprintingPlateSetup(), mapBarcodeToVessel);

        // ShearingAliquot
//        mapBarcodeToVessel.clear();
        shearingAliquotEntity = labEventFactory.buildFromBettaLims(crspPicoJaxbBuilder.getShearingAliquot(),
                mapBarcodeToVessel);

        // Shearing Pico
        shearingPicoTransfer1 = labEventFactory.buildFromBettaLims(crspPicoJaxbBuilder.getShearingPicoTransfer1(),
                mapBarcodeToVessel);
        StaticPlate shearingPicoPlate1 = (StaticPlate) shearingPicoTransfer1.getTargetLabVessels().iterator().next();
        shearingPicoTransfer2 = labEventFactory.buildFromBettaLims(crspPicoJaxbBuilder.getShearingPicoTransfer2(),
                mapBarcodeToVessel);
        StaticPlate shearingPicoPlate2 = (StaticPlate) shearingPicoTransfer2.getTargetLabVessels().iterator().next();

        labEventFactory.buildFromBettaLimsPlateEventDbFree(crspPicoJaxbBuilder.getShearingPicoBufferAddition1(),
                shearingPicoPlate1);
        labEventFactory.buildFromBettaLimsPlateEventDbFree(crspPicoJaxbBuilder.getShearingPicoBufferAddition2(),
                shearingPicoPlate2);
        return this;
    }

    public LabEvent getShearingAliquotEntity() {
        return shearingAliquotEntity;
    }
}
