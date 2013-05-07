package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
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
    private final List<String>                tubeBarcodeList;
    private final String testPrefix;
    private final String rackBarcode;
    private String shearPlateBarcode;
    private String shearCleanPlateBarcode;
    private String covarisRackBarCode;

    private PlateEventType         exExShearingBucket;
    private PlateTransferEventType shearTransferEventJaxb;
    private PlateEventType covarisLoadEventJaxb;
    private PlateTransferEventType postShearingTransferCleanupEventJaxb;
    private PlateTransferEventType shearingQcEventJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

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

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateEventType getExExShearingBucket() {
        return exExShearingBucket;
    }

    public ExomeExpressShearingJaxbBuilder invoke() {

        exExShearingBucket =
                bettaLimsMessageTestFactory
                        .buildRackEvent(LabEventType.SHEARING_BUCKET.getName(), rackBarcode, tubeBarcodeList);
        BettaLimsMessageTestFactory.addMessage(messageList, bettaLimsMessageTestFactory, exExShearingBucket);

        shearPlateBarcode = "ShearPlate" + testPrefix;
        shearTransferEventJaxb = bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                tubeBarcodeList, shearPlateBarcode);
        BettaLimsMessageTestFactory.addMessage(messageList, bettaLimsMessageTestFactory, shearTransferEventJaxb);

        covarisLoadEventJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent(LabEventType.COVARIS_LOADED.getName(), shearPlateBarcode);
        BettaLimsMessageTestFactory.addMessage(messageList, bettaLimsMessageTestFactory, covarisLoadEventJaxb);

        shearCleanPlateBarcode = "ShearCleanPlate" + testPrefix;
        postShearingTransferCleanupEventJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.POST_SHEARING_TRANSFER_CLEANUP.getName()
                , shearPlateBarcode, shearCleanPlateBarcode);
        BettaLimsMessageTestFactory
                .addMessage(messageList, bettaLimsMessageTestFactory, postShearingTransferCleanupEventJaxb);

        String shearQcPlateBarcode = "ShearQcPlate" + testPrefix;
        shearingQcEventJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(LabEventType.SHEARING_QC.getName(), shearCleanPlateBarcode,
                shearQcPlateBarcode);
        BettaLimsMessageTestFactory.addMessage(messageList, bettaLimsMessageTestFactory, shearingQcEventJaxb);

        return this;
    }
}
