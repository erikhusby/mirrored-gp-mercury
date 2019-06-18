package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for library construction UMI messages
 */
public class LibraryConstructionCellFreeUMIJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String umiTubeBarcode;
    private final String p7IndexPlateBarcode;
    private final String p5IndexPlateBarcode;
    private final List<String> tubeBarcodeList;
    private final String rackBarcode;

    private String pondRegRackBarcode;
    private List<String> pondRegTubeBarcodes;
    private String pondPico1Barcode;
    private String pondPico2Barcode;

    private String endRepairPlateBarcode;
    private PlateTransferEventType preEndRepairTransferEventJaxb;
    private PlateTransferEventType pondRegistrationJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateEventType endRepairAbaseJaxb;
    private PlateEventType postEndRepairThermoCyclerLoadedJaxb;
    private ReceptaclePlateTransferEvent umiAdditionJaxb;
    private PlateEventType postUmiAdditionThermoCyclerLoadedJaxb;
    private PlateTransferEventType umiCleanupJaxb;
    private PlateTransferEventType p7IndexJaxb;
    private PlateTransferEventType p5IndexJaxb;
    private PlateEventType postIndexAdapterLigationThermoCyclerLoaded;

    public LibraryConstructionCellFreeUMIJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                                     String umiTubeBarcode, String p7IndexPlateBarcode,String p5IndexPlateBarcode,
                                                     List<String> tubeBarcodeList, String rackBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.umiTubeBarcode = umiTubeBarcode;
        this.p7IndexPlateBarcode = p7IndexPlateBarcode;
        this.p5IndexPlateBarcode = p5IndexPlateBarcode;
        this.tubeBarcodeList = tubeBarcodeList;
        this.rackBarcode = rackBarcode;
    }

    public LibraryConstructionCellFreeUMIJaxbBuilder invoke() {
        endRepairPlateBarcode = "EndRepairPlate" + testPrefix;
        preEndRepairTransferEventJaxb = bettaLimsMessageTestFactory.buildRackToPlate("PreEndRepairTransfer", rackBarcode,
                tubeBarcodeList, endRepairPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, preEndRepairTransferEventJaxb);

        endRepairAbaseJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EndRepair_ABase", endRepairPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, endRepairAbaseJaxb);

        postEndRepairThermoCyclerLoadedJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostEndRepairThermoCyclerLoaded", endRepairPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, postEndRepairThermoCyclerLoadedJaxb);

        umiAdditionJaxb = bettaLimsMessageTestFactory
                .buildTubeToPlate("UMIAddition", umiTubeBarcode, endRepairPlateBarcode, "Eppendorf96",
                        "ALL96", "tube");
        bettaLimsMessageTestFactory.addMessage(messageList, umiAdditionJaxb);

        postUmiAdditionThermoCyclerLoadedJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostUMIAdditionThermoCyclerLoaded", endRepairPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, postUmiAdditionThermoCyclerLoadedJaxb);

        String umiCleanupBarcode = "ligationCleanupPlate" + testPrefix;
        umiCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("UMICleanup",
                endRepairPlateBarcode,
                umiCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, umiCleanupJaxb);

        p7IndexJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("IndexedAdapterLigation",
                p7IndexPlateBarcode,
                umiCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, p7IndexJaxb);

        p5IndexJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("IndexP5PondEnrichment",
                p5IndexPlateBarcode,
                umiCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, p5IndexJaxb);

        postIndexAdapterLigationThermoCyclerLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostIndexedAdapterLigationThermoCyclerLoaded", umiCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, postIndexAdapterLigationThermoCyclerLoaded);

        int numSamples = tubeBarcodeList.size();
        pondRegRackBarcode = "PondReg" + testPrefix;
        pondRegTubeBarcodes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            pondRegTubeBarcodes.add(LabEventTest.POND_REGISTRATION_TUBE_PREFIX + testPrefix + rackPosition);
        }
        pondRegistrationJaxb = bettaLimsMessageTestFactory.buildPlateToRack("CFDnaPondRegistration", umiCleanupBarcode,
                pondRegRackBarcode, pondRegTubeBarcodes);
        for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            pondRegTubeBarcodes.add(LabEventTest.POND_REGISTRATION_TUBE_PREFIX + testPrefix + rackPosition);
        }
        for (ReceptacleType receptacleType : pondRegistrationJaxb.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(new BigDecimal("50"));
        }
        bettaLimsMessageTestFactory.addMessage(messageList, pondRegistrationJaxb);

        // Pico plate barcodes must be all numeric to be accepted by the Varioskan parser
        pondPico1Barcode = "771" + testPrefix;
        PlateTransferEventType pondPico1Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("PondPico",
                pondRegRackBarcode, pondRegTubeBarcodes, pondPico1Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, pondPico1Jaxb);
        pondPico2Barcode = "772" + testPrefix;
        PlateTransferEventType pondPico2Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("PondPico",
                pondRegRackBarcode, pondRegTubeBarcodes, pondPico2Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, pondPico2Jaxb);

        return this;
    }

    public PlateTransferEventType getPreEndRepairTransferEventJaxb() {
        return preEndRepairTransferEventJaxb;
    }

    public PlateTransferEventType getPondRegistrationJaxb() {
        return pondRegistrationJaxb;
    }

    public PlateEventType getEndRepairAbaseJaxb() {
        return endRepairAbaseJaxb;
    }

    public PlateEventType getPostEndRepairThermoCyclerLoadedJaxb() {
        return postEndRepairThermoCyclerLoadedJaxb;
    }

    public ReceptaclePlateTransferEvent getUmiAdditionJaxb() {
        return umiAdditionJaxb;
    }

    public PlateEventType getPostUmiAdditionThermoCyclerLoadedJaxb() {
        return postUmiAdditionThermoCyclerLoadedJaxb;
    }

    public PlateEventType getPostIndexAdapterLigationThermoCyclerLoaded() {
        return postIndexAdapterLigationThermoCyclerLoaded;
    }

    public PlateTransferEventType getP7IndexJaxb() {
        return p7IndexJaxb;
    }

    public PlateTransferEventType getP5IndexJaxb() {
        return p5IndexJaxb;
    }

    public PlateTransferEventType getUmiCleanupJaxb() {
        return umiCleanupJaxb;
    }

    public List<String> getPondRegTubeBarcodes() {
        return pondRegTubeBarcodes;
    }

    public String getPondRegRackBarcode() {
        return pondRegRackBarcode;
    }
}
