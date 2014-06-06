package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * User: jowalsh
 * Date: 6/2/14
 */
public class KapaQCJaxbBuilder {
    private final List<String> sourceBarcodes;
    private final String sourceRackBarcode;
    private final String testPrefix;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private String dilutionPlateBarcode;
    private String kapaQCSetupPlate;

    private PlateTransferEventType kapaQCDilutionPlateJaxb;
    private PlateTransferEventType kapaQCSetup48EventA1Jaxb;
    private PlateTransferEventType kapaQCSetup48EventA2Jaxb;
    private PlateTransferEventType kapaQCSetup48EventA13Jaxb;
    private PlateTransferEventType kapaQCSetup48EventA14Jaxb;

    private PlateTransferEventType kapaQCSetup96EventA1Jaxb;
    private PlateTransferEventType kapaQCSetup96EventA2Jaxb;

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    public KapaQCJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, List<String> sourceBarcodes,
                             String sourceRackBarcode, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.sourceBarcodes = sourceBarcodes;
        this.sourceRackBarcode = sourceRackBarcode;
        this.testPrefix = testPrefix;
    }

    public KapaQCJaxbBuilder invoke(boolean do48Sample) {
        dilutionPlateBarcode = "kapaQCDilutionPlate" + testPrefix;
        kapaQCDilutionPlateJaxb = bettaLimsMessageTestFactory.buildRackToPlate("KapaQCDilutionPlateTransfer",
                sourceRackBarcode,
                sourceBarcodes, dilutionPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, kapaQCDilutionPlateJaxb);

        if(do48Sample) {
            // 4 x kapaQCSetupPlatefor 48 sample transfer
            kapaQCSetupPlate = "kapaQC48SetupPlate" + testPrefix;
            kapaQCSetup48EventA1Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "KapaQCPlateSetup", dilutionPlateBarcode, kapaQCSetupPlate, "P96COLS1-6BYROW", "P384_48TIP_1INTERVAL_A1");
            kapaQCSetup48EventA2Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "KapaQCPlateSetup", dilutionPlateBarcode, kapaQCSetupPlate, "P96COLS1-6BYROW", "P384_48TIP_1INTERVAL_A2");
            kapaQCSetup48EventA13Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "KapaQCPlateSetup", dilutionPlateBarcode, kapaQCSetupPlate, "P96COLS1-6BYROW", "P384_48TIP_1_INTERVAL_A13");
            kapaQCSetup48EventA14Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "KapaQCPlateSetup", dilutionPlateBarcode, kapaQCSetupPlate, "P96COLS1-6BYROW", "P384_48TIP_1_INTERVAL_A14");

            bettaLimsMessageTestFactory.addMessage(messageList, kapaQCSetup48EventA1Jaxb, kapaQCSetup48EventA2Jaxb,
                    kapaQCSetup48EventA13Jaxb, kapaQCSetup48EventA14Jaxb);
        } else {
            // 2 x KapaQCSetupPlate for 96
            kapaQCSetupPlate = "kapaQC96SetupPlate" + testPrefix;
            kapaQCSetup96EventA1Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "KapaQCPlateSetup", dilutionPlateBarcode, kapaQCSetupPlate, "ALL96", "P384_96TIP_1INTERVAL_A1");

            kapaQCSetup96EventA2Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "KapaQCPlateSetup", dilutionPlateBarcode, kapaQCSetupPlate, "ALL96", "P384_96TIP_1INTERVAL_A2");

            bettaLimsMessageTestFactory.addMessage(messageList, kapaQCSetup96EventA1Jaxb, kapaQCSetup96EventA2Jaxb);
        }

        return this;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateTransferEventType getKapaQCDilutionPlateJaxb() {
        return kapaQCDilutionPlateJaxb;
    }

    public PlateTransferEventType getKapaQCSetup48EventA1Jaxb() {
        return kapaQCSetup48EventA1Jaxb;
    }

    public PlateTransferEventType getKapaQCSetup48EventA2Jaxb() {
        return kapaQCSetup48EventA2Jaxb;
    }

    public PlateTransferEventType getKapaQCSetup48EventA13Jaxb() {
        return kapaQCSetup48EventA13Jaxb;
    }

    public PlateTransferEventType getKapaQCSetup48EventA14Jaxb() {
        return kapaQCSetup48EventA14Jaxb;
    }

    public PlateTransferEventType getKapaQCSetup96EventA1Jaxb() {
        return kapaQCSetup96EventA1Jaxb;
    }

    public PlateTransferEventType getKapaQCSetup96EventA2Jaxb() {
        return kapaQCSetup96EventA2Jaxb;
    }
}