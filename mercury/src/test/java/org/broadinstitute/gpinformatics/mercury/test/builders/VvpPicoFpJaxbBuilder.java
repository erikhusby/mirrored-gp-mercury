package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

import java.math.BigDecimal;
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
    private PlateTransferEventType fingerprintingAliquot;
    private PlateTransferEventType fingerprintingPlateSetup; // todo auto export like ArrayPlatingDilution?
    private PlateTransferEventType picoTransfer1; // todo jmt or picoMicroflourTransfer / picoBufferAddition?
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
        for (ReceptacleType receptacleType : volumeMeasurementAdd.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(new BigDecimal(50));
        }

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
        fingerprintingAliquot = bettaLimsMessageTestFactory.buildPlateToRack("FingerprintingAliquotForwardBsp",
                picoDilutionPlateBarcode, fpRackBarcode, fpTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingAliquot);

        String fpPlateBarcode = testPrefix + "FPS";
        fingerprintingPlateSetup = bettaLimsMessageTestFactory.buildRackToPlate("FingerprintingPlateSetupForwardBsp",
                fpRackBarcode, fpTubeBarcodes, fpPlateBarcode);
        fingerprintingPlateSetup.getPlate().setPhysType("Plate96Well200PCR");
        PositionMapType destinationPositionMap = new PositionMapType();
        destinationPositionMap.setBarcode(fpPlateBarcode);
        for(ReceptacleType receptacleType: fingerprintingPlateSetup.getSourcePositionMap().getReceptacle()) {
            ReceptacleType destinationReceptacle = new ReceptacleType();
            destinationReceptacle.setReceptacleType("Well200");
            destinationReceptacle.setPosition(receptacleType.getPosition());
            destinationReceptacle.setVolume(new BigDecimal("8"));
            destinationReceptacle.setConcentration(new BigDecimal("0.01"));
            destinationPositionMap.getReceptacle().add(destinationReceptacle);
        }
        fingerprintingPlateSetup.setPositionMap(destinationPositionMap);
        bettaLimsMessageTestFactory.addMessage(messageList, fingerprintingPlateSetup);

        // Must be 12 digits
        picoPlateBarcode1 = testPrefix + "11";
        picoTransfer1 = bettaLimsMessageTestFactory.buildPlateToPlate("PicoTransfer", picoDilutionPlateBarcode,
                picoPlateBarcode1);
        bettaLimsMessageTestFactory.addMessage(messageList, picoTransfer1);

        // Must be 12 digits
        picoPlateBarcode2 = testPrefix + "22";
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

    public PlateTransferEventType getPicoTransfer1() {
        return picoTransfer1;
    }

    public PlateTransferEventType getPicoTransfer2() {
        return picoTransfer2;
    }
}
