package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.presentation.transfervis.TransferVisualizerFrame;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.*;

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
    private final WorkflowName workflowName;

    private TubeFormation denatureRack;
    private IlluminaFlowcell illuminaFlowcell;
    private StripTube stripTube;
    private String testPrefix;
    private TubeFormation normalizationRack;

    public QtpEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                            LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                            List<TubeFormation> normCatchRacks,
                            List<String> normCatchRackBarcodes, List<List<String>> listLcsetListNormCatchBarcodes,
                            Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes,
                            WorkflowName workflowName, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.normCatchRacks = normCatchRacks;
        this.normCatchRackBarcodes = normCatchRackBarcodes;
        this.listLcsetListNormCatchBarcodes = listLcsetListNormCatchBarcodes;
        this.mapBarcodeToNormCatchTubes = mapBarcodeToNormCatchTubes;
        this.workflowName = workflowName;
        this.testPrefix = testPrefix;
    }

    public QtpEntityBuilder invoke() {
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                listLcsetListNormCatchBarcodes, normCatchRackBarcodes, workflowName).invoke();
        PlateCherryPickEvent cherryPickJaxb = qtpJaxbBuilder.getPoolingTransferJaxb();
        final String poolRackBarcode = qtpJaxbBuilder.getPoolRackBarcode();
        final String ecoPlateBarcode = qtpJaxbBuilder.getEcoPlateBarcode();
        PlateCherryPickEvent denatureJaxb = qtpJaxbBuilder.getDenatureJaxb();
        final String normalizationRackBarcode = qtpJaxbBuilder.getNormalizationRackBarcode();
        PlateCherryPickEvent normalizationJaxb = qtpJaxbBuilder.getNormalizationJaxb();
        final String denatureRackBarcode = qtpJaxbBuilder.getDenatureRackBarcode();
        PlateCherryPickEvent stripTubeTransferJaxb = qtpJaxbBuilder.getStripTubeTransferJaxb();
        final String stripTubeHolderBarcode = qtpJaxbBuilder.getStripTubeHolderBarcode();
        PlateTransferEventType flowcellTransferJaxb = qtpJaxbBuilder.getFlowcellTransferJaxb();

        ReceptacleEventType flowcellLoadJaxb = qtpJaxbBuilder.getFlowcellLoad();
//        Map<String, TwoDBarcodedTube> mapBarcodeToPoolTube = null;

        int i = 0;
        List<TwoDBarcodedTube> poolTubes = new ArrayList<TwoDBarcodedTube>();
        List<TubeFormation> poolingRacks = new ArrayList<TubeFormation>();
        for (final TubeFormation normCatchRack : normCatchRacks) {
            // PoolingTransfer
            LabEventTest.validateWorkflow("PoolingTransfer", normCatchRack);
//            mapBarcodeToPoolTube = new HashMap<String, TwoDBarcodedTube>();
            final int finalI = i;
//            LabEvent poolingEntity = labEventFactory.buildCherryPickRackToRackDbFree(cherryPickJaxb,
//                    new HashMap<String, TubeFormation>() {{
//                        put(normCatchRackBarcodes.get(finalI), normCatchRack);
//                    }},
//                    new HashMap<String, RackOfTubes>(),
//                    mapBarcodeToNormCatchTubes,
//                    new HashMap<String, TubeFormation>() {{
//                        put(poolRackBarcode, null);
//                    }}, mapBarcodeToPoolTube, null
//            );
            Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
            mapBarcodeToVessel.put(normCatchRackBarcodes.get(finalI), normCatchRack);
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
            i++;
        }

        // Pre-EcoTransfer/DenatureTransfer rearray
        TubeFormation rearrayedPoolingRack;
        if (poolTubes.size() > 1) {
            Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
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

        // EcoTransfer
        LabEventTest.validateWorkflow("EcoTransfer", rearrayedPoolingRack);
        LabEvent ecoTransferEventEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(qtpJaxbBuilder.getEcoTransferJaxb(), rearrayedPoolingRack, null);
        labEventHandler.processEvent(ecoTransferEventEntity);
        // asserts
        StaticPlate ecoPlate = (StaticPlate) ecoTransferEventEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(ecoPlate.getSampleInstances().size(), catchSampleInstanceCount,
                "Wrong number of sample instances");

        // Normalization
        LabEventTest.validateWorkflow("NormalizationTransfer", rearrayedPoolingRack);
//        Map<String, TwoDBarcodedTube> mapBarcodeToNormalizationTube = new HashMap<String, TwoDBarcodedTube>();
        Map<String, TubeFormation> mapNormalizationBarcodeToPoolRack = new HashMap<String, TubeFormation>();
        mapNormalizationBarcodeToPoolRack.put(poolRackBarcode, rearrayedPoolingRack);
//        LabEvent normalizationEntity = labEventFactory.buildCherryPickRackToRackDbFree(normalizationJaxb,
//                mapNormalizationBarcodeToPoolRack,
//                new HashMap<String, RackOfTubes>(),
//                mapBarcodeToPoolTube,
//                new HashMap<String, TubeFormation>() {{
//                    put(normalizationRackBarcode, null);
//                }}, mapBarcodeToNormalizationTube, null
//        );
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
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
        Map<String, TwoDBarcodedTube> mapBarcodeToDenatureTube = new HashMap<String, TwoDBarcodedTube>();

        Map<String, TubeFormation> mapBarcodeToNormRack = new HashMap<String, TubeFormation>() {{
            put(normalizationRackBarcode, normalizationRack);
        }};

//        LabEvent denatureEntity = labEventFactory.buildCherryPickRackToRackDbFree(denatureJaxb,
//                mapBarcodeToNormRack,
//                new HashMap<String, RackOfTubes>() {{
//                    put(normalizationRackBarcode, normalizationRack.getRacksOfTubes().iterator().next());
//                }
//                },
//                mapBarcodeToNormalizationTube,
//                new HashMap<String, TubeFormation>() {{
//                    put(denatureRackBarcode, null);
//                }}, mapBarcodeToDenatureTube, null
//        );
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.putAll(mapBarcodeToNormRack);
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

        if (true) {
            TransferVisualizerFrame transferVisualizerFrame = new TransferVisualizerFrame();
            transferVisualizerFrame.renderVessel(denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01));
            try {
                Thread.sleep(500000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        Assert.assertEquals(denaturedSampleInstances.size(), catchSampleInstanceCount,
                "Wrong number of denatured samples");
        Assert.assertEquals(
                denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances().size(),
                catchSampleInstanceCount, "Wrong number of denatured samples");
        LabEvent flowcellTransferEntity;
        // StripTubeBTransfer
        if (workflowName != WorkflowName.EXOME_EXPRESS) {
            LabEventTest.validateWorkflow("StripTubeBTransfer", denatureRack);

            Map<String, StripTube> mapBarcodeToStripTube = new HashMap<String, StripTube>();
            LabEvent stripTubeTransferEntity =
                    labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeTransferJaxb,
                            new HashMap<String, TubeFormation>() {{
                                put(denatureRackBarcode, denatureRack);
                            }},
                            mapBarcodeToDenatureTube,
                            new HashMap<String, TubeFormation>() {{
                                put(stripTubeHolderBarcode, null);
                            }},
                            mapBarcodeToStripTube, new HashMap<String, RackOfTubes>()
                    );
            labEventHandler.processEvent(stripTubeTransferEntity);
            // asserts
            stripTube = (StripTube) stripTubeTransferEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(
                    stripTube.getContainerRole().getSampleInstancesAtPosition(VesselPosition.TUBE1).size(),
                    catchSampleInstanceCount,
                    "Wrong number of samples in strip tube well");

            // FlowcellTransfer
            LabEventTest.validateWorkflow("FlowcellTransfer", stripTube);
            flowcellTransferEntity = labEventFactory.buildFromBettaLimsPlateToPlateDbFree(flowcellTransferJaxb,
                    stripTube, null);
            labEventHandler.processEvent(flowcellTransferEntity);
            //asserts
            illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
            Set<SampleInstance> lane1SampleInstances = illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                    VesselPosition.LANE1);
            Assert.assertEquals(lane1SampleInstances.size(), normCatchRacks.get(0).getSampleInstances().size(),
                    "Wrong number of samples in flowcell lane");
            //FlowcellLoaded
            LabEventTest.validateWorkflow(LabEventType.FLOWCELL_LOADED.getName(), illuminaFlowcell);
            LabEvent flowcellLoadEntity = labEventFactory
                    .buildReceptacleEventDbFree(flowcellLoadJaxb, illuminaFlowcell);
            labEventHandler.processEvent(flowcellLoadEntity);
        }
        return this;
    }

    public TubeFormation getDenatureRack() {
        return denatureRack;
    }

    public IlluminaFlowcell getIlluminaFlowcell() {
        return illuminaFlowcell;
    }

    public StripTube getStripTube() {
        return stripTube;
    }
}
