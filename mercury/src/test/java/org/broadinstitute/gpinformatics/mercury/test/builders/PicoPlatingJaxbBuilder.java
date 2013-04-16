package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Refer to {@link org.broadinstitute.gpinformatics.mercury.test.SamplesPicoEndToEndTest} for expanded version.
 * <p/>
 * TODO SGM:  Merge to lessen code duplication.
 */
public class PicoPlatingJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String>                tubeBarcodes;
    private String rackBarcode;

    private PlateEventType         picoPlatingBucket;
    private PlateTransferEventType picoPlatingQc;
    private PlateTransferEventType picoPlatingSetup1;
    private PlateTransferEventType picoPlatingSetup2;
    private PlateTransferEventType picoPlatingSetup3;
    private PlateTransferEventType picoPlatingSetup4;
    private PlateTransferEventType picoPlatingNormalizaion;
    private PlateTransferEventType picoPlatingPostNormSetup;


    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
    private String picoPlatingQcBarcode;
    private String picoPlatingSetup1Barcode;
    private String picoPlatingSetup2Barcode;
    private String picoPlatingSetup3Barcode;
    private String picoPlatingSetup4Barcode;
    private String picoPlatingNormalizaionBarcode;
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

    public PlateTransferEventType getPicoPlatingNormalizaion() {
        return picoPlatingNormalizaion;
    }

    public PlateTransferEventType getPicoPlatingPostNormSetup() {
        return picoPlatingPostNormSetup;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getPicoPlatingNormalizaionBarcode() {
        return picoPlatingNormalizaionBarcode;
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
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoPlatingBucket);

        picoPlatingQcBarcode = LabEventType.PICO_PLATING_QC.getName() + testPrefix;
        picoPlatingQc = bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.PICO_PLATING_QC.getName(),
                rackBarcode, tubeBarcodes, picoPlatingQcBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoPlatingQc);


        picoPlatingSetup1Barcode = LabEventType.PICO_DILUTION_TRANSFER.getName() + testPrefix;
        picoPlatingSetup1 = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.PICO_DILUTION_TRANSFER
                .getName(), picoPlatingQcBarcode, picoPlatingSetup1Barcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoPlatingSetup1);

        picoPlatingSetup2Barcode = LabEventType.PICO_BUFFER_ADDITION.getName() + testPrefix;
        picoPlatingSetup2 = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.PICO_BUFFER_ADDITION
                .getName(), picoPlatingSetup1Barcode, picoPlatingSetup2Barcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoPlatingSetup2);

        picoPlatingSetup3Barcode = LabEventType.PICO_MICROFLUOR_TRANSFER.getName() + testPrefix;
        picoPlatingSetup3 = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.PICO_MICROFLUOR_TRANSFER
                .getName(), picoPlatingSetup2Barcode, picoPlatingSetup3Barcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoPlatingSetup3);

        picoPlatingSetup4Barcode = LabEventType.PICO_STANDARDS_TRANSFER.getName() + testPrefix;
        picoPlatingSetup4 = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.PICO_STANDARDS_TRANSFER
                .getName(), picoPlatingSetup3Barcode, picoPlatingSetup4Barcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoPlatingSetup4);

        picoPlateNormBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= tubeBarcodes.size() / 2; rackPosition++) {
            picoPlateNormBarcodes.add("PicoPlateNorm" + testPrefix + rackPosition);
        }
        picoPlatingNormalizaionBarcode = LabEventType.SAMPLES_NORMALIZATION_TRANSFER.getName() + testPrefix;
        picoPlatingNormalizaion = bettaLimsMessageTestFactory.buildRackToRack(LabEventType
                .SAMPLES_NORMALIZATION_TRANSFER
                .getName(), rackBarcode, tubeBarcodes, picoPlatingNormalizaionBarcode, picoPlateNormBarcodes);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoPlatingNormalizaion);

        picoPlatingPostNormSetupBarcode = LabEventType.PICO_PLATING_POST_NORM_PICO.getName() + testPrefix;
        picoPlatingPostNormSetup = bettaLimsMessageTestFactory
                .buildRackToPlate(LabEventType.PICO_PLATING_POST_NORM_PICO
                                              .getName(), picoPlatingNormalizaionBarcode, picoPlateNormBarcodes,
                                         picoPlatingPostNormSetupBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, picoPlatingPostNormSetup);

        return this;
    }
}
