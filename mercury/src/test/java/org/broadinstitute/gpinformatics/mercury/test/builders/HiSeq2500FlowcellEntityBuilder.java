package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.AbstractEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.DenatureToDilutionTubeHandler;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.EventHandlerSelector;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Scott Matthews
 *         Date: 4/3/13
 *         Time: 6:31 AM
 */
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
    private TubeFormation dilutionRack;
    private final String fctTicket;
    private final ProductionFlowcellPath productionFlowcellPath;
    private StripTube stripTube;
    private final String workflowName;


    public HiSeq2500FlowcellEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                          LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                          final TubeFormation denatureRack, String flowcellBarcode, String testPrefix,
                                          String fctTicket, ProductionFlowcellPath productionFlowcellPath,
                                          String designationName, String workflowName) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.denatureRack = denatureRack;
        this.flowcellBarcode = flowcellBarcode;
        this.testPrefix = testPrefix;
        this.fctTicket = fctTicket;
        this.productionFlowcellPath = productionFlowcellPath;
        this.designationName = designationName;
        this.workflowName = workflowName;
    }

    public HiSeq2500FlowcellEntityBuilder invoke() {
        final HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder =
                new HiSeq2500JaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                        denatureRack.getContainerRole().getContainedVessels().iterator().next().getLabel(),
                        denatureRack.getLabel(), fctTicket, productionFlowcellPath,
                        denatureRack.getSampleInstanceCount(), designationName, workflowName);
        hiSeq2500JaxbBuilder.invoke();
        PlateCherryPickEvent dilutionJaxb = hiSeq2500JaxbBuilder.getDilutionJaxb();
        ReceptaclePlateTransferEvent flowcellTransferJaxb = hiSeq2500JaxbBuilder.getFlowcellTransferJaxb();
        PlateCherryPickEvent stripTubeTransferJaxb = hiSeq2500JaxbBuilder.getStripTubeTransferJaxb();
        final String stripTubeHolderBarcode = hiSeq2500JaxbBuilder.getStripTubeHolderBarcode();
        PlateTransferEventType stbFlowcellTransferJaxb = hiSeq2500JaxbBuilder.getStbFlowcellTransferJaxb();

        ReceptacleEventType flowcellLoadJaxb = hiSeq2500JaxbBuilder.getFlowcellLoad();


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
            labEventHandler.processEvent(dilutionTransferEntity);
            EventHandlerSelector eventHandlerSelector = new EventHandlerSelector();
            eventHandlerSelector.setDenatureToDilutionTubeHandler(new DenatureToDilutionTubeHandler());
            eventHandlerSelector.applyEventSpecificHandling(dilutionTransferEntity, dilutionJaxb);
            dilutionRack = (TubeFormation) dilutionTransferEntity.getTargetLabVessels().iterator().next();
            Assert.assertEquals(denatureRack.getContainerRole().getContainedVessels().size(), 1);

            // DilutionToFlowcellTransfer
            LabEventTest.validateWorkflow("DilutionToFlowcellTransfer", dilutionRack);
            LabEvent flowcellTransferEntity = labEventFactory.buildVesselToSectionDbFree(flowcellTransferJaxb,
                    dilutionRack.getContainerRole().getContainedVessels().iterator().next(), null,
                    SBSSection.ALL2.getSectionName());
            labEventHandler.processEvent(flowcellTransferEntity);
            //asserts
            illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();

            break;
        case DENATURE_TO_FLOWCELL:
            LabEventTest.validateWorkflow("DenatureToFlowcellTransfer", denatureRack);
            flowcellTransferEntity = labEventFactory.buildVesselToSectionDbFree(flowcellTransferJaxb,
                    denatureRack.getContainerRole().getContainedVessels().iterator().next(), null,
                    SBSSection.ALL2.getSectionName());
            labEventHandler.processEvent(flowcellTransferEntity);
            illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
            break;
        case STRIPTUBE_TO_FLOWCELL:
            Map<String, TwoDBarcodedTube> mapBarcodeToDenatureTube = new HashMap<>();

            LabEventTest.validateWorkflow("StripTubeBTransfer", denatureRack);

            Map<String, StripTube> mapBarcodeToStripTube = new HashMap<>();
            int catchSampleInstanceCount = denatureRack.getSampleInstances().size();
            LabEvent stripTubeTransferEntity =
                    labEventFactory.buildCherryPickRackToStripTubeDbFree(stripTubeTransferJaxb,
                            new HashMap<String, TubeFormation>() {{
                                put(denatureRack.getLabel(), denatureRack);
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
            LabEvent stbFlowcellTransferEntity =
                    labEventFactory.buildFromBettaLimsPlateToPlateDbFree(stbFlowcellTransferJaxb,
                            stripTube, null);
            labEventHandler.processEvent(stbFlowcellTransferEntity);
            //asserts
            illuminaFlowcell = (IlluminaFlowcell) stbFlowcellTransferEntity.getTargetLabVessels().iterator().next();
            break;
        }


        Set<SampleInstance> lane1SampleInstances =
                illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                        VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.size(), denatureRack.getSampleInstances().size(),
                "Wrong number of samples in flowcell lane");

        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(),
                (!"Whole Genome".equals(workflowName)) ? 2 : 1,
                "Wrong number of reagents");

        Set<SampleInstance> lane2SampleInstances =
                illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                        VesselPosition.LANE2);
        Assert.assertEquals(lane2SampleInstances.size(), denatureRack.getSampleInstances().size(),
                "Wrong number of samples in flowcell lane");

        Assert.assertEquals(lane2SampleInstances.iterator().next().getReagents().size(),
                (!"Whole Genome".equals(workflowName)) ? 2 : 1,
                "Wrong number of reagents");

        LabEventTest.validateWorkflow("FlowcellLoaded", illuminaFlowcell);

        LabEvent flowcellLoadEntity = labEventFactory
                .buildReceptacleEventDbFree(flowcellLoadJaxb, illuminaFlowcell);
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
