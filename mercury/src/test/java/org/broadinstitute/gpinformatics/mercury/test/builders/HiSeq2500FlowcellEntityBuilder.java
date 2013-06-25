package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.transfervis.TransferVisualizerFrame;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

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

    public HiSeq2500FlowcellEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                          LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                          TubeFormation denatureRack, String flowcellBarcode, String testPrefix) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.denatureRack = denatureRack;
        this.flowcellBarcode = flowcellBarcode;
        this.testPrefix = testPrefix;
    }

    public HiSeq2500FlowcellEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                          LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                          TubeFormation denatureRack, String flowcellBarcode, String testPrefix,
                                          String designationName) {

        this(bettaLimsMessageTestFactory, labEventFactory, labEventHandler, denatureRack, flowcellBarcode, testPrefix);
        this.designationName = designationName;
    }

    public HiSeq2500FlowcellEntityBuilder invoke() {
        HiSeq2500JaxbBuilder hiSeq2500JaxbBuilder = new HiSeq2500JaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                denatureRack.getContainerRole().getContainedVessels().iterator().next().getLabel());
        if (StringUtils.isNotBlank(designationName)) {
            hiSeq2500JaxbBuilder.setSquidDesignationName(designationName);
        }
        hiSeq2500JaxbBuilder.invoke();
        ReceptaclePlateTransferEvent flowcellTransferJaxb = hiSeq2500JaxbBuilder.getFlowcellTransferJaxb();

        // DenatureToFlowcellTransfer
        LabEventTest.validateWorkflow("DenatureToFlowcellTransfer", denatureRack);
        LabEvent flowcellTransferEntity = labEventFactory.buildVesselToSectionDbFree(flowcellTransferJaxb,
                denatureRack.getContainerRole().getContainedVessels().iterator().next(), null,
                SBSSection.ALL2.getSectionName());
        labEventHandler.processEvent(flowcellTransferEntity);
        //asserts
        illuminaFlowcell = (IlluminaFlowcell) flowcellTransferEntity.getTargetLabVessels().iterator().next();
        Set<SampleInstance> lane1SampleInstances =
                illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                        VesselPosition.LANE1);
        Assert.assertEquals(lane1SampleInstances.size(), denatureRack.getSampleInstances().size(),
                "Wrong number of samples in flowcell lane");
        Assert.assertEquals(lane1SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");
        Set<SampleInstance> lane2SampleInstances =
                illuminaFlowcell.getContainerRole().getSampleInstancesAtPosition(
                        VesselPosition.LANE2);
        Assert.assertEquals(lane2SampleInstances.size(), denatureRack.getSampleInstances().size(),
                "Wrong number of samples in flowcell lane");
        Assert.assertEquals(lane2SampleInstances.iterator().next().getReagents().size(), 2,
                "Wrong number of reagents");

        LabEventTest.validateWorkflow("FlowcellLoaded", illuminaFlowcell);

        ReceptacleEventType flowcellLoadJaxb =
                bettaLimsMessageTestFactory.buildReceptacleEvent("FlowcellLoaded", flowcellBarcode, "Flowcell2Lane");

        LabEvent flowcellLoadEntity = labEventFactory
                .buildReceptacleEventDbFree(flowcellLoadJaxb, illuminaFlowcell);
        labEventHandler.processEvent(flowcellLoadEntity);

        if (false) {
            TransferVisualizerFrame transferVisualizerFrame = new TransferVisualizerFrame();
            transferVisualizerFrame.renderVessel(denatureRack.getContainerRole().getVesselAtPosition(VesselPosition.A01));
            try {
                Thread.sleep(500000L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

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
}
