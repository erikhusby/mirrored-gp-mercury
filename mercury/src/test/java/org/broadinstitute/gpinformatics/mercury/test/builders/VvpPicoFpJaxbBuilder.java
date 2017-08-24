package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB DTOs for VVP protocol that does transfers for Pico and fingerprinting.
 */
public class VvpPicoFpJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final List<String> tubeBarcodeList;
    private final String testPrefix;

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateEventType volumeMeasurement;
    private PlateEventType volumeMeasurementAdd;
    private PlateTransferEventType picoDilutionTransfer;
// ?   private PlateTransferEventType picoMicroflourTransfer;
// ?   private PlateEventType picoBufferAddition;
    private PlateTransferEventType fingerprintingAliquot;
    private PlateTransferEventType fingerprintingPlateSetup;

    public VvpPicoFpJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, List<String> tubeBarcodeList,
            String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.tubeBarcodeList = tubeBarcodeList;
        this.testPrefix = testPrefix;
    }

    public VvpPicoFpJaxbBuilder invoke() {
        String rackBarcode = testPrefix + "1";
        volumeMeasurement = bettaLimsMessageTestFactory.buildPlateEvent("VolumeMeasurement", rackBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, volumeMeasurement);

        volumeMeasurementAdd = bettaLimsMessageTestFactory.buildPlateEvent("VolumeMeasurement", rackBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, volumeMeasurementAdd);

        String picoPlateBarcode = testPrefix + "2";
        picoDilutionTransfer = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer", rackBarcode,
                tubeBarcodeList, picoPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoDilutionTransfer);

        String fpRackBarcode = testPrefix + "3";
        List<String> fpTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < tubeBarcodeList.size(); i++) {
            fpTubeBarcodes.add(testPrefix + "FP" + i);
        }
        fingerprintingAliquot = bettaLimsMessageTestFactory.buildPlateToRack("FingerprintingAliquot", picoPlateBarcode,
                fpRackBarcode, fpTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingAliquot);

        String fpPlateBarcode = testPrefix + "4";
        fingerprintingPlateSetup = bettaLimsMessageTestFactory.buildRackToPlate("FingerprintingPlateSetup",
                fpRackBarcode, fpTubeBarcodes, fpPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingPlateSetup);

        return this;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateEventType getVolumeMeasurement() {
        return volumeMeasurement;
    }

    public PlateEventType getVolumeMeasurementAdd() {
        return volumeMeasurementAdd;
    }

    public PlateTransferEventType getPicoDilutionTransfer() {
        return picoDilutionTransfer;
    }

    public PlateTransferEventType getFingerprintingAliquot() {
        return fingerprintingAliquot;
    }

    public PlateTransferEventType getFingerprintingPlateSetup() {
        return fingerprintingPlateSetup;
    }
}
