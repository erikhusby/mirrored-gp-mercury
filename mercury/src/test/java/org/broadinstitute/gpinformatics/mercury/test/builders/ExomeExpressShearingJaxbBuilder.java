package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLimsMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for Shearing messages
 */
public class ExomeExpressShearingJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final List<String> tubeBarcodeList;
    private final String testPrefix;
    private final String rackBarcode;
    private String shearPlateBarcode;
    private String shearCleanPlateBarcode;
    private String covarisRackBarCode;

    private PlateEventType exExShearingBucket;
    private PlateTransferEventType shearTransferEventJaxb;
    private PlateEventType covarisLoadEventJaxb;
    private PlateTransferEventType postShearingTransferCleanupEventJaxb;
    private PlateTransferEventType shearingQcEventJaxb;
    private final List<BettaLimsMessage> messageList = new ArrayList<>();

    public ExomeExpressShearingJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                           List<String> tubeBarcodeList, String testPrefix, String rackBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.tubeBarcodeList = tubeBarcodeList;
        this.testPrefix = testPrefix;
        this.rackBarcode = rackBarcode;
    }

    //
    public PlateTransferEventType getShearTransferEventJaxb() {
        return shearTransferEventJaxb;
    }

    public PlateEventType getCovarisLoadEventJaxb() {
        return covarisLoadEventJaxb;
    }

    public String getCovarisRackBarCode() {
        return covarisRackBarCode;
    }

    public PlateTransferEventType getPostShearingTransferCleanupEventJaxb() {
        return postShearingTransferCleanupEventJaxb;
    }

    public PlateTransferEventType getShearingQcEventJaxb() {
        return shearingQcEventJaxb;
    }

    public String getShearPlateBarcode() {
        return shearPlateBarcode;
    }

    public String getShearCleanPlateBarcode() {
        return shearCleanPlateBarcode;
    }

    public List<BettaLimsMessage> getMessageList() {
        return messageList;
    }

    public PlateEventType getExExShearingBucket() {
        return exExShearingBucket;
    }

    public ExomeExpressShearingJaxbBuilder invoke() {

        exExShearingBucket =
                bettaLimsMessageTestFactory
                        .buildRackEvent(LabEventType.SHEARING_BUCKET.getName(), rackBarcode, tubeBarcodeList);
        bettaLimsMessageTestFactory.addMessage(messageList, exExShearingBucket);

        shearPlateBarcode = "ShearPlate" + testPrefix;
        shearTransferEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                        tubeBarcodeList, shearPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, shearTransferEventJaxb);

        covarisLoadEventJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent(LabEventType.COVARIS_LOADED.getName(), shearPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, covarisLoadEventJaxb);

        shearCleanPlateBarcode = "ShearCleanPlate" + testPrefix;
        postShearingTransferCleanupEventJaxb =
                bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName()
                        , shearPlateBarcode, shearCleanPlateBarcode);
        bettaLimsMessageTestFactory
                .addMessage(messageList, postShearingTransferCleanupEventJaxb);

        String shearQcPlateBarcode = "ShearQcPlate" + testPrefix;
        shearingQcEventJaxb = bettaLimsMessageTestFactory
                .buildPlateToPlate(LabEventType.SHEARING_QC.getName(), shearCleanPlateBarcode,
                        shearQcPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, shearingQcEventJaxb);

        return this;
    }
}
