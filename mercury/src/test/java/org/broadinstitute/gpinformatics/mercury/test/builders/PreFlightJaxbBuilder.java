package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for Pre-flight messages
 */
public class PreFlightJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String>                tubeBarcodes;

    private String rackBarcode;
    private PlateTransferEventType preflightPicoSetup1;
    private PlateTransferEventType preflightPicoSetup2;
    private PlateEventType         preflightNormalization;
    private PlateTransferEventType preflightPostNormPicoSetup1;
    private PlateTransferEventType preflightPostNormPicoSetup2;

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    public PreFlightJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                List<String> tubeBarcodes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.tubeBarcodes = tubeBarcodes;
    }

    public PlateTransferEventType getPreflightPicoSetup1() {
        return preflightPicoSetup1;
    }

    public PlateTransferEventType getPreflightPicoSetup2() {
        return preflightPicoSetup2;
    }

    public PlateEventType getPreflightNormalization() {
        return preflightNormalization;
    }

    public PlateTransferEventType getPreflightPostNormPicoSetup1() {
        return preflightPostNormPicoSetup1;
    }

    public PlateTransferEventType getPreflightPostNormPicoSetup2() {
        return preflightPostNormPicoSetup2;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public PreFlightJaxbBuilder invoke() {
        rackBarcode = "PreflightRack" + testPrefix;
        preflightPicoSetup1 = bettaLimsMessageTestFactory.buildRackToPlate("PreflightPicoSetup", rackBarcode,
                tubeBarcodes,
                "PreflightPicoPlate1" + testPrefix);
        bettaLimsMessageTestFactory.addMessage(messageList, preflightPicoSetup1);

        preflightPicoSetup2 = bettaLimsMessageTestFactory.buildRackToPlate("PreflightPicoSetup", rackBarcode,
                tubeBarcodes,
                "PreflightPicoPlate2" + testPrefix);
        bettaLimsMessageTestFactory.addMessage(messageList, preflightPicoSetup2);

        preflightNormalization = bettaLimsMessageTestFactory.buildRackEvent("PreflightNormalization", rackBarcode,
                tubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, preflightNormalization);

        preflightPostNormPicoSetup1 = bettaLimsMessageTestFactory.buildRackToPlate("PreflightPostNormPicoSetup",
                rackBarcode, tubeBarcodes,
                "PreflightPostNormPicoPlate1" + testPrefix);
        bettaLimsMessageTestFactory.addMessage(messageList, preflightPostNormPicoSetup1);

        preflightPostNormPicoSetup2 = bettaLimsMessageTestFactory.buildRackToPlate("PreflightPostNormPicoSetup",
                rackBarcode, tubeBarcodes,
                "PreflightPostNormPicoPlate2" + testPrefix);
        bettaLimsMessageTestFactory.addMessage(messageList, preflightPostNormPicoSetup2);

        return this;
    }
}
