package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

public class SingleCell10XJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String sourceRackBarcode;
    private final List<String> tubeBarcodes;
    private final String indexPlateBarcode;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateTransferEventType endRepairAbaseJaxb;
    private PlateTransferEventType aTailCleanupJaxb;
    private PlateEventType adapterLigationJaxb;
    private PlateTransferEventType ligationCleanupJaxb;
    private PlateTransferEventType indexAdapterPCRJaxb;
    private PlateTransferEventType doubleSidedSpriJaxb;
    private String doubleSidedSpriPlate;
    private List<String> spriTubeBarcodes;

    public SingleCell10XJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                    String sourceRackBarcode, List<String> tubeBarcodes, String indexPlateBarcode) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.sourceRackBarcode = sourceRackBarcode;
        this.tubeBarcodes = tubeBarcodes;
        this.indexPlateBarcode = indexPlateBarcode;
    }

    public SingleCell10XJaxbBuilder invoke() {
        String aTailPlate = "aTailPlate" + testPrefix;
        String spriPlate = "aTailPlate" + testPrefix;
        endRepairAbaseJaxb = bettaLimsMessageTestFactory.buildRackToPlate("SingleCellEndRepairABase",
                sourceRackBarcode, tubeBarcodes, spriPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, endRepairAbaseJaxb);

        String aTailCleanupPlate = "aTailCleanupPlate" + testPrefix;
        aTailCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("SingleCellATailCleanup",
                aTailPlate,
                aTailCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, aTailCleanupJaxb);

        adapterLigationJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "SingleCellAdapterLigation", aTailCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, adapterLigationJaxb);

        String ligationCleanupPlate = "ligationCleanupPlate" + testPrefix;
        ligationCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("SingleCellLigationCleanup",
                aTailCleanupPlate,
                ligationCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, ligationCleanupJaxb);

        indexAdapterPCRJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("SingleCellIndexAdapterPCR",
                indexPlateBarcode,
                ligationCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, indexAdapterPCRJaxb);

        String hashingSpriRack = "dblSidedSprirck" + testPrefix;
        spriTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < tubeBarcodes.size(); i++) {
            spriTubeBarcodes.add("SCSpriTube_" + i);
        }
        doubleSidedSpriJaxb = bettaLimsMessageTestFactory.buildPlateToRack("SingleCellDoubleSidedCleanup",
                ligationCleanupPlate, hashingSpriRack, spriTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, doubleSidedSpriJaxb);

        return this;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateTransferEventType getEndRepairAbaseJaxb() {
        return endRepairAbaseJaxb;
    }

    public PlateTransferEventType getaTailCleanupJaxb() {
        return aTailCleanupJaxb;
    }

    public PlateEventType getAdapterLigationJaxb() {
        return adapterLigationJaxb;
    }

    public PlateTransferEventType getLigationCleanupJaxb() {
        return ligationCleanupJaxb;
    }

    public PlateTransferEventType getIndexAdapterPCRJaxb() {
        return indexAdapterPCRJaxb;
    }

    public PlateTransferEventType getDoubleSidedSpriJaxb() {
        return doubleSidedSpriJaxb;
    }

    public String getDoubleSidedSpriPlate() {
        return doubleSidedSpriPlate;
    }

    public List<String> getSpriTubeBarcodes() {
        return spriTubeBarcodes;
    }
}
