package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for the Stool Extraction to TNA messages.
 */
public class StoolTNAJaxbBuilder {
    private final String sourcePlateBarcode;
    private final int numSamples;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;

    private List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateTransferEventType stoolExtractionSetupJaxb;
    private PlateTransferEventType stoolFinalTransferJaxb;
    private PlateTransferEventType stoolRnaAliquotJaxb;
    private PlateEventType rNaseAdditionJaxb;
    private PlateTransferEventType stoolDNACleanupJaxb;
    private PlateTransferEventType stoolRNACleanupJaxb;
    private List<String> rnaTubeBarcodes = new ArrayList<>();
    private List<String> dnaTubeBarcodes = new ArrayList<>();


    public StoolTNAJaxbBuilder(String sourcePlateBarcode, int numSamples,
                               BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix) {
        this.sourcePlateBarcode = sourcePlateBarcode;
        this.numSamples = numSamples;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
    }

    public StoolTNAJaxbBuilder invoke() {
        String chemagenPlate = testPrefix + "StoolTNAChemagenInputPlate";
        stoolExtractionSetupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                "StoolExtractionSetup", sourcePlateBarcode, chemagenPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, stoolExtractionSetupJaxb);

        String postChemagenTransferPlate = testPrefix + "ChemagenTransferPlate";
        stoolFinalTransferJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("StoolFinalTransfer", chemagenPlate,
                postChemagenTransferPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, stoolFinalTransferJaxb);

        //RNA Route includes another Plate to Plate Transfer
        String rnaAliquotePlate = testPrefix + "StoolRnaAliquotPlate";
        stoolRnaAliquotJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("StoolRnaAliquot", postChemagenTransferPlate,
                rnaAliquotePlate);
        bettaLimsMessageTestFactory.addMessage(messageList, stoolRnaAliquotJaxb);

        rNaseAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "StoolRNaseAddition", postChemagenTransferPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, rNaseAdditionJaxb);

        //Spri Cleanup for both RNA and DNA routes
        for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            rnaTubeBarcodes.add(testPrefix + "RNA" + rackPosition);
            dnaTubeBarcodes.add(testPrefix + "DNA" + rackPosition);
        }
        String dnaCleanupPlate = testPrefix + "StoolDNASpriCleanupPlate";
        String rnaCleanupPlate = testPrefix + "StoolRNASpriCleanupPlate";

        stoolDNACleanupJaxb = bettaLimsMessageTestFactory.buildPlateToRack(
                "StoolSPRICleanup", postChemagenTransferPlate, dnaCleanupPlate, dnaTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, stoolDNACleanupJaxb);


        stoolRNACleanupJaxb = bettaLimsMessageTestFactory.buildPlateToRack(
                "StoolSPRICleanup", rnaAliquotePlate, rnaCleanupPlate, rnaTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, stoolRNACleanupJaxb);

        return this;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateTransferEventType getStoolExtractionSetupJaxb() {
        return stoolExtractionSetupJaxb;
    }

    public PlateTransferEventType getStoolFinalTransferJaxb() {
        return stoolFinalTransferJaxb;
    }

    public PlateTransferEventType getStoolRnaAliquotJaxb() {
        return stoolRnaAliquotJaxb;
    }

    public PlateEventType getrNaseAdditionJaxb() {
        return rNaseAdditionJaxb;
    }

    public PlateTransferEventType getStoolDNACleanupJaxb() {
        return stoolDNACleanupJaxb;
    }

    public PlateTransferEventType getStoolRNACleanupJaxb() {
        return stoolRNACleanupJaxb;
    }
}
