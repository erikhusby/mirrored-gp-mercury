package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds JAXB objects for Extractions cryovial blood messages
 */
public class ExtractionsBloodJaxbBuilder {
    private static final int NUMBER_OF_RACK_COLUMNS = 32;

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> bloodCryovialTubeBarcodes;
    private final String hamiltonCarrierBarcode;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    private String bloodPreChemagenDeepwellBarcode;
    private PlateCherryPickEvent bloodCryovialTransfer;
    private String postChemagenDeepwellBarcode;
    private PlateTransferEventType extractionsBloodDeepwellToChemagen;
    private String bloodFinalRackBarcode;
    private ArrayList<String> bloodFinalTubeBarcodes;
    private PlateTransferEventType extractionsBloodChemagenToFinalRack;

    public ExtractionsBloodJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            List<String> bloodCryovialTubeBarcodes, String hamiltonCarrierBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.bloodCryovialTubeBarcodes = bloodCryovialTubeBarcodes;
        this.hamiltonCarrierBarcode = hamiltonCarrierBarcode;
    }

    public ExtractionsBloodJaxbBuilder invoke() {

        //BloodCryovialExtraction, cherry pick from Hamilton Carrier to Deepwell plate
        List<BettaLimsMessageTestFactory.CherryPick> bloodCherryPicks = new ArrayList<>();
        bloodPreChemagenDeepwellBarcode = testPrefix + "PreChemagenBloodDeepWell";
        for (int i = 1; i <= bloodCryovialTubeBarcodes.size(); i++) {
            String sourceWell = bettaLimsMessageTestFactory.buildWellName(NUMBER_OF_RACK_COLUMNS, i,
                    BettaLimsMessageTestFactory.WellNameType.LONG);

            String destinationWell = bettaLimsMessageTestFactory.buildWellName(i,
                    BettaLimsMessageTestFactory.WellNameType.LONG);
            bloodCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(hamiltonCarrierBarcode, sourceWell,
                    bloodPreChemagenDeepwellBarcode, destinationWell));
        }
        bloodCryovialTransfer = bettaLimsMessageTestFactory.buildCherryPickToPlate("BloodCryovialExtraction",
                "HamiltonSampleCarrier32",
                Collections.singletonList(hamiltonCarrierBarcode), Collections.singletonList(bloodCryovialTubeBarcodes),
                Collections.singletonList(bloodPreChemagenDeepwellBarcode), bloodCherryPicks);
        bettaLimsMessageTestFactory.addMessage(messageList, bloodCryovialTransfer);

        //ExtractionsBloodDeepwellToChemagen
        postChemagenDeepwellBarcode = testPrefix + "PostChemagenDeepWell";
        extractionsBloodDeepwellToChemagen = bettaLimsMessageTestFactory.buildPlateToPlate("ExtractionsBloodDeepwellToChemagen",
                bloodPreChemagenDeepwellBarcode, postChemagenDeepwellBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, extractionsBloodDeepwellToChemagen);

        //ExtractionsBloodChemagenToFinalRack
        bloodFinalRackBarcode = testPrefix + "FinalRackBarcode";
        bloodFinalTubeBarcodes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= bloodCryovialTubeBarcodes.size(); rackPosition++) {
            bloodFinalTubeBarcodes.add("BloodFinalRack" + testPrefix + rackPosition);
        }
        extractionsBloodChemagenToFinalRack = bettaLimsMessageTestFactory.buildPlateToRack("ExtractionsBloodChemagenToFinalRack",
                postChemagenDeepwellBarcode, bloodFinalRackBarcode, bloodFinalTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, extractionsBloodChemagenToFinalRack);

        return this;
    }

    public List<String> getBloodCryovialTubeBarcodes() {
        return bloodCryovialTubeBarcodes;
    }

    public String getHamiltonCarrierBarcode() {
        return hamiltonCarrierBarcode;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getBloodPreChemagenDeepwellBarcode() {
        return bloodPreChemagenDeepwellBarcode;
    }

    public PlateCherryPickEvent getBloodCryovialTransfer() {
        return bloodCryovialTransfer;
    }

    public String getPostChemagenDeepwellBarcode() {
        return postChemagenDeepwellBarcode;
    }

    public PlateTransferEventType getExtractionsBloodDeepwellToChemagen() {
        return extractionsBloodDeepwellToChemagen;
    }

    public String getBloodFinalRackBarcode() {
        return bloodFinalRackBarcode;
    }

    public ArrayList<String> getBloodFinalTubeBarcodes() {
        return bloodFinalTubeBarcodes;
    }

    public PlateTransferEventType getExtractionsBloodChemagenToFinalRack() {
        return extractionsBloodChemagenToFinalRack;
    }
}
