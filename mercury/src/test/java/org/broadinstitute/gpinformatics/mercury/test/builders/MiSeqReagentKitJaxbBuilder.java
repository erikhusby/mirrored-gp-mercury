/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MiSeqReagentKitJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String denatureTubeBarcode;
    private final Map<String, VesselPosition> denatureBarcodeMap;
    private final String mySeqReagentKitBarcode;

    private BettaLIMSMessage denatureToReagentKitJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

    public MiSeqReagentKitJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                      String denatureTubeBarcode, Map<String, VesselPosition> denatureBarcodeMap, String mySeqReagentKitBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.denatureTubeBarcode = denatureTubeBarcode;
        this.denatureBarcodeMap = denatureBarcodeMap;
        this.mySeqReagentKitBarcode = mySeqReagentKitBarcode;
    }

    public MiSeqReagentKitJaxbBuilder invoke() {
        VesselTransferEjb vesselTransferEjb = new VesselTransferEjb();
        denatureToReagentKitJaxb = vesselTransferEjb
                .denatureToReagentKitTransfer(null, denatureBarcodeMap, mySeqReagentKitBarcode, "pdunlea", "UI");

        messageList.add(denatureToReagentKitJaxb);
        return this;
    }

    public BettaLIMSMessage getDenatureToReagentKitJaxb() {
        return denatureToReagentKitJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
