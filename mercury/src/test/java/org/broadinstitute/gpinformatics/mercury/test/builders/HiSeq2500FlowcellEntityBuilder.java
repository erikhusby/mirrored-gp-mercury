package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HiSeq2500FlowcellEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String flowcellBarcode;
    private IlluminaFlowcell illuminaFlowcell;
    private LabEvent flowcellTransferEntity;
    private final TubeFormation denatureRack;
    private String testPrefix;
    private String designationName;
    private int flowcellLanes;
    private TubeFormation dilutionRack;
    private final String fctTicket;
    private final ProductionFlowcellPath productionFlowcellPath;
    private StripTube stripTube;


    public HiSeq2500FlowcellEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                          LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                          final TubeFormation denatureRack, String flowcellBarcode, String testPrefix,
                                          String fctTicket, ProductionFlowcellPath productionFlowcellPath,
                                          String designationName, int flowcellLanes) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.denatureRack = denatureRack;
        this.flowcellBarcode = flowcellBarcode;
        this.testPrefix = testPrefix;
        this.fctTicket = fctTicket;
        this.productionFlowcellPath = productionFlowcellPath;
        this.designationName = designationName;
        this.flowcellLanes = flowcellLanes;
    }

    public HiSeq2500FlowcellEntityBuilder invoke() {
        BarcodedTube denatureTube =
                TestUtils.getFirst(denatureRack.getContainerRole().getContainedVessels());
        Assert.assertNotNull(denatureTube);
        List<String> denatureTubeBarcodes = new ArrayList<>();
        for (VesselPosition vesselPosition : denatureRack.getRackType().getVesselGeometry().getVesselPositions()) {
            BarcodedTube vesselAtPosition = denatureRack.getContainerRole().getVesselAtPosition(vesselPosition);
            if (vesselAtPosition != null) {
                denatureTubeBarcodes.add(vesselAtPosition.getLabel());
            }
        }

        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                denatureTubeBarcodes, denatureRack.getRacksOfTubes().iterator().next().getLabel(), fctTicket,
                productionFlowcellPath, denatureRack.getSampleInstanceCount(), designationName, flowcellLanes);
        hiSeq2500JaxbBuilder.invoke();
        PlateCherryPickEvent dilutionJaxb = hiSeq2500JaxbBuilder.getDilutionJaxb();
        ReceptaclePlateTransferEvent flowcellTransferJaxb = hiSeq2500JaxbBuilder.getFlowcellTransferJaxb();
        PlateCherryPickEvent stripTubeTransferJaxb = hiSeq2500JaxbBuilder.getStripTubeTransferJaxb();
        final String stripTubeHolderBarcode = hiSeq2500JaxbBuilder.getStripTubeHolderBarcode();
        PlateTransferEventType stbFlowcellTransferJaxb = hiSeq2500JaxbBuilder.getStbFlowcellTransferJaxb();

        ReceptacleEventType flowcellLoadJaxb = hiSeq2500JaxbBuilder.getFlowcellLoad();

        // Used to verify that the tube loading the flowcell is correct. This is filled in in the body of switch statement.
        String expectedNearestTubeLabel = denatureTube.getLabel();

        switch (productionFlowcellPath) {
        case DILUTION_TO_FLOWCELL:
            //DenatureToDilution
            LabEventTest.validateWorkflow("DenatureToDilutionTransfer", denatureRack);

            LabEvent dilutionTransferEntity =
                    labEventFactory.buildFromBettaLims(dilutionJaxb, new HashMap<String, LabVessel>() {{
                        put(denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01).getLabel(),
                                denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01));
                        put(denatureRack.getLabel(), denatureRack);
                    }});
            labEventFactory.getEventHandlerSelector().applyEventSpecificHandling(dilutionTransferEntity, dilutionJaxb);
            labEventHandler.processEvent(dilutionTransferEntity);
            dilutionRack = (TubeFormation) TestUtils.getFirst(dilutionTransferEntity.getTargetLabVessels());
            Assert.assertNotNull(dilutionRack);
            Assert.assertEquals(dilutionRack.getContainerRole().getContainedVessels().size(), 1);

            // DilutionToFlowcellTransfer
            LabEventTest.validateWorkflow("DilutionToFlowcellTransfer", dilutionRack);
            BarcodedTube dilutionTube =
                    TestUtils.getFirst(dilutionRack.getContainerRole().getContainedVessels());
            Assert.assertNotNull(dilutionTube);

            // Tube loaded onto the flowcell is the dilution tube.
            expectedNearestTubeLabel = dilutionTube.getLabel();
            LabEvent flowcellTransferEntity =
                    labEventFactory.buildVesselToSectionDbFree(flowcellTransferJaxb, dilutionTube, null,
                    SBSSection.ALL2.getSectionName());
            labEventFactory.getEventHandlerSelector().applyEventSpecificHandling(flowcellTransferEntity,
                    flowcellTransferJaxb);
            labEventHandler.processEvent(flowcellTransferEntity);
            //asserts
            illuminaFlowcell =
                    (IlluminaFlowcell) TestUtils.getFirst(flowcellTransferEntity.getTargetLabVessels());

            break;
        case DENATURE_TO_FLOWCELL:
            LabEventTest.validateWorkflow("DenatureToFlowcellTransfer", denatureRack);
            flowcellTransferEntity = labEventFactory.buildVesselToSectionDbFree(flowcellTransferJaxb,
                    denatureTube, null,
                    SBSSection.ALL2.getSectionName());

            labEventFactory.getEventHandlerSelector()
                    .applyEventSpecificHandling(flowcellTransferEntity, flowcellTransferJaxb);
            labEventHandler.processEvent(flowcellTransferEntity);
            illuminaFlowcell =
                    (IlluminaFlowcell) TestUtils.getFirst(flowcellTransferEntity.getTargetLabVessels());
            break;
        case STRIPTUBE_TO_FLOWCELL:
            Map<String, BarcodedTube> mapBarcodeToDenatureTube = new HashMap<>();
            mapBarcodeToDenatureTube.put(denatureTube.getLabel(), denatureTube);

            LabEventTest.validateWorkflow("StripTubeBTransfer", denatureRack);

            Map<String, StripTube> mapBarcodeToStripTube = new HashMap<>();
            int catchSampleInstanceCount = denatureRack.getSampleInstancesV2().size();
            LabEvent stripTubeTransferEntity =
                    labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeTransferJaxb,
                            new HashMap<String, TubeFormation>() {{
                                put(denatureRack.getRacksOfTubes().iterator().next().getLabel(), denatureRack);
                            }},
                            mapBarcodeToDenatureTube,
                            new HashMap<String, TubeFormation>() {{
                                put(stripTubeHolderBarcode, null);
                            }},
                            mapBarcodeToStripTube, new HashMap<String, RackOfTubes>()
                    );
            labEventFactory.getEventHandlerSelector()
                    .applyEventSpecificHandling(stripTubeTransferEntity, stripTubeTransferJaxb);
            labEventHandler.processEvent(stripTubeTransferEntity);
            // asserts
            stripTube = (StripTube) TestUtils.getFirst(stripTubeTransferEntity.getTargetLabVessels());
            Assert.assertNotNull(stripTube);
            Assert.assertEquals(
                    stripTube.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.TUBE1).size(),
                    catchSampleInstanceCount,
                    "Wrong number of samples in strip tube well");

            // FlowcellTransfer
            LabEventTest.validateWorkflow("FlowcellTransfer", stripTube);
            LabEvent stbFlowcellTransferEntity =
                    labEventFactory.buildFromBettaLimsPlateToPlateDbFree(stbFlowcellTransferJaxb,
                            stripTube, null);
            labEventFactory.getEventHandlerSelector()
                    .applyEventSpecificHandling(stbFlowcellTransferEntity, stbFlowcellTransferJaxb);
            labEventHandler.processEvent(stbFlowcellTransferEntity);
            //asserts
            illuminaFlowcell =
                    (IlluminaFlowcell) TestUtils.getFirst(stbFlowcellTransferEntity.getTargetLabVessels());
            break;
        }

        // Verify that the tube used to load the flowcell is the correct one.
        Assert.assertNotNull(expectedNearestTubeLabel);
        Assert.assertNotNull(illuminaFlowcell);
        for (LabVessel tube : illuminaFlowcell.getNearestTubeAncestorsForLanes().values()) {
            Assert.assertTrue(tube.getLabel().equals(expectedNearestTubeLabel));
        }

        Set<SampleInstanceV2> lane1SampleInstances =
                illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.size(), denatureTube.getSampleInstancesV2().size(),
                "Wrong number of samples in flowcell lane");

        SampleInstanceV2 sampleInstance = lane1SampleInstances.iterator().next();

        Assert.assertNotNull(sampleInstance);
        String workflowName = sampleInstance.getWorkflowName();
        Assert.assertNotNull(workflowName);

        for( SampleInstanceV2 sampleInstIteratee : lane1SampleInstances ) {
            Assert.assertNotNull(sampleInstIteratee.getWorkflowName());
        }

        int reagentsSize = 0;
        switch (workflowName) {
        case Workflow.WHOLE_GENOME:
        case Workflow.PCR_FREE:
        case Workflow.PCR_PLUS:
        case Workflow.PCR_FREE_HYPER_PREP:
        case Workflow.PCR_PLUS_HYPER_PREP:
        case Workflow.CELL_FREE_HYPER_PREP:
        case Workflow.TRU_SEQ_STRAND_SPECIFIC_CRSP:
            reagentsSize = 1;
            break;
        case Workflow.AGILENT_EXOME_EXPRESS:
        case Workflow.HYBRID_SELECTION:
            reagentsSize = 2;
            break;
        case Workflow.ICE_EXOME_EXPRESS:
        case Workflow.ICE_EXOME_EXPRESS_HYPER_PREP:
        case Workflow.ICE:
            reagentsSize = 3;
        }

        Assert.assertEquals(sampleInstance.getReagents().size(), reagentsSize, "Wrong number of reagents");

        Set<SampleInstanceV2> lane2SampleInstances =
                illuminaFlowcell.getContainerRole().getSampleInstancesAtPositionV2(VesselPosition.LANE2);

        sampleInstance = lane2SampleInstances.iterator().next();
        Assert.assertNotNull(sampleInstance);
        workflowName = sampleInstance.getWorkflowName();
        Assert.assertNotNull(workflowName);

        Assert.assertEquals(lane2SampleInstances.size(), denatureTube.getSampleInstancesV2().size(),
                "Wrong number of samples in flowcell lane");

        Assert.assertEquals(sampleInstance.getReagents().size(), reagentsSize, "Wrong number of reagents");

        LabEventTest.validateWorkflow("FlowcellLoaded", illuminaFlowcell);

        LabEvent flowcellLoadEntity = labEventFactory.buildReceptacleEventDbFree(flowcellLoadJaxb, illuminaFlowcell);
        labEventFactory.getEventHandlerSelector().applyEventSpecificHandling(flowcellLoadEntity, flowcellLoadJaxb);
        labEventHandler.processEvent(flowcellLoadEntity);

        return this;
    }

    public IlluminaFlowcell getIlluminaFlowcell() {
        return illuminaFlowcell;
    }

    public LabEvent getFlowcellTransferEntity() {
        return flowcellTransferEntity;
    }

    public TubeFormation getDenatureRack() {
        return denatureRack;
    }

    public void setDesignationName(String designationName) {
        this.designationName = designationName;
    }

    public TubeFormation getDilutionRack() {
        return dilutionRack;
    }
}
