package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
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
    private final String denatureTubeBarcode;
    private String flowcellBarcode;
    private String squidDesignationName;
    private final String denatureRackBarcode;

    public String getDilutionTubeBarcode() {
        return dilutionTubeBarcode;
    }

    public String getDilutionRackBarcode() {
        return dilutionRackBarcode;
    }

    private String dilutionTubeBarcode;
    private String dilutionRackBarcode;
    private final String fctTicket;

    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
    private ReceptaclePlateTransferEvent flowcellTransferJaxb;
    private PlateCherryPickEvent dilutionTransferJaxb;

    public HiSeq2500JaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                String testPrefix, String denatureTubeBarcode, String denatureRackBarcode,
                                String fctTicket) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.denatureTubeBarcode = denatureTubeBarcode;
        this.denatureRackBarcode = denatureRackBarcode;
        this.fctTicket = fctTicket;
    }

    public HiSeq2500JaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                String testPrefix, String denatureTubeBarcode, String designationName,
                                String denatureRackBarcode,
                                String fctTicket) {
        this(bettaLimsMessageTestFactory, testPrefix, denatureTubeBarcode, denatureRackBarcode, fctTicket);
        squidDesignationName = designationName;
    }

    public HiSeq2500JaxbBuilder invoke() {

        dilutionRackBarcode = "DilutionRack" + testPrefix;
        dilutionTubeBarcode = "DilutionTube" + testPrefix;


        List<BettaLimsMessageTestFactory.CherryPick> dilutionCherrypicks =
                Collections.singletonList(new BettaLimsMessageTestFactory.CherryPick(
                        denatureRackBarcode, Character.toString((char) ('A')) + "01",
                        dilutionRackBarcode, Character.toString((char) ('A')) + "01"));

        dilutionTransferJaxb = bettaLimsMessageTestFactory.buildCherryPick("DenatureToDilutionTransfer",
                Collections.singletonList(denatureRackBarcode),
                Collections.singletonList(Collections.singletonList(denatureTubeBarcode)),
                Collections.singletonList(dilutionRackBarcode),
                Collections.singletonList(Collections.singletonList(dilutionTubeBarcode)), dilutionCherrypicks);
        MetadataType dilutionMetadata = new MetadataType();
        dilutionMetadata.setName(DenatureToDilutionTubeHandler.FCT_METADATA_NAME);
        dilutionMetadata.setValue(fctTicket);
        dilutionTransferJaxb.getPositionMap().iterator().next().getReceptacle().iterator().next().getMetadata()
                .add(dilutionMetadata);

        bettaLimsMessageTestFactory.addMessage(messageList, dilutionTransferJaxb);

        flowcellBarcode = "Flowcell" + testPrefix;
        flowcellTransferJaxb =
                bettaLimsMessageTestFactory.buildTubeToPlate("DilutionToFlowcellTransfer",
                        dilutionTubeBarcode, flowcellBarcode, LabEventTest.PHYS_TYPE_FLOWCELL_2_LANE,
                        LabEventTest.SECTION_ALL_2, "tube");
        if (StringUtils.isNotBlank(dilutionTubeBarcode)) {
            MetadataType denatureMetaData = new MetadataType();
            denatureMetaData.setName("DesignationName");
            denatureMetaData.setValue(squidDesignationName);
            flowcellTransferJaxb.getSourceReceptacle().getMetadata().add(denatureMetaData);
        }
        bettaLimsMessageTestFactory.addMessage(messageList, flowcellTransferJaxb);

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
}
