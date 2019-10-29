package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

public class SingleCellHashingJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String sourceRackBarcode;
    private final List<String> tubeBarcodes;
    private final String indexPlateBarcode;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateTransferEventType spri1Jaxb;
    private PlateTransferEventType spri2Jaxb;
    private PlateTransferEventType pcrJaxb;
    private PlateTransferEventType indexAdapterPCRJaxb;
    private List<String> spriTubeBarcodes;
    private String hashingSpriRack;
    private String hashingCleanupRack;
    private PlateTransferEventType cleanupJaxb;
    private List<String> cleanupBarcodes;

    public SingleCellHashingJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                    String sourceRackBarcode, List<String> tubeBarcodes, String indexPlateBarcode) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.sourceRackBarcode = sourceRackBarcode;
        this.tubeBarcodes = tubeBarcodes;
        this.indexPlateBarcode = indexPlateBarcode;
    }

    public SingleCellHashingJaxbBuilder invoke() {
        String spriPlate = "hasingSpriRack1" + testPrefix;
        spri1Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("SingleCellHashingSPRI1",
                sourceRackBarcode, tubeBarcodes, spriPlate);

        bettaLimsMessageTestFactory.addMessage(messageList, spri1Jaxb);

        hashingSpriRack = "hasingSpriRack2" + testPrefix;
        spriTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < tubeBarcodes.size(); i++) {
            spriTubeBarcodes.add("SCHashSpriTube2_" + i);
        }
        spri2Jaxb = bettaLimsMessageTestFactory.buildPlateToRack("SingleCellHashingSPRI2",
                spriPlate, hashingSpriRack, spriTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, spri2Jaxb);

        indexAdapterPCRJaxb = bettaLimsMessageTestFactory.buildPlateToRack("SingleCellHashingIndexAddition",
                indexPlateBarcode, hashingSpriRack, spriTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, indexAdapterPCRJaxb);

        String pcrPlate = "hasingPcr1" + testPrefix;
        pcrJaxb = bettaLimsMessageTestFactory.buildRackToPlate("SingleCellHashingPCR",
                hashingSpriRack, spriTubeBarcodes, pcrPlate);

        bettaLimsMessageTestFactory.addMessage(messageList, pcrJaxb);

        hashingCleanupRack = "hashingCleanupRack" + testPrefix;
        cleanupBarcodes = new ArrayList<>();
        for (int i = 0; i < tubeBarcodes.size(); i++) {
            cleanupBarcodes.add("SCHashCleanupTube_" + i);
        }
        cleanupJaxb = bettaLimsMessageTestFactory.buildPlateToRack("SingleCellHashingPCRCleanup",
                pcrPlate, hashingCleanupRack, cleanupBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, cleanupJaxb);

        return this;
    }

    public String getSourceRackBarcode() {
        return sourceRackBarcode;
    }

    public List<String> getTubeBarcodes() {
        return tubeBarcodes;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateTransferEventType getSpri1Jaxb() {
        return spri1Jaxb;
    }

    public PlateTransferEventType getSpri2Jaxb() {
        return spri2Jaxb;
    }

    public PlateTransferEventType getPcrJaxb() {
        return pcrJaxb;
    }

    public PlateTransferEventType getIndexAdapterPCRJaxb() {
        return indexAdapterPCRJaxb;
    }

    public List<String> getSpriTubeBarcodes() {
        return spriTubeBarcodes;
    }

    public String getHashingSpriRack() {
        return hashingSpriRack;
    }

    public String getHashingCleanupRack() {
        return hashingCleanupRack;
    }

    public PlateTransferEventType getCleanupJaxb() {
        return cleanupJaxb;
    }
}
