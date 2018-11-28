package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Builds JAXB objects for Extractions cryovial blood messages.  This is used in GPUI tests.
 */
@SuppressWarnings("unused")
public class ExtractionsBloodJaxbBuilder {
    private static final int NUMBER_OF_RACK_COLUMNS = 32;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> bloodCryovialTubeBarcodes;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private final Random random = new Random(System.currentTimeMillis());
    private PlateCherryPickEvent bloodCryovialTransfer;
    private PlateTransferEventType extractionsBloodDeepwellToChemagen;
    private PlateTransferEventType extractionsBloodChemagenToFinalRack;
    private String finalRackBarcode;
    private List<String> bloodFinalTubeBarcodes;
    private String preChemagenDeepwellBarcode;
    private String postChemagenDeepwellBarcode;

    public final String DEEPWELL96 = "Plate96RoundWellBlock2000";

    public ExtractionsBloodJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            List<String> bloodCryovialTubeBarcodes, String preChemagenDeepWellBarcode,
            String postChemagenDeepWellBarcode, String finalRackBarcode,
            List<String> finalRackTubeBarcodes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.bloodCryovialTubeBarcodes = bloodCryovialTubeBarcodes;
        this.preChemagenDeepwellBarcode = preChemagenDeepWellBarcode;
        this.postChemagenDeepwellBarcode = postChemagenDeepWellBarcode;
        this.finalRackBarcode = finalRackBarcode;
        bloodFinalTubeBarcodes = finalRackTubeBarcodes;
    }

    public ExtractionsBloodJaxbBuilder invoke() {

        // BloodCryovialExtraction is a cherry pick from Hamilton Carrier to Deepwell plate.
        // Dummy barcode is only needed for internal message element cross referencing.
        String hamiltonCarrierBarcode = "dummy";
        PositionMapType destinationPositionMapType = new PositionMapType();
        destinationPositionMapType.setBarcode(preChemagenDeepwellBarcode);
        List<BettaLimsMessageTestFactory.CherryPick> bloodCherryPicks = new ArrayList<>();
        for (int i = 1; i <= bloodCryovialTubeBarcodes.size(); i++) {
            String sourceWell = bettaLimsMessageTestFactory.buildWellName(NUMBER_OF_RACK_COLUMNS, i,
                    BettaLimsMessageTestFactory.WellNameType.LONG);

            String destinationWell = bettaLimsMessageTestFactory.buildWellName(i,
                    BettaLimsMessageTestFactory.WellNameType.LONG);
            bloodCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(hamiltonCarrierBarcode, sourceWell,
                    preChemagenDeepwellBarcode, destinationWell));

            ReceptacleType destinationReceptacle = new ReceptacleType();
            destinationReceptacle.setReceptacleType("Well2000");
            destinationReceptacle.setPosition(destinationWell);
            destinationPositionMapType.getReceptacle().add(destinationReceptacle);
        }
        bloodCryovialTransfer = bettaLimsMessageTestFactory.buildCherryPickToPlate("BloodCryovialExtraction",
                "HamiltonSampleCarrier32",
                Collections.singletonList(hamiltonCarrierBarcode), Collections.singletonList(bloodCryovialTubeBarcodes),
                Collections.singletonList(preChemagenDeepwellBarcode), bloodCherryPicks);

        bloodCryovialTransfer.getPositionMap().add(destinationPositionMapType);

        // Sets the plate type to deepwell and fixes up the source positionMap to be 1xN.
        bloodCryovialTransfer.getPlate().get(0).setPhysType(DEEPWELL96);
        remapPositionsForSampleCarrier(bloodCryovialTransfer.getSourcePositionMap().get(0).getReceptacle());
        bettaLimsMessageTestFactory.addMessage(messageList, bloodCryovialTransfer);


        // ExtractionsBloodDeepwellToChemagen is a transfer from deepwell96 to another deepwell96.
        extractionsBloodDeepwellToChemagen = bettaLimsMessageTestFactory.buildPlateToPlate(
                "ExtractionsBloodDeepwellToChemagen",
                preChemagenDeepwellBarcode, postChemagenDeepwellBarcode);
        extractionsBloodDeepwellToChemagen.getSourcePlate().setPhysType(DEEPWELL96);
        extractionsBloodDeepwellToChemagen.getPlate().setPhysType(DEEPWELL96);
        bettaLimsMessageTestFactory.addMessage(messageList, extractionsBloodDeepwellToChemagen);


        // ExtractionsBloodChemagenToFinalRack is a deepwell96 to matrix rack transfer.
        extractionsBloodChemagenToFinalRack = bettaLimsMessageTestFactory.buildPlateToRack(
                "ExtractionsBloodChemagenToFinalRack", postChemagenDeepwellBarcode,
                finalRackBarcode, bloodFinalTubeBarcodes);
        extractionsBloodChemagenToFinalRack.getSourcePlate().setPhysType(DEEPWELL96);
        for (ReceptacleType receptacleType : extractionsBloodChemagenToFinalRack.getPositionMap().getReceptacle()) {
            receptacleType.setMaterialType("DNA:DNA Genomic");
        }
        bettaLimsMessageTestFactory.addMessage(messageList, extractionsBloodChemagenToFinalRack);

        return this;
    }

    public List<String> getBloodCryovialTubeBarcodes() {
        return bloodCryovialTubeBarcodes;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getPreChemagenDeepwellBarcode() {
        return preChemagenDeepwellBarcode;
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

    public String getFinalRackBarcode() {
        return finalRackBarcode;
    }

    public List<String> getBloodFinalTubeBarcodes() {
        return bloodFinalTubeBarcodes;
    }

    public PlateTransferEventType getExtractionsBloodChemagenToFinalRack() {
        return extractionsBloodChemagenToFinalRack;
    }

    /**
     *  Converts CherryPick builder's 8x12 plate cellnames to a 1xN sampleCarrier names, e.g. "B1" becomes "A13".
     */
    public static void remapPositionsForSampleCarrier(List<ReceptacleType> receptacle) {
        for (ReceptacleType receptacleType : receptacle) {
            int rowNumber = "ABCDEFGH".indexOf(receptacleType.getPosition().substring(0, 1));
            int column = Integer.parseInt(receptacleType.getPosition().substring(1));
            int targetColumn = column + 12 * rowNumber;
            receptacleType.setPosition("A" + targetColumn);
        }
    }
}
