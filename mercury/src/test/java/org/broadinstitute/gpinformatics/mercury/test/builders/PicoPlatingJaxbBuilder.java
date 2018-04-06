package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Refer to {@link org.broadinstitute.gpinformatics.mercury.test.SamplesPicoEndToEndTest} for expanded version.
 * <p/>
 * TODO Merge to lessen code duplication.
 */
public class PicoPlatingJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> tubeBarcodes;
    private String rackBarcode;

    private PlateEventType picoPlatingBucket;
    private PlateTransferEventType picoPlatingQc;
    private PlateTransferEventType picoPlatingSetup1;
    private PlateTransferEventType picoPlatingSetup2;
    private PlateTransferEventType picoPlatingSetup3;
    private PlateTransferEventType picoPlatingSetup4;
    private PlateTransferEventType picoPlatingNormalization;
    private PlateTransferEventType picoPlatingPostNormSetup;


    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private String picoPlatingQcBarcode;
    private String picoPlatingSetup1Barcode;
    private String picoPlatingSetup2Barcode;
    private String picoPlatingSetup3Barcode;
    private String picoPlatingSetup4Barcode;
    private String picoPlatingNormalizationBarcode;
    private String picoPlatingPostNormSetupBarcode;
    private List<String> picoPlateNormBarcodes;

    public PicoPlatingJaxbBuilder(String rackBarcode, List<String> tubeBarcodes, String testPrefix,
                                  BettaLimsMessageTestFactory bettaLimsMessageTestFactory) {
        this.rackBarcode = rackBarcode;
        this.tubeBarcodes = tubeBarcodes;
        this.testPrefix = testPrefix;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
    }

    public String getTestPrefix() {
        return testPrefix;
    }

    public List<String> getTubeBarcodes() {
        return tubeBarcodes;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public PlateTransferEventType getPicoPlatingQc() {
        return picoPlatingQc;
    }

    public PlateTransferEventType getPicoPlatingSetup1() {
        return picoPlatingSetup1;
    }

    public PlateTransferEventType getPicoPlatingSetup2() {
        return picoPlatingSetup2;
    }

    public PlateTransferEventType getPicoPlatingSetup3() {
        return picoPlatingSetup3;
    }

    public PlateTransferEventType getPicoPlatingSetup4() {
        return picoPlatingSetup4;
    }

    public PlateTransferEventType getPicoPlatingNormalization() {
        return picoPlatingNormalization;
    }

    public PlateTransferEventType getPicoPlatingPostNormSetup() {
        return picoPlatingPostNormSetup;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getPicoPlatingNormalizationBarcode() {
        return picoPlatingNormalizationBarcode;
    }

    public List<String> getPicoPlateNormBarcodes() {
        return picoPlateNormBarcodes;
    }

    public PlateEventType getPicoPlatingBucket() {
        return picoPlatingBucket;
    }

    public PicoPlatingJaxbBuilder invoke() {

        picoPlatingBucket = bettaLimsMessageTestFactory
                .buildRackEvent(LabEventType.PICO_PLATING_BUCKET.getName(), rackBarcode, tubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, picoPlatingBucket);

        picoPlatingQcBarcode = LabEventType.PICO_PLATING_QC.getName() + testPrefix;
        picoPlatingQc = bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.PICO_PLATING_QC.getName(),
                rackBarcode, tubeBarcodes, picoPlatingQcBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoPlatingQc);


        picoPlatingSetup1Barcode = LabEventType.PICO_DILUTION_TRANSFER.getName() + testPrefix;
        picoPlatingSetup1 = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.PICO_DILUTION_TRANSFER
                .getName(), picoPlatingQcBarcode, picoPlatingSetup1Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoPlatingSetup1);

        picoPlatingSetup2Barcode = LabEventType.PICO_BUFFER_ADDITION.getName() + testPrefix;
        picoPlatingSetup2 = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.PICO_BUFFER_ADDITION
                .getName(), picoPlatingSetup1Barcode, picoPlatingSetup2Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoPlatingSetup2);

        picoPlatingSetup3Barcode = LabEventType.PICO_MICROFLUOR_TRANSFER.getName() + testPrefix;
        picoPlatingSetup3 = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.PICO_MICROFLUOR_TRANSFER
                .getName(), picoPlatingSetup2Barcode, picoPlatingSetup3Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoPlatingSetup3);

        picoPlatingSetup4Barcode = LabEventType.PICO_STANDARDS_TRANSFER.getName() + testPrefix;
        picoPlatingSetup4 = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.PICO_STANDARDS_TRANSFER
                .getName(), picoPlatingSetup3Barcode, picoPlatingSetup4Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoPlatingSetup4);

        picoPlateNormBarcodes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= tubeBarcodes.size(); rackPosition++) {
            picoPlateNormBarcodes.add("PicoPlateNorm" + testPrefix + rackPosition);
        }
        picoPlatingNormalizationBarcode = LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName() + testPrefix;
        picoPlatingNormalization = bettaLimsMessageTestFactory.buildRackToRack(
                LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName(), rackBarcode, tubeBarcodes,
                picoPlatingNormalizationBarcode, picoPlateNormBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, picoPlatingNormalization);

        picoPlatingPostNormSetupBarcode = LabEventType.PICO_PLATING_POST_NORM_PICO.getName() + testPrefix;
        picoPlatingPostNormSetup = bettaLimsMessageTestFactory.buildRackToPlate(
                LabEventType.PICO_PLATING_POST_NORM_PICO.getName(), picoPlatingNormalizationBarcode,
                picoPlateNormBarcodes, picoPlatingPostNormSetupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoPlatingPostNormSetup);

        return this;
    }
}
