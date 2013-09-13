package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds entity graph for Qtp events
 */
public class QtpEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final List<TubeFormation> normCatchRacks;
    private final List<String> normCatchRackBarcodes;
    private final List<List<String>> listLcsetListNormCatchBarcodes;
    private final Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;

    private TubeFormation denatureRack;
    private String testPrefix;
    private TubeFormation normalizationRack;

    public QtpEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                            LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                            List<TubeFormation> normCatchRacks,
                            List<String> normCatchRackBarcodes, List<List<String>> listLcsetListNormCatchBarcodes,
                            Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes,
                            String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.normCatchRacks = normCatchRacks;
        this.normCatchRackBarcodes = normCatchRackBarcodes;
        this.listLcsetListNormCatchBarcodes = listLcsetListNormCatchBarcodes;
        this.mapBarcodeToNormCatchTubes = mapBarcodeToNormCatchTubes;
        this.testPrefix = testPrefix;
    }

    public QtpEntityBuilder invoke() {
        return invoke(true);
    }

    public QtpEntityBuilder invoke(boolean doEco) {
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                listLcsetListNormCatchBarcodes, normCatchRackBarcodes, doEco).invoke();
        PlateCherryPickEvent cherryPickJaxb = qtpJaxbBuilder.getPoolingTransferJaxb();
        PlateCherryPickEvent denatureJaxb = qtpJaxbBuilder.getDenatureJaxb();
        String normalizationRackBarcode = qtpJaxbBuilder.getNormalizationRackBarcode();
        PlateCherryPickEvent normalizationJaxb = qtpJaxbBuilder.getNormalizationJaxb();
        final String denatureRackBarcode = qtpJaxbBuilder.getDenatureRackBarcode();

        List<TwoDBarcodedTube> poolTubes = new ArrayList<>();
        List<TubeFormation> poolingRacks = new ArrayList<>();
        for (TubeFormation normCatchRack : normCatchRacks) {
            // PoolingTransfer
            LabEventTest.validateWorkflow("PoolingTransfer", normCatchRack);
            Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
            mapBarcodeToVessel.put(normCatchRack.getLabel(), normCatchRack);
            mapBarcodeToVessel.putAll(mapBarcodeToNormCatchTubes);
            LabEvent poolingEntity = labEventFactory.buildFromBettaLims(cherryPickJaxb, mapBarcodeToVessel);
            labEventHandler.processEvent(poolingEntity);
            // asserts
            TubeFormation poolingRack = (TubeFormation) poolingEntity.getTargetLabVessels().iterator().next();
            poolingRacks.add(poolingRack);
            poolTubes.add(poolingRack.getContainerRole().getVesselAtPosition(VesselPosition.A01));
            Set<SampleInstance> pooledSampleInstances =
                    poolingRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(pooledSampleInstances.size(), normCatchRack.getSampleInstances().size(),
                    "Wrong number of pooled samples");
        }

        // Pre-EcoTransfer/DenatureTransfer rearray
        TubeFormation rearrayedPoolingRack;
        if (poolTubes.size() > 1) {
            Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<>();
            for (int j = 0; j < poolTubes.size(); j++) {
                mapPositionToTube.put(VesselPosition.getByName(bettaLimsMessageTestFactory.buildWellName(j + 1,
                        BettaLimsMessageTestFactory.WellNameType.SHORT)),
                        poolTubes.get(j));
            }
            rearrayedPoolingRack = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
            rearrayedPoolingRack.addRackOfTubes(new RackOfTubes("poolRearray", RackOfTubes.RackType.Matrix96));
        } else {
            rearrayedPoolingRack = poolingRacks.get(0);
        }

        int catchSampleInstanceCount = 0;
        for (TubeFormation normCatchRack : normCatchRacks) {
            catchSampleInstanceCount += normCatchRack.getSampleInstances().size();
        }

        // EcoTransfer or Viia7Transfer
        String ecoViia7Step = doEco ? "EcoTransfer" : "Viia7Transfer";
        PlateTransferEventType ecoViia7Event =
                doEco ? qtpJaxbBuilder.getEcoTransferJaxb() : qtpJaxbBuilder.getViia7TransferJaxb();

        LabEventTest.validateWorkflow(ecoViia7Step, rearrayedPoolingRack);
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(rearrayedPoolingRack.getLabel(), rearrayedPoolingRack);
        for (TwoDBarcodedTube twoDBarcodedTube : rearrayedPoolingRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(twoDBarcodedTube.getLabel(), twoDBarcodedTube);
        }
        LabEvent ecoViia7TransferEventEntity = labEventFactory.buildFromBettaLims(ecoViia7Event, mapBarcodeToVessel);
        labEventHandler.processEvent(ecoViia7TransferEventEntity);
        // asserts
        StaticPlate ecoPlate = (StaticPlate) ecoViia7TransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(ecoPlate.getSampleInstances().size(), catchSampleInstanceCount,
                "Wrong number of sample instances");

        // Normalization
        LabEventTest.validateWorkflow("NormalizationTransfer", rearrayedPoolingRack);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(rearrayedPoolingRack.getLabel(), rearrayedPoolingRack);
        for (TwoDBarcodedTube poolTube : poolTubes) {
            mapBarcodeToVessel.put(poolTube.getLabel(), poolTube);
        }
        LabEvent normalizationEntity = labEventFactory.buildFromBettaLims(normalizationJaxb, mapBarcodeToVessel);
        labEventHandler.processEvent(normalizationEntity);
        // asserts
        normalizationRack = (TubeFormation) normalizationEntity.getTargetLabVessels().iterator().next();
        Set<SampleInstance> normalizedSampleInstances =
                normalizationRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
        Assert.assertEquals(normalizedSampleInstances.size(), catchSampleInstanceCount,
                "Wrong number of normalized samples");
        Assert.assertEquals(
                normalizationRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances()
                        .size(),
                catchSampleInstanceCount, "Wrong number of normalized samples");

        // DenatureTransfer
        LabEventTest.validateWorkflow("DenatureTransfer", rearrayedPoolingRack);
        Map<String, TwoDBarcodedTube> mapBarcodeToDenatureTube = new HashMap<>();
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(normalizationRack.getLabel(), normalizationRack);
        for (TwoDBarcodedTube twoDBarcodedTube : normalizationRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(twoDBarcodedTube.getLabel(), twoDBarcodedTube);
        }
        mapBarcodeToVessel.put(normalizationRackBarcode, normalizationRack.getRacksOfTubes().iterator().next());

        LabEvent denatureEntity = labEventFactory.buildFromBettaLims(denatureJaxb, mapBarcodeToVessel);
        labEventHandler.processEvent(denatureEntity);
        // asserts
        denatureRack = (TubeFormation) denatureEntity.getTargetLabVessels().iterator().next();
        Set<SampleInstance> denaturedSampleInstances =
                denatureRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);

        Assert.assertEquals(denaturedSampleInstances.size(), catchSampleInstanceCount,
                "Wrong number of denatured samples");
        Assert.assertEquals(
                denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances().size(),
                catchSampleInstanceCount, "Wrong number of denatured samples");
        return this;
    }

    public TubeFormation getDenatureRack() {
        return denatureRack;
    }

}
