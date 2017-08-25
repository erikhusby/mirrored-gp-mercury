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
    private String rackBarcode;
    private final List<String> tubeBarcodeList;
    private final String testPrefix;

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateEventType volumeMeasurement;
    private PlateEventType volumeMeasurementAdd;
    private PlateTransferEventType picoDilutionTransfer;
// ?   private PlateTransferEventType picoMicroflourTransfer; or PicoTransfer?
// ?   private PlateEventType picoBufferAddition;
    private PlateTransferEventType fingerprintingAliquot;
    private PlateTransferEventType fingerprintingPlateSetup;
    private PlateTransferEventType picoTransfer1;
    private PlateTransferEventType picoTransfer2;
    private String picoPlateBarcode1;
    private String picoPlateBarcode2;

    public VvpPicoFpJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String rackBarcode,
            List<String> tubeBarcodeList, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.rackBarcode = rackBarcode;
        this.tubeBarcodeList = tubeBarcodeList;
        this.testPrefix = testPrefix;
    }

    public VvpPicoFpJaxbBuilder invoke() {
        volumeMeasurement = bettaLimsMessageTestFactory.buildRackEvent("VolumeMeasurement", rackBarcode,
                tubeBarcodeList);
        bettaLimsMessageTestFactory.addMessage(messageList, volumeMeasurement);

        volumeMeasurementAdd = bettaLimsMessageTestFactory.buildRackEvent("VolumeMeasurement", rackBarcode,
                tubeBarcodeList);
        bettaLimsMessageTestFactory.addMessage(messageList, volumeMeasurementAdd);

        String picoDilutionPlateBarcode = testPrefix + "PD";
        picoDilutionTransfer = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransferForwardBsp",
                rackBarcode, tubeBarcodeList, picoDilutionPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoDilutionTransfer);

        List<String> fpTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < tubeBarcodeList.size(); i++) {
            fpTubeBarcodes.add(testPrefix + "FP" + i);
        }
        String fpRackBarcode = testPrefix + "FPR";
        fingerprintingAliquot = bettaLimsMessageTestFactory.buildPlateToRack("FingerprintingAliquot",
                picoDilutionPlateBarcode, fpRackBarcode, fpTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingAliquot);

        String fpPlateBarcode = testPrefix + "FPS";
        fingerprintingPlateSetup = bettaLimsMessageTestFactory.buildRackToPlate("FingerprintingPlateSetup",
                fpRackBarcode, fpTubeBarcodes, fpPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingPlateSetup);

        picoPlateBarcode1 = testPrefix + "1";
        picoTransfer1 = bettaLimsMessageTestFactory.buildPlateToPlate("PicoTransfer", picoDilutionPlateBarcode,
                picoPlateBarcode1);
        bettaLimsMessageTestFactory.addMessage(messageList, picoTransfer1);

        picoPlateBarcode2 = testPrefix + "2";
        picoTransfer2 = bettaLimsMessageTestFactory.buildPlateToPlate("PicoTransfer", picoDilutionPlateBarcode,
                picoPlateBarcode2);
        bettaLimsMessageTestFactory.addMessage(messageList, picoTransfer2);

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

    public String getPicoPlateBarcode1() {
        return picoPlateBarcode1;
    }

    public String getPicoPlateBarcode2() {
        return picoPlateBarcode2;
    }
}
