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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
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
    private final Map<String, BarcodedTube> mapBarcodeToNormCatchTubes;
    private String testPrefix;

    private TubeFormation denatureRack;
    private TubeFormation normalizationRack;

    public QtpEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory,
            LabEventHandler labEventHandler,
            List<TubeFormation> normCatchRacks,
            List<String> normCatchRackBarcodes,
            List<List<String>> listLcsetListNormCatchBarcodes,
            Map<String, BarcodedTube> mapBarcodeToNormCatchTubes,
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
        return invoke(true, true);
    }

    public QtpEntityBuilder invoke(boolean doPoolingTransfer, boolean doEco) {
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                listLcsetListNormCatchBarcodes, normCatchRackBarcodes, doPoolingTransfer, doEco).invoke();
        PlateCherryPickEvent cherryPickJaxb = qtpJaxbBuilder.getPoolingTransferJaxb();
        PlateCherryPickEvent denatureJaxb = qtpJaxbBuilder.getDenatureJaxb();
        String normalizationRackBarcode = qtpJaxbBuilder.getNormalizationRackBarcode();
        PlateCherryPickEvent normalizationJaxb = qtpJaxbBuilder.getNormalizationJaxb();

        List<BarcodedTube> poolTubes = new ArrayList<>();
        List<TubeFormation> poolingRacks = new ArrayList<>();
        if (doPoolingTransfer) {
            for (TubeFormation normCatchRack : normCatchRacks) {
                // PoolingTransfer
                LabEventTest.validateWorkflow("PoolingTransfer", normCatchRack);
                Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
                mapBarcodeToVessel.put(normCatchRack.getLabel(), normCatchRack);
                mapBarcodeToVessel.putAll(mapBarcodeToNormCatchTubes);
                LabEvent poolingEntity = labEventFactory.buildFromBettaLims(cherryPickJaxb, mapBarcodeToVessel);
                labEventHandler.processEvent(poolingEntity);
                TubeFormation poolingRack = (TubeFormation) poolingEntity.getTargetLabVessels().iterator().next();
                poolingRacks.add(poolingRack);
                poolTubes.add(poolingRack.getContainerRole().getVesselAtPosition(VesselPosition.A01));
                // asserts
                Set<SampleInstance> pooledSampleInstances =
                        poolingRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
                Assert.assertEquals(pooledSampleInstances.size(), normCatchRack.getSampleInstances().size(),
                        "Wrong number of pooled samples");
            }
        } else {
            for (TubeFormation normCatchRack : normCatchRacks) {
                for (VesselPosition vesselPosition : normCatchRack.getRacksOfTubes().iterator().next().getRackType()
                        .getVesselGeometry().getVesselPositions()) {
                    BarcodedTube vesselAtPosition =
                            normCatchRack.getContainerRole().getVesselAtPosition(vesselPosition);
                    if (vesselAtPosition != null) {
                        poolTubes.add(vesselAtPosition);
                    }
                }
            }
            poolingRacks.addAll(normCatchRacks);
        }

        // Pre-EcoTransfer/DenatureTransfer rearray
        TubeFormation rearrayedPoolingRack;
        if (poolTubes.size() > 1) {
            Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
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

        int totalSampleInstanceCount = 0;
        int perTubeSampleInstanceCount = 0;
        for (TubeFormation normCatchRack : normCatchRacks) {
            totalSampleInstanceCount += normCatchRack.getSampleInstances().size();
        }
        perTubeSampleInstanceCount = rearrayedPoolingRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01).size();

        // EcoTransfer or Viia7Transfer
        String ecoViia7Step = doEco ? "EcoTransfer" : "Viia7Transfer";
        PlateTransferEventType ecoViia7Event =
                doEco ? qtpJaxbBuilder.getEcoTransferJaxb() : qtpJaxbBuilder.getViia7TransferJaxb();

        LabEventTest.validateWorkflow(ecoViia7Step, rearrayedPoolingRack);
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(rearrayedPoolingRack.getLabel(), rearrayedPoolingRack);
        for (BarcodedTube barcodedTube : rearrayedPoolingRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent ecoViia7TransferEventEntity = labEventFactory.buildFromBettaLims(ecoViia7Event, mapBarcodeToVessel);
        labEventHandler.processEvent(ecoViia7TransferEventEntity);
        // asserts
        StaticPlate ecoPlate = (StaticPlate) ecoViia7TransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(ecoPlate.getSampleInstances().size(), totalSampleInstanceCount,
                "Wrong number of sample instances");

        // Normalization
        LabEventTest.validateWorkflow("NormalizationTransfer", rearrayedPoolingRack);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(rearrayedPoolingRack.getLabel(), rearrayedPoolingRack);
        for (BarcodedTube poolTube : poolTubes) {
            mapBarcodeToVessel.put(poolTube.getLabel(), poolTube);
        }
        LabEvent normalizationEntity = labEventFactory.buildFromBettaLims(normalizationJaxb, mapBarcodeToVessel);
        labEventHandler.processEvent(normalizationEntity);
        // asserts
        normalizationRack = (TubeFormation) normalizationEntity.getTargetLabVessels().iterator().next();
        //
        Set<SampleInstance> normalizedSampleInstances =
                normalizationRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
        Assert.assertEquals(normalizedSampleInstances.size(), perTubeSampleInstanceCount,
                "Wrong number of normalized samples");
        Assert.assertEquals(
                normalizationRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances()
                        .size(),
                perTubeSampleInstanceCount, "Wrong number of normalized samples");

        // DenatureTransfer
        LabEventTest.validateWorkflow("DenatureTransfer", rearrayedPoolingRack);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(normalizationRack.getLabel(), normalizationRack);
        for (BarcodedTube barcodedTube : normalizationRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        mapBarcodeToVessel.put(normalizationRackBarcode, normalizationRack.getRacksOfTubes().iterator().next());

        LabEvent denatureEntity = labEventFactory.buildFromBettaLims(denatureJaxb, mapBarcodeToVessel);
        labEventHandler.processEvent(denatureEntity);
        // asserts
        denatureRack = (TubeFormation) denatureEntity.getTargetLabVessels().iterator().next();
        Set<SampleInstance> denaturedSampleInstances =
                denatureRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);

        Assert.assertEquals(denaturedSampleInstances.size(), perTubeSampleInstanceCount,
                "Wrong number of denatured samples");
        Assert.assertEquals(
                denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances().size(),
                perTubeSampleInstanceCount, "Wrong number of denatured samples");
        return this;
    }

    public TubeFormation getDenatureRack() {
        return denatureRack;
    }

}
