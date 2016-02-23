package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.DenatureToDilutionTubeHandler;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Builds JAXB objects for HiSeq 4000 Denature, StripTube, and Flowcell messages
 */
public class HiSeq4000JaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> denatureTubeBarcodes;
    private final Map<String, String> denatureToPosition;
    private final String denatureRackBarcode;
    private final LabBatch fctTicket;
    private final int sampleInstanceCount;
    private final String designationName;
    private final int flowcellLanes;
    private String flowcellBarcode;
    private String stripTubeHolderBarcode;
    private String stripTubeBarcode;
    private PlateCherryPickEvent stripTubeTransferJaxb;
    private MetadataType dilutionMetadata;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateTransferEventType stbFlowcellTransferJaxb;
    private ReceptacleEventType flowcellLoad;

    public HiSeq4000JaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                String flowcellBarcode, List<String> denatureTubeBarcodes,
                                Map<String, String> denatureToPosition, String denatureRackBarcode,
                                LabBatch fctTicket,
                                int sampleInstanceCount,
                                String designationName, int flowcellLanes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.flowcellBarcode = flowcellBarcode;
        this.denatureTubeBarcodes = denatureTubeBarcodes;
        this.denatureToPosition = denatureToPosition;
        this.denatureRackBarcode = denatureRackBarcode;
        this.fctTicket = fctTicket;
        this.sampleInstanceCount = sampleInstanceCount;
        this.designationName = designationName;
        this.flowcellLanes = flowcellLanes;
    }

    public HiSeq4000JaxbBuilder invoke() {
        stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
        List<BettaLimsMessageTestFactory.CherryPick> stripTubeCherryPicks = new ArrayList<>();
        for (LabBatchStartingVessel startingVessel : fctTicket.getLabBatchStartingVessels()) {
            LabVessel denatureLabVessel = startingVessel.getLabVessel();
            String lane = startingVessel.getVesselPosition().name();
            int laneNum = Integer.parseInt(lane.substring(lane.length() - 1));
            char laneRow = (char) (laneNum + 64); //Convert 1 to A
            String destWell = laneRow + "01";
            stripTubeCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(
                    denatureRackBarcode, denatureToPosition.get(denatureLabVessel.getLabel()),
                    stripTubeHolderBarcode, destWell
            ));
        }

        stripTubeBarcode = "StripTube" + testPrefix + "1";

        stripTubeTransferJaxb = bettaLimsMessageTestFactory.buildCherryPickToStripTube("StripTubeBTransfer",
                Collections.singletonList(denatureRackBarcode),
                Collections.singletonList(denatureTubeBarcodes),
                stripTubeHolderBarcode,
                Collections.singletonList(stripTubeBarcode),
                stripTubeCherryPicks);
        dilutionMetadata = new MetadataType();
        dilutionMetadata.setName(DenatureToDilutionTubeHandler.FCT_METADATA_NAME);
        dilutionMetadata.setValue(fctTicket.getBusinessKey());
        stripTubeTransferJaxb.getPositionMap().iterator().next().getReceptacle().iterator().next().getMetadata().
                add(dilutionMetadata);

        bettaLimsMessageTestFactory.addMessage(messageList, stripTubeTransferJaxb);

        // FlowcellTransfer
        stbFlowcellTransferJaxb = bettaLimsMessageTestFactory.buildStripTubeToFlowcell("FlowcellTransfer",
                stripTubeBarcode, flowcellBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, stbFlowcellTransferJaxb);

        flowcellLoad = bettaLimsMessageTestFactory.buildReceptacleEvent("FlowcellLoaded",
                flowcellBarcode, LabEventFactory.PHYS_TYPE_FLOWCELL);
        flowcellLoad.setStation(BettaLimsMessageTestFactory.HISEQ_SEQUENCING_STATION_MACHINE_NAME);
        bettaLimsMessageTestFactory.addMessage(messageList, flowcellLoad);

        return this;
    }

    public BettaLimsMessageTestFactory getBettaLimsMessageTestFactory() {
        return bettaLimsMessageTestFactory;
    }

    public String getTestPrefix() {
        return testPrefix;
    }

    public List<String> getDenatureTubeBarcodes() {
        return denatureTubeBarcodes;
    }

    public Map<String, String> getDenatureToPosition() {
        return denatureToPosition;
    }

    public String getDenatureRackBarcode() {
        return denatureRackBarcode;
    }

    public LabBatch getFctTicket() {
        return fctTicket;
    }

    public int getSampleInstanceCount() {
        return sampleInstanceCount;
    }

    public String getDesignationName() {
        return designationName;
    }

    public int getFlowcellLanes() {
        return flowcellLanes;
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public String getStripTubeHolderBarcode() {
        return stripTubeHolderBarcode;
    }

    public String getStripTubeBarcode() {
        return stripTubeBarcode;
    }

    public PlateCherryPickEvent getStripTubeTransferJaxb() {
        return stripTubeTransferJaxb;
    }

    public MetadataType getDilutionMetadata() {
        return dilutionMetadata;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateTransferEventType getStbFlowcellTransferJaxb() {
        return stbFlowcellTransferJaxb;
    }

    public ReceptacleEventType getFlowcellLoad() {
        return flowcellLoad;
    }
}
