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

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.VesselTransferEjb;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class MiSeqReagentKitJaxbBuilder {

    private final Map<String, VesselPosition> denatureBarcodeMap;
    private final String miSeqReagentKitBarcode;
    private BettaLIMSMessage denatureToReagentKitJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

    public MiSeqReagentKitJaxbBuilder(Map<String, VesselPosition> denatureBarcodeMap,
                                      String miSeqReagentKitBarcode, String flowcellBarcode) {
        this.denatureBarcodeMap = denatureBarcodeMap;
        this.miSeqReagentKitBarcode = miSeqReagentKitBarcode;
    }

    public MiSeqReagentKitJaxbBuilder invoke() {
        VesselTransferEjb vesselTransferEjb = new VesselTransferEjb();
        String username = "pdunlea";
        String stationName = "ZAN";
        denatureToReagentKitJaxb = vesselTransferEjb
                .denatureToReagentKitTransfer(null, denatureBarcodeMap, miSeqReagentKitBarcode, username, stationName);

        messageList.add(denatureToReagentKitJaxb);
        Assert.assertNotNull(denatureToReagentKitJaxb);
        Assert.assertNotNull(denatureToReagentKitJaxb.getPlateCherryPickEvent());
        PlateCherryPickEvent plateCherryPickEvent = denatureToReagentKitJaxb.getPlateCherryPickEvent().get(0);
        Assert.assertEquals(plateCherryPickEvent.getPlate().size(), 1);
        Assert.assertEquals(getMessageList().size(), 1);
        return this;
    }

    public BettaLIMSMessage getDenatureToReagentKitJaxb() {
        return denatureToReagentKitJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
