package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for Shearing messages
 */
public class ShearingJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final List<String>                tubeBarcodeList;
    private final String testPrefix;
    private final String rackBarcode;
    private String shearPlateBarcode;
    private String shearCleanPlateBarcode;

    private PlateTransferEventType shearingTransferEventJaxb;
    private PlateTransferEventType postShearingTransferCleanupEventJaxb;
    private PlateTransferEventType shearingQcEventJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    public ShearingJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, List<String> tubeBarcodeList,
                               String testPrefix, String rackBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.tubeBarcodeList = tubeBarcodeList;
        this.testPrefix = testPrefix;
        this.rackBarcode = rackBarcode;
    }

    public PlateTransferEventType getShearingTransferEventJaxb() {
        return shearingTransferEventJaxb;
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

    public ShearingJaxbBuilder invoke() {
        shearPlateBarcode = "ShearPlate" + testPrefix;
        shearingTransferEventJaxb = bettaLimsMessageTestFactory.buildRackToPlate("ShearingTransfer", rackBarcode,
                tubeBarcodeList, shearPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, shearingTransferEventJaxb);

        shearCleanPlateBarcode = "ShearCleanPlate" + testPrefix;
        postShearingTransferCleanupEventJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                "PostShearingTransferCleanup", shearPlateBarcode, shearCleanPlateBarcode);
        bettaLimsMessageTestFactory
                .addMessage(messageList, postShearingTransferCleanupEventJaxb);

        String shearQcPlateBarcode = "ShearQcPlate" + testPrefix;
        shearingQcEventJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("ShearingQC", shearCleanPlateBarcode,
                shearQcPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, shearingQcEventJaxb);

        return this;
    }
}
