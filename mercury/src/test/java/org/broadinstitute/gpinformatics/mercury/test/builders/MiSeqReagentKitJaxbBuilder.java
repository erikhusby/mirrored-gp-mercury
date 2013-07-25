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
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MiSeqReagentKitJaxbBuilder {

    private final Map<String, VesselPosition> denatureBarcodeMap;
    private final String miSeqReagentKitBarcode;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private PlateCherryPickEvent denatureToReagentKitJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    public MiSeqReagentKitJaxbBuilder(Map<String, VesselPosition> denatureBarcodeMap,
                                      String miSeqReagentKitBarcode, String flowcellBarcode,
                                      BettaLimsMessageTestFactory bettaLimsMessageTestFactory) {
        this.denatureBarcodeMap = denatureBarcodeMap;
        this.miSeqReagentKitBarcode = miSeqReagentKitBarcode;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
    }

    public MiSeqReagentKitJaxbBuilder invoke() {
        VesselTransferEjb vesselTransferEjb = new VesselTransferEjb();
        String username = "pdunlea";
        String stationName = "ZAN";
        denatureToReagentKitJaxb = vesselTransferEjb
                .denatureToReagentKitTransfer(null, denatureBarcodeMap, miSeqReagentKitBarcode, username, stationName).getPlateCherryPickEvent().iterator().next();

        bettaLimsMessageTestFactory.addMessage(messageList, denatureToReagentKitJaxb);

        return this;
    }

    public PlateCherryPickEvent getDenatureToReagentKitJaxb() {
        return denatureToReagentKitJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
