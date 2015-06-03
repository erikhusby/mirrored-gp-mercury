package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.DenatureToDilutionTubeHandler;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 4/3/13
 *         Time: 6:32 AM
 */
public class HiSeq2500JaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private String testPrefix;
    private final List<String> denatureTubeBarcodes;
    private String flowcellBarcode;
    private String squidDesignationName;
    private int flowcellLanes;
    private final String denatureRackBarcode;

    private String dilutionTubeBarcode;
    private String dilutionRackBarcode;
    private final String fctTicket;

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private ReceptaclePlateTransferEvent flowcellTransferJaxb;
    private PlateCherryPickEvent dilutionTransferJaxb;
    private final ProductionFlowcellPath productionFlowcellPath;

    private String stripTubeHolderBarcode;
    private PlateCherryPickEvent stripTubeTransferJaxb;

    private PlateTransferEventType stbFlowcellTransferJaxb;
    private ReceptacleEventType flowcellLoad;
    private String stripTubeBarcode;

    private final int poolSize;


    public HiSeq2500JaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                String testPrefix, List<String> denatureTubeBarcodes, String denatureRackBarcode,
                                String fctTicket, ProductionFlowcellPath productionFlowcellPath,
                                int poolSize, String designationName, int flowcellLanes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.denatureTubeBarcodes = denatureTubeBarcodes;
        this.denatureRackBarcode = denatureRackBarcode;
        this.fctTicket = fctTicket;
        this.productionFlowcellPath = productionFlowcellPath;
        this.poolSize = poolSize;
        squidDesignationName = designationName;
        this.flowcellLanes = flowcellLanes;
    }

    public HiSeq2500JaxbBuilder invoke() {

        dilutionRackBarcode = "DilutionRack" + testPrefix;
        dilutionTubeBarcode = "DilutionTube" + testPrefix;
        flowcellBarcode = "Flowcell" + testPrefix;

        switch (productionFlowcellPath) {

        case DILUTION_TO_FLOWCELL:
            List<BettaLimsMessageTestFactory.CherryPick> dilutionCherrypicks =
                    Collections.singletonList(new BettaLimsMessageTestFactory.CherryPick(
                            denatureRackBarcode, Character.toString((char) ('A')) + "01",
                            dilutionRackBarcode, Character.toString((char) ('A')) + "01"));

            dilutionTransferJaxb = bettaLimsMessageTestFactory.buildCherryPick("DenatureToDilutionTransfer",
                    Collections.singletonList(denatureRackBarcode),
                    Collections.singletonList(denatureTubeBarcodes),
                    Collections.singletonList(dilutionRackBarcode),
                    Collections.singletonList(Collections.singletonList(dilutionTubeBarcode)), dilutionCherrypicks);
            MetadataType dilutionMetadata = new MetadataType();
            dilutionMetadata.setName(DenatureToDilutionTubeHandler.FCT_METADATA_NAME);
            dilutionMetadata.setValue(fctTicket);
            dilutionTransferJaxb.getPositionMap().iterator().next().
                    getReceptacle().iterator().next().getMetadata()
                    .add(dilutionMetadata);

            bettaLimsMessageTestFactory.addMessage(messageList, dilutionTransferJaxb);

            flowcellTransferJaxb =
                    bettaLimsMessageTestFactory.buildTubeToPlate("DilutionToFlowcellTransfer",
                            dilutionTubeBarcode, flowcellBarcode, LabEventTest.PHYS_TYPE_FLOWCELL_2_LANE,
                            LabEventTest.SECTION_ALL_2, "tube");
            bettaLimsMessageTestFactory.addMessage(messageList, flowcellTransferJaxb);
            break;
        case DENATURE_TO_FLOWCELL:

            flowcellTransferJaxb =
                    bettaLimsMessageTestFactory.buildTubeToPlate("DenatureToFlowcellTransfer",
                            denatureTubeBarcodes.get(0), flowcellBarcode, LabEventTest.PHYS_TYPE_FLOWCELL_2_LANE,
                            LabEventTest.SECTION_ALL_2, "tube");
            if (StringUtils.isNotBlank(squidDesignationName)) {
                MetadataType denatureMetaData = new MetadataType();
                denatureMetaData.setName("DesignationName");
                denatureMetaData.setValue(squidDesignationName);
                flowcellTransferJaxb.getSourceReceptacle().getMetadata().add(denatureMetaData);
            }
            bettaLimsMessageTestFactory.addMessage(messageList, flowcellTransferJaxb);
            break;
        case STRIPTUBE_TO_FLOWCELL:
            // StripTubeBTransfer
            stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
            List<BettaLimsMessageTestFactory.CherryPick> stripTubeCherryPicks = new ArrayList<>();
            int sourcePosition = 0;
            // Transfer column 1 to 8 rows, using non-empty source rows
            for (int destinationPosition = 0; destinationPosition < flowcellLanes; destinationPosition++) {
                stripTubeCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(
                        denatureRackBarcode, Character.toString((char) ('A' + 0)) + "01",
                        stripTubeHolderBarcode, Character.toString((char) ('A' + destinationPosition)) + "01"));
                if (sourcePosition + 1 < poolSize) {
                    sourcePosition++;
                }
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
            dilutionMetadata.setValue(fctTicket);
            stripTubeTransferJaxb.getPositionMap().iterator().next().getReceptacle().iterator().next().getMetadata().
                    add(dilutionMetadata);

            bettaLimsMessageTestFactory.addMessage(messageList, stripTubeTransferJaxb);

            // FlowcellTransfer
            stbFlowcellTransferJaxb = bettaLimsMessageTestFactory.buildStripTubeToFlowcell("FlowcellTransfer",
                    stripTubeBarcode, flowcellBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, stbFlowcellTransferJaxb);

            break;
        }
        flowcellLoad = bettaLimsMessageTestFactory.buildReceptacleEvent("FlowcellLoaded",
                flowcellBarcode, LabEventFactory.PHYS_TYPE_FLOWCELL);
        bettaLimsMessageTestFactory.addMessage(messageList, flowcellLoad);

        return this;
    }

    public ReceptaclePlateTransferEvent getFlowcellTransferJaxb() {
        return flowcellTransferJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public void setSquidDesignationName(String squidDesignationName) {
        this.squidDesignationName = squidDesignationName;
    }

    public PlateCherryPickEvent getDilutionJaxb() {
        return dilutionTransferJaxb;
    }

    public String getDilutionTubeBarcode() {
        return dilutionTubeBarcode;
    }

    public String getDilutionRackBarcode() {
        return dilutionRackBarcode;
    }

    public String getStripTubeHolderBarcode() {
        return stripTubeHolderBarcode;
    }

    public PlateCherryPickEvent getStripTubeTransferJaxb() {
        return stripTubeTransferJaxb;
    }

    public ReceptacleEventType getFlowcellLoad() {
        return flowcellLoad;
    }

    public String getStripTubeBarcode() {
        return stripTubeBarcode;
    }

    public PlateTransferEventType getStbFlowcellTransferJaxb() {
        return stbFlowcellTransferJaxb;
    }


}
