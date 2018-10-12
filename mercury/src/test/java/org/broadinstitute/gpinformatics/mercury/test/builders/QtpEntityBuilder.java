package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
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
    private List<BarcodedTube> poolTubes = new ArrayList<>();

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
        return invoke(true, QtpJaxbBuilder.PcrType.ECO_DUPLICATE);
    }

    public QtpEntityBuilder invoke(boolean doPoolingTransfer, QtpJaxbBuilder.PcrType pcrType) {
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                listLcsetListNormCatchBarcodes, normCatchRackBarcodes, doPoolingTransfer, pcrType);
        qtpJaxbBuilder.invokeToQuant();
        qtpJaxbBuilder.invokePostQuant();
        PlateCherryPickEvent cherryPickJaxb = qtpJaxbBuilder.getPoolingTransferJaxb();
        PlateCherryPickEvent denatureJaxb = qtpJaxbBuilder.getDenatureJaxb();
        String normalizationRackBarcode = qtpJaxbBuilder.getNormalizationRackBarcode();
        PlateCherryPickEvent normalizationJaxb = qtpJaxbBuilder.getNormalizationJaxb();

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
                Set<SampleInstanceV2> pooledSampleInstances =
                        poolingRack.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
                Assert.assertEquals(pooledSampleInstances.size(), normCatchRack.getSampleInstancesV2().size(),
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
            totalSampleInstanceCount += normCatchRack.getSampleInstancesV2().size();
        }
        perTubeSampleInstanceCount = rearrayedPoolingRack.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01).size();

        // EcoTransfer or Viia7Transfer
        boolean isEcoSetup = pcrType == QtpJaxbBuilder.PcrType.ECO_DUPLICATE ||
                             pcrType == QtpJaxbBuilder.PcrType.ECO_TRIPLICATE;

        String ecoViia7Step = isEcoSetup ? "EcoTransfer" : "Viia7Transfer";
        LabEventTest.validateWorkflow(ecoViia7Step, rearrayedPoolingRack);

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(rearrayedPoolingRack.getLabel(), rearrayedPoolingRack);
        for (BarcodedTube barcodedTube : rearrayedPoolingRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        switch (pcrType) {
            case ECO_DUPLICATE: {
                PlateTransferEventType ecoDuplicateA3Event = qtpJaxbBuilder.getEcoTransferDuplicateA3Jaxb();
                PlateTransferEventType ecoDuplicateB3Event = qtpJaxbBuilder.getEcoTransferDuplicateB3Jaxb();

                LabEvent ecoDuplicateA3TransferEventEntity =
                        labEventFactory.buildFromBettaLims(ecoDuplicateA3Event, mapBarcodeToVessel);
                LabEvent ecoDuplicateB3TransferEventEntity =
                        labEventFactory.buildFromBettaLims(ecoDuplicateB3Event, mapBarcodeToVessel);

                labEventHandler.processEvent(ecoDuplicateA3TransferEventEntity);
                labEventHandler.processEvent(ecoDuplicateB3TransferEventEntity);

                StaticPlate ecoPlate =
                        (StaticPlate) ecoDuplicateA3TransferEventEntity.getTargetLabVessels().iterator().next();

                Set<SampleInstanceV2> ecoSampleInstancesA3 =
                        ecoPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A03);
                Set<SampleInstanceV2> ecoSampleInstancesB3 =
                        ecoPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.B03);
                Assert.assertEquals(ecoSampleInstancesA3.size(), totalSampleInstanceCount,
                        "Wrong number of sample instances");
                Assert.assertEquals(ecoSampleInstancesB3.size(), totalSampleInstanceCount,
                        "Wrong number of sample instances");
                break;
            }
            case ECO_TRIPLICATE:
                PlateTransferEventType ecoTripA3Event = qtpJaxbBuilder.getEcoTransferTriplicateA3();
                PlateTransferEventType ecoTripA5Event = qtpJaxbBuilder.getEcoTransferTriplicateA5();
                PlateTransferEventType ecoTripA7Event = qtpJaxbBuilder.getEcoTransferTriplicateA7();
                LabEvent ecoTripA3TransferEventEntity = labEventFactory.buildFromBettaLims(ecoTripA3Event,
                        mapBarcodeToVessel);
                LabEvent ecoTripA5TransferEventEntity = labEventFactory.buildFromBettaLims(ecoTripA5Event,
                        mapBarcodeToVessel);
                LabEvent ecoTripA7TransferEventEntity = labEventFactory.buildFromBettaLims(ecoTripA7Event, mapBarcodeToVessel);
                labEventHandler.processEvent(ecoTripA3TransferEventEntity);
                labEventHandler.processEvent(ecoTripA5TransferEventEntity);
                labEventHandler.processEvent(ecoTripA7TransferEventEntity);
                StaticPlate ecoPlate = (StaticPlate) ecoTripA3TransferEventEntity.getTargetLabVessels().iterator().next();
                //
                Set<SampleInstanceV2> ecoSampleInstancesA3 =
                        ecoPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A03);
                Set<SampleInstanceV2> ecoSampleInstancesB5 =
                        ecoPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.B05);
                Set<SampleInstanceV2> ecoSampleInstancesA7 =
                        ecoPlate.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A07);
                Assert.assertEquals(ecoSampleInstancesA3.size(), totalSampleInstanceCount,
                        "Wrong number of sample instances");
                Assert.assertEquals(ecoSampleInstancesB5.size(), totalSampleInstanceCount,
                        "Wrong number of sample instances");
                Assert.assertEquals(ecoSampleInstancesA7.size(), totalSampleInstanceCount,
                        "Wrong number of sample instances");
                break;
            case VIIA_7:
                PlateTransferEventType viia7Event = qtpJaxbBuilder.getViia7TransferJaxb();
                LabEvent viia7TransferEventEntity = labEventFactory.buildFromBettaLims(viia7Event, mapBarcodeToVessel);
                labEventHandler.processEvent(viia7TransferEventEntity);
                StaticPlate viiaPlate = (StaticPlate) viia7TransferEventEntity.getTargetLabVessels().iterator().next();
                Assert.assertEquals(viiaPlate.getSampleInstancesV2().size(), totalSampleInstanceCount,
                        "Wrong number of sample instances");
                break;
        }

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
        Set<SampleInstanceV2> normalizedSampleInstances =
                normalizationRack.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);
        Assert.assertEquals(normalizedSampleInstances.size(), perTubeSampleInstanceCount,
                "Wrong number of normalized samples");
        Assert.assertEquals(
                normalizationRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstancesV2()
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
        Set<SampleInstanceV2> denaturedSampleInstances =
                denatureRack.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.A01);

        Assert.assertEquals(denaturedSampleInstances.size(), perTubeSampleInstanceCount,
                "Wrong number of denatured samples");
        Assert.assertEquals(
                denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstancesV2().size(),
                perTubeSampleInstanceCount, "Wrong number of denatured samples");
        return this;
    }

    public TubeFormation getDenatureRack() {
        return denatureRack;
    }

    public TubeFormation getNormalizationRack() {
        return normalizationRack;
    }

    public List<BarcodedTube> getPoolTubes() {
        return poolTubes;
    }
}
