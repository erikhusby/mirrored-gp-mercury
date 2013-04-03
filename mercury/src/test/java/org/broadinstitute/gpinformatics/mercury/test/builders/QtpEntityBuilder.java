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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
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
    private final BettaLimsMessageTestFactory   bettaLimsMessageTestFactory;
    private final LabEventFactory               labEventFactory;
    private final LabEventHandler               labEventHandler;
    private final List<TubeFormation>           normCatchRacks;
    private final List<String> normCatchRackBarcodes;
    private final List<List<String>> listLcsetListNormCatchBarcodes;
    private final Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes;
    private final WorkflowName                  workflowName;

    private TubeFormation denatureRack;
    private IlluminaFlowcell illuminaFlowcell;
    private StripTube        stripTube;

    public QtpEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                            LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                            List<TubeFormation> normCatchRacks,
                            List<String> normCatchRackBarcodes, List<List<String>> listLcsetListNormCatchBarcodes,
                            Map<String, TwoDBarcodedTube> mapBarcodeToNormCatchTubes,
                            WorkflowName workflowName) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.normCatchRacks = normCatchRacks;
        this.normCatchRackBarcodes = normCatchRackBarcodes;
        this.listLcsetListNormCatchBarcodes = listLcsetListNormCatchBarcodes;
        this.mapBarcodeToNormCatchTubes = mapBarcodeToNormCatchTubes;
        this.workflowName = workflowName;
    }

    public void invoke() {
        QtpJaxbBuilder qtpJaxbBuilder = new QtpJaxbBuilder(bettaLimsMessageTestFactory, "",
                listLcsetListNormCatchBarcodes, normCatchRackBarcodes, workflowName).invoke();
        PlateCherryPickEvent cherryPickJaxb = qtpJaxbBuilder.getPoolingTransferJaxb();
        final String poolRackBarcode = qtpJaxbBuilder.getPoolRackBarcode();
        PlateCherryPickEvent denatureJaxb = qtpJaxbBuilder.getDenatureJaxb();
        final String denatureRackBarcode = qtpJaxbBuilder.getDenatureRackBarcode();
        PlateCherryPickEvent stripTubeTransferJaxb = qtpJaxbBuilder.getStripTubeTransferJaxb();
        final String stripTubeHolderBarcode = qtpJaxbBuilder.getStripTubeHolderBarcode();
        PlateTransferEventType flowcellTransferJaxb = qtpJaxbBuilder.getFlowcellTransferJaxb();

        ReceptacleEventType flowcellLoadJaxb = qtpJaxbBuilder.getFlowcellLoad();
        Map<String, TwoDBarcodedTube> mapBarcodeToPoolTube = null;

        int i = 0;
        List<TwoDBarcodedTube> poolTubes = new ArrayList<TwoDBarcodedTube>();
        List<TubeFormation> poolingRacks = new ArrayList<TubeFormation>();
        for (final TubeFormation normCatchRack : normCatchRacks) {
            // PoolingTransfer
            LabEventTest.validateWorkflow("PoolingTransfer", normCatchRack);
            mapBarcodeToPoolTube = new HashMap<String, TwoDBarcodedTube>();
            final int finalI = i;
            LabEvent poolingEntity = labEventFactory.buildCherryPickRackToRackDbFree(cherryPickJaxb,
                    new HashMap<String, TubeFormation>() {{
                        put(normCatchRackBarcodes.get(finalI), normCatchRack);
                    }},
                    new HashMap<String, RackOfTubes>(),
                    mapBarcodeToNormCatchTubes,
                    new HashMap<String, TubeFormation>() {{
                        put(poolRackBarcode, null);
                    }}, mapBarcodeToPoolTube, null
            );
            labEventHandler.processEvent(poolingEntity);
            // asserts
            TubeFormation poolingRack = (TubeFormation) poolingEntity.getTargetLabVessels().iterator().next();
            poolingRacks.add(poolingRack);
            poolTubes.add(poolingRack.getContainerRole().getVesselAtPosition(VesselPosition.A01));
            Set<SampleInstance> pooledSampleInstances = poolingRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(pooledSampleInstances.size(), normCatchRack.getSampleInstances().size(),
                                       "Wrong number of pooled samples");
            i++;
        }

        TubeFormation rearrayedPoolingRack;
        if (poolTubes.size() > 1) {
            Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
            for (int j = 0; j < poolTubes.size(); j++) {
                mapPositionToTube.put(VesselPosition.getByName(bettaLimsMessageTestFactory.buildWellName(j + 1)),
                        poolTubes.get(j));
            }
            rearrayedPoolingRack = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
            rearrayedPoolingRack.addRackOfTubes(new RackOfTubes("poolRearray", RackOfTubes.RackType.Matrix96));
        } else {
            rearrayedPoolingRack = poolingRacks.get(0);
        }

        // DenatureTransfer
        LabEventTest.validateWorkflow("DenatureTransfer", rearrayedPoolingRack);
        Map<String, TwoDBarcodedTube> mapBarcodeToDenatureTube = new HashMap<String, TwoDBarcodedTube>();
        Map<String, TubeFormation> mapBarcodeToPoolRack = new HashMap<String, TubeFormation>();
        mapBarcodeToPoolRack.put(poolRackBarcode, rearrayedPoolingRack);
        LabEvent denatureEntity = labEventFactory.buildCherryPickRackToRackDbFree(denatureJaxb,
                mapBarcodeToPoolRack,
                new HashMap<String, RackOfTubes>(),
                mapBarcodeToPoolTube,
                new HashMap<String, TubeFormation>() {{
                    put(denatureRackBarcode, null);
                }}, mapBarcodeToDenatureTube, null
        );
        labEventHandler.processEvent(denatureEntity);
        // asserts
        denatureRack = (TubeFormation) denatureEntity.getTargetLabVessels().iterator().next();
        Set<SampleInstance> denaturedSampleInstances = denatureRack.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
        int catchSampleInstanceCount = 0;
        for (TubeFormation normCatchRack : normCatchRacks) {
            catchSampleInstanceCount += normCatchRack.getSampleInstances().size();
        }

        Assert.assertEquals(denaturedSampleInstances.size(), catchSampleInstanceCount, "Wrong number of denatured samples");
        Assert.assertEquals(denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getSampleInstances().size(),
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
