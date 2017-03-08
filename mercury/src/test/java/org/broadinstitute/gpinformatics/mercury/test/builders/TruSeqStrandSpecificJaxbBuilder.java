package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for TruSeq Strand Specific messages.
 */
public class TruSeqStrandSpecificJaxbBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final List<String> tubeBarcodeList;
    private final String testPrefix;
    private final String indexPlateBarcode;
    private final int numSamples;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private final String rackBarcode;
    private String polyAPlateBarcode;
    private String polyASelectionPlate;
    private String secondStrandCleanupPlate;
    private String endRepairCleanupTSleanupPlate;
    private String adapterLigationCleanupPlate;
    private PlateTransferEventType polyATransferEventJaxb;
    private PlateEventType erccSpikeInEventJaxb;
    private PlateTransferEventType postASelectionEventJaxb;
    private PlateEventType polyABindingTSThermoCyclerJaxb;
    private PlateEventType mRNAElutionTSThermoCyclerJaxb;
    private PlateEventType fragmentationTSThermoCyclerJaxb;
    private PlateEventType firstStrandEventJaxb;
    private PlateEventType firstStrandTSThermoCyclerJaxb;
    private PlateEventType secondStrandEventJaxb;
    private PlateEventType secondStrandTSThermoCyclerJaxb;
    private PlateTransferEventType secondStrandCleanupEventJaxb;
    private PlateEventType endRepairTSEventJaxb;
    private PlateTransferEventType endRepairCleanupTSEventJaxb;
    private PlateEventType abaseTSEventJaxb;
    private PlateEventType aBaseTSThermoCyclerJaxb;
    private PlateTransferEventType indexedAdapterLigationTSJaxb;
    private PlateEventType adapterLigationTSThermoCyclerJaxb;
    private PlateTransferEventType adapterLigationCleanupTSJaxb;
    private PlateEventType enrichmentTSEventJaxb;
    private PlateEventType enrichmentTSThermoCyclerJaxb;
    private String enrichmentCleanupRackBarcode;
    private List<String> enrichmentcleanupTubeBarcodes;
    private PlateTransferEventType enrichmentCleanupTSJaxb;
    private String pondPico1Barcode;
    private String pondPico2Barcode;

    public TruSeqStrandSpecificJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                           List<String> tubeBarcodeList,
                                           String testPrefix,
                                           String indexPlateBarcode,
                                           String rackBarcode, int numSamples) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.tubeBarcodeList = tubeBarcodeList;
        this.testPrefix = testPrefix;
        this.indexPlateBarcode = indexPlateBarcode;
        this.rackBarcode = rackBarcode;
        this.numSamples = numSamples;
    }

    public String getPolyAPlateBarcode() {
        return polyAPlateBarcode;
    }

    public String getPolyASelectionPlate() {
        return polyASelectionPlate;
    }

    public String getSecondStrandCleanupPlate() {
        return secondStrandCleanupPlate;
    }

    public String getAdapterLigationCleanupPlate() {
        return adapterLigationCleanupPlate;
    }

    public PlateTransferEventType getPolyATransferEventJaxb() {
        return polyATransferEventJaxb;
    }

    public PlateEventType getErccSpikeInEventJaxb() {
        return erccSpikeInEventJaxb;
    }

    public PlateTransferEventType getPostASelectionEventJaxb() {
        return postASelectionEventJaxb;
    }

    public PlateEventType getFirstStrandEventJaxb() {
        return firstStrandEventJaxb;
    }

    public PlateEventType getSecondStrandEventJaxb() {
        return secondStrandEventJaxb;
    }

    public PlateTransferEventType getSecondStrandCleanupEventJaxb() {
        return secondStrandCleanupEventJaxb;
    }

    public PlateEventType getAbaseTSEventJaxb() {
        return abaseTSEventJaxb;
    }

    public PlateTransferEventType getIndexedAdapterLigationTSJaxb() {
        return indexedAdapterLigationTSJaxb;
    }

    public String getIndexPlateBarcode() {
        return indexPlateBarcode;
    }

    public PlateTransferEventType getAdapterLigationCleanupTSJaxb() {
        return adapterLigationCleanupTSJaxb;
    }

    public PlateEventType getEnrichmentTSEventJaxb() {
        return enrichmentTSEventJaxb;
    }

    public String getEnrichmentCleanupRackBarcode() {
        return enrichmentCleanupRackBarcode;
    }

    public List<String> getEnrichmentcleanupTubeBarcodes() {
        return enrichmentcleanupTubeBarcodes;
    }

    public PlateTransferEventType getEnrichmentCleanupTSJaxb() {
        return enrichmentCleanupTSJaxb;
    }

    public String getPondPico1Barcode() {
        return pondPico1Barcode;
    }

    public String getPondPico2Barcode() {
        return pondPico2Barcode;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public TruSeqStrandSpecificJaxbBuilder invoke() {
        polyAPlateBarcode = "PolyAPlate" + testPrefix;
        polyATransferEventJaxb = bettaLimsMessageTestFactory.buildRackToPlate("PolyATransfer", rackBarcode,
                tubeBarcodeList, polyAPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, polyATransferEventJaxb);

        erccSpikeInEventJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("ERCCSpikeIn", polyAPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, erccSpikeInEventJaxb);

        polyASelectionPlate = "PolyASelectionPLate" + testPrefix;
        postASelectionEventJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                "PolyASelectionTS", polyAPlateBarcode, polyASelectionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, postASelectionEventJaxb);

        polyABindingTSThermoCyclerJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("PolyABindingTSThermoCyclerLoaded", polyASelectionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, polyABindingTSThermoCyclerJaxb);

        mRNAElutionTSThermoCyclerJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("MRNAElutionTSThermoCyclerLoaded", polyASelectionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, mRNAElutionTSThermoCyclerJaxb);

        fragmentationTSThermoCyclerJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("FragmentationTSThermoCyclerLoaded", polyASelectionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, fragmentationTSThermoCyclerJaxb);


        firstStrandEventJaxb = bettaLimsMessageTestFactory.buildPlateEvent("FirstStrandTS", polyASelectionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, firstStrandEventJaxb);

        firstStrandTSThermoCyclerJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("FirstStrandTSThermoCyclerLoaded", polyASelectionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, firstStrandTSThermoCyclerJaxb);

        secondStrandEventJaxb = bettaLimsMessageTestFactory.buildPlateEvent("SecondStrandTS", polyASelectionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, secondStrandEventJaxb);

        secondStrandTSThermoCyclerJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("SecondStrandTSThermoCyclerLoaded", polyASelectionPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, secondStrandTSThermoCyclerJaxb);

        secondStrandCleanupPlate = "SecondStrandCleanupPlate" + testPrefix;
        secondStrandCleanupEventJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                "SecondStrandCleanupTS", polyASelectionPlate, secondStrandCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, secondStrandCleanupEventJaxb);

        abaseTSEventJaxb = bettaLimsMessageTestFactory.buildPlateEvent("ABaseTS", secondStrandCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, abaseTSEventJaxb);

        aBaseTSThermoCyclerJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("ABaseTSThermoCyclerLoaded", secondStrandCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, aBaseTSThermoCyclerJaxb);

        indexedAdapterLigationTSJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("IndexedAdapterLigationTS",
                indexPlateBarcode,
                secondStrandCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, indexedAdapterLigationTSJaxb);

        adapterLigationTSThermoCyclerJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("AdapterLigationTSThermoCyclerLoaded", secondStrandCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, adapterLigationTSThermoCyclerJaxb);

        adapterLigationCleanupPlate = "AdapterLigationTSCleanupPlate" + testPrefix;
        adapterLigationCleanupTSJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("AdapterLigationCleanupTS",
                secondStrandCleanupPlate,
                adapterLigationCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, adapterLigationCleanupTSJaxb);

        enrichmentTSEventJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EnrichmentTS", adapterLigationCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, enrichmentTSEventJaxb);

        enrichmentTSThermoCyclerJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("EnrichmentTSThermoCyclerLoaded", adapterLigationCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, enrichmentTSThermoCyclerJaxb);

        enrichmentCleanupRackBarcode = "TruSeqReg" + testPrefix;
        enrichmentcleanupTubeBarcodes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            enrichmentcleanupTubeBarcodes.add("TruSeqReg" + testPrefix + rackPosition);
        }
        enrichmentCleanupTSJaxb = bettaLimsMessageTestFactory.buildPlateToRack("EnrichmentCleanupTS", adapterLigationCleanupPlate,
                enrichmentCleanupRackBarcode, enrichmentcleanupTubeBarcodes);
        for (ReceptacleType receptacleType : enrichmentCleanupTSJaxb.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(new BigDecimal("50"));
        }
        bettaLimsMessageTestFactory.addMessage(messageList, enrichmentCleanupTSJaxb);

        pondPico1Barcode = "771" + testPrefix;
        PlateTransferEventType pondPico1Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("PondPico",
                enrichmentCleanupRackBarcode, enrichmentcleanupTubeBarcodes, pondPico1Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, pondPico1Jaxb);
        pondPico2Barcode = "772" + testPrefix;
        PlateTransferEventType pondPico2Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("PondPico",
                enrichmentCleanupRackBarcode, enrichmentcleanupTubeBarcodes, pondPico2Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, pondPico2Jaxb);

        return this;
    }
}
