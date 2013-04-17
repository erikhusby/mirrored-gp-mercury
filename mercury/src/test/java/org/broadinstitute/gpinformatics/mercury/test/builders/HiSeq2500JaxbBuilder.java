package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
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

    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
    private ReceptaclePlateTransferEvent flowcellTransferJaxb;

    public HiSeq2500JaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                String testPrefix, String denatureTubeBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.denatureTubeBarcode = denatureTubeBarcode;
    }

    public HiSeq2500JaxbBuilder invoke() {
        flowcellBarcode = "Flowcell" + testPrefix;
        flowcellTransferJaxb =
                bettaLimsMessageTestFactory.buildTubeToPlate("DenatureToFlowcellTransfer",
                        denatureTubeBarcode, flowcellBarcode, LabEventTest.PHYS_TYPE_FLOWCELL_2_LANE, LabEventTest.SECTION_ALL_2, "tube");
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, flowcellTransferJaxb);

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
}
