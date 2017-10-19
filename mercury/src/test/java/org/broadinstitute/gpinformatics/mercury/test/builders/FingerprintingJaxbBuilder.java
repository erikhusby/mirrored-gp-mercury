package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for Fingerprinting messages.
 */
public class FingerprintingJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String sourcePlate;
    private final int numSamples;
    private final List<Triple<String, String, Integer>> staReagents;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    private PlateEventType fingerprintingSTAAdditionJaxb;
    private PlateEventType fingerprintingPostSTAAdditionThermoCyclerJaxb;
    private PlateTransferEventType fingerprintingSlrDilution;
    private PlateTransferEventType fingerprintingIFCTransfer;
    private PlateEventType fingerprintingFC1Loaded;
    private String slrDilutionPlate;
    private String ifcChip;

    public FingerprintingJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                     String sourcePlate, int numSamples,
                                     List<Triple<String, String, Integer>> staReagents) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.sourcePlate = sourcePlate;
        this.numSamples = numSamples;
        this.staReagents = staReagents;
    }

    public FingerprintingJaxbBuilder invoke() {
        fingerprintingSTAAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "FingerprintingSTAAddition", sourcePlate,
                BettaLimsMessageTestFactory.reagentList(staReagents));
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingSTAAdditionJaxb);

        fingerprintingPostSTAAdditionThermoCyclerJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "FingerprintingPostSTAAdditionThermoCyclerLoaded", sourcePlate);
        fingerprintingPostSTAAdditionThermoCyclerJaxb.setStation("Thermo1");
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingPostSTAAdditionThermoCyclerJaxb);

        slrDilutionPlate = testPrefix + "SlrDilutionPlate";
        fingerprintingSlrDilution = bettaLimsMessageTestFactory.buildPlateToPlate("FingerprintingSLRDilution",
                sourcePlate, slrDilutionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingSlrDilution);

        ifcChip = testPrefix + "IfcChip";
        fingerprintingIFCTransfer = bettaLimsMessageTestFactory.buildPlateToPlate("FingerprintingIFCTransfer",
                slrDilutionPlate, ifcChip);
        fingerprintingIFCTransfer.getPlate().setPhysType("Fluidigm96.96AccessArrayIFC");
        fingerprintingIFCTransfer.getPlate().setSection("P384COLS7-12BYROW");
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingIFCTransfer);

        fingerprintingFC1Loaded = bettaLimsMessageTestFactory.buildPlateEvent(
                "FingerprintingFC1Loaded", ifcChip);
        fingerprintingFC1Loaded.setStation("FC1A");
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingFC1Loaded);

        return this;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateEventType getFingerprintingSTAAdditionJaxb() {
        return fingerprintingSTAAdditionJaxb;
    }

    public PlateEventType getFingerprintingPostSTAAdditionThermoCyclerJaxb() {
        return fingerprintingPostSTAAdditionThermoCyclerJaxb;
    }

    public PlateTransferEventType getFingerprintingSlrDilution() {
        return fingerprintingSlrDilution;
    }

    public PlateTransferEventType getFingerprintingIFCTransfer() {
        return fingerprintingIFCTransfer;
    }

    public PlateEventType getFingerprintingFC1Loaded() {
        return fingerprintingFC1Loaded;
    }

    public String getSlrDilutionPlate() {
        return slrDilutionPlate;
    }

    public String getIfcChip() {
        return ifcChip;
    }

    public String getSourcePlate() {
        return sourcePlate;
    }
}
