package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds JAXB objects for library construction messages
 */
public class LibraryConstructionJaxbBuilder {
    public static final String P_7_INDEX_PLATE_BARCODE = "000002715223";
    public static final String P_5_INDEX_PLATE_BARCODE = "000002655323";

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String shearCleanPlateBarcode;
    private final String p7IndexPlateBarcode;
    private final String p5IndexPlateBarcode;
    private int numSamples;
    private TargetSystem targetSystem;

    private String pondRegRackBarcode;
    private List<String> pondRegTubeBarcodes;
    private String pondPico1Barcode;
    private String pondPico2Barcode;

    private PlateEventType endRepairJaxb;
    private PlateEventType endRepairCleanupJaxb;
    private PlateEventType aBaseJaxb;
    private PlateEventType aBaseCleanupJaxb;
    private PlateTransferEventType indexedAdapterLigationJaxb;
    private PlateTransferEventType ligationCleanupJaxb;
    private PlateEventType pondEnrichmentJaxb;
    private PlateTransferEventType indexP5PondEnrichmentJaxb;
    private PlateTransferEventType pondCleanupJaxb;
    private PlateTransferEventType pondRegistrationJaxb;
    private PlateEventType postABaseThermoCyclerLoadedJaxb;
    private PlateEventType postIdxAdapterLigationThermoCyclerLoadedJaxb;
    private PlateEventType postPondEnrichmentThermoCyclerLoadedJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private final List<Triple<String, String, Integer>> endRepairReagents;
    private final List<Triple<String, String, Integer>> endRepairCleanupReagents;
    private final List<Triple<String, String, Integer>> pondEnrichmentReagents;
    private LibraryConstructionEntityBuilder.PondType pondType;

    public enum TargetSystem {
        /** Messages that might be routed to Squid must have pre-registered lab machines and reagent kit types. */
        SQUID_VIA_MERCURY,
        /** Mercury doesn't pre-register machines and reagent types, so the messages have fewer constraints. */
        MERCURY_ONLY
    }

    // todo jmt why do the reagents need to be parameters?  All callers supply the same values.
    public LibraryConstructionJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            String shearCleanPlateBarcode, String p7IndexPlateBarcode,
            String p5IndexPlateBarcode, int numSamples, TargetSystem targetSystem,
            List<Triple<String, String, Integer>> endRepairReagents,
            List<Triple<String, String, Integer>> endRepairCleanupReagents,
            List<Triple<String, String, Integer>> pondEnrichmentReagents,
            LibraryConstructionEntityBuilder.PondType pondType) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.shearCleanPlateBarcode = shearCleanPlateBarcode;
        this.p7IndexPlateBarcode = p7IndexPlateBarcode;
        this.p5IndexPlateBarcode = p5IndexPlateBarcode;
        this.numSamples = numSamples;
        this.targetSystem = targetSystem;
        this.endRepairReagents = endRepairReagents;
        this.endRepairCleanupReagents = endRepairCleanupReagents;
        this.pondEnrichmentReagents = pondEnrichmentReagents;
        this.pondType = pondType;
    }

    public PlateEventType getEndRepairJaxb() {
        return endRepairJaxb;
    }

    public PlateEventType getEndRepairCleanupJaxb() {
        return endRepairCleanupJaxb;
    }

    public PlateEventType getaBaseJaxb() {
        return aBaseJaxb;
    }

    public PlateEventType getaBaseCleanupJaxb() {
        return aBaseCleanupJaxb;
    }

    public String getP7IndexPlateBarcode() {
        return p7IndexPlateBarcode;
    }

    public String getP5IndexPlateBarcode() {
        return p5IndexPlateBarcode;
    }

    public PlateTransferEventType getIndexedAdapterLigationJaxb() {
        return indexedAdapterLigationJaxb;
    }

    public PlateTransferEventType getLigationCleanupJaxb() {
        return ligationCleanupJaxb;
    }

    public PlateEventType getPondEnrichmentJaxb() {
        return pondEnrichmentJaxb;
    }

    public PlateTransferEventType getIndexP5PondEnrichmentJaxb() {
        return indexP5PondEnrichmentJaxb;
    }

    public PlateTransferEventType getPondCleanupJaxb() {
        return pondCleanupJaxb;
    }

    public PlateTransferEventType getPondRegistrationJaxb() {
        return pondRegistrationJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getPondRegRackBarcode() {
        return pondRegRackBarcode;
    }

    public List<String> getPondRegTubeBarcodes() {
        return pondRegTubeBarcodes;
    }

    public PlateEventType getPostABaseThermoCyclerLoadedJaxb() {
        return postABaseThermoCyclerLoadedJaxb;
    }

    public PlateEventType getPostIdxAdapterLigationThermoCyclerLoadedJaxb() {
        return postIdxAdapterLigationThermoCyclerLoadedJaxb;
    }

    public PlateEventType getPostPondEnrichmentThermoCyclerLoadedJaxb() {
        return postPondEnrichmentThermoCyclerLoadedJaxb;
    }

    public String getPondPico1Barcode() {
        return pondPico1Barcode;
    }

    public String getPondPico2Barcode() {
        return pondPico2Barcode;
    }

    public LibraryConstructionJaxbBuilder invoke() {
        endRepairJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EndRepair", shearCleanPlateBarcode,
                targetSystem == TargetSystem.MERCURY_ONLY ?
                        BettaLimsMessageTestFactory.reagentList(endRepairReagents) : Collections.EMPTY_LIST);
        bettaLimsMessageTestFactory.addMessage(messageList, endRepairJaxb);

        endRepairCleanupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EndRepairCleanup", shearCleanPlateBarcode,
                targetSystem == TargetSystem.MERCURY_ONLY ?
                        BettaLimsMessageTestFactory.reagentList(endRepairCleanupReagents) : Collections.EMPTY_LIST);
        bettaLimsMessageTestFactory.addMessage(messageList, endRepairCleanupJaxb);

        aBaseJaxb = bettaLimsMessageTestFactory.buildPlateEvent("ABase", shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, aBaseJaxb);

        postABaseThermoCyclerLoadedJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("PostAbaseThermoCyclerLoaded", shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, postABaseThermoCyclerLoadedJaxb);


        aBaseCleanupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("ABaseCleanup", shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, aBaseCleanupJaxb);

//            indexPlateBarcode = "IndexPlate" + testPrefix;
        indexedAdapterLigationJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("IndexedAdapterLigation",
                p7IndexPlateBarcode,
                shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, indexedAdapterLigationJaxb);

        postIdxAdapterLigationThermoCyclerLoadedJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostIndexedAdapterLigationThermoCyclerLoaded", shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, postIdxAdapterLigationThermoCyclerLoadedJaxb);

        String ligationCleanupBarcode = "ligationCleanupPlate" + testPrefix;
        ligationCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("AdapterLigationCleanup",
                shearCleanPlateBarcode,
                ligationCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, ligationCleanupJaxb);

        if (p5IndexPlateBarcode == null) {
            pondEnrichmentJaxb = bettaLimsMessageTestFactory.buildPlateEvent("PondEnrichment", ligationCleanupBarcode,
                    targetSystem == TargetSystem.MERCURY_ONLY ?
                            BettaLimsMessageTestFactory.reagentList(pondEnrichmentReagents) : Collections.EMPTY_LIST);

            bettaLimsMessageTestFactory.addMessage(messageList, pondEnrichmentJaxb);
        } else {
            indexP5PondEnrichmentJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("IndexP5PondEnrichment",
                    p5IndexPlateBarcode, ligationCleanupBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, indexP5PondEnrichmentJaxb);
        }

        postPondEnrichmentThermoCyclerLoadedJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostPondEnrichmentThermoCyclerLoaded", ligationCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, postPondEnrichmentThermoCyclerLoadedJaxb);

        String pondCleanupBarcode = "pondCleanupPlate" + testPrefix;
        pondCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("HybSelPondEnrichmentCleanup",
                ligationCleanupBarcode, pondCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, pondCleanupJaxb);

        pondRegRackBarcode = "PondReg" + testPrefix;
        pondRegTubeBarcodes = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            pondRegTubeBarcodes.add(LabEventTest.POND_REGISTRATION_TUBE_PREFIX + testPrefix + rackPosition);
        }
        pondRegistrationJaxb = bettaLimsMessageTestFactory.buildPlateToRack(pondType.getEventType(), pondCleanupBarcode,
                pondRegRackBarcode, pondRegTubeBarcodes);
        for (ReceptacleType receptacleType : pondRegistrationJaxb.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(new BigDecimal("50"));
        }
        bettaLimsMessageTestFactory.addMessage(messageList, pondRegistrationJaxb);

        // Pico plate barcodes must be all numeric to be accepted by the Varioskan parser
        pondPico1Barcode = "771" + testPrefix;
        PlateTransferEventType pondPico1Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("PondPico",
                pondRegRackBarcode, pondRegTubeBarcodes, pondPico1Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, pondPico1Jaxb);
        pondPico2Barcode = "772" + testPrefix;
        PlateTransferEventType pondPico2Jaxb = bettaLimsMessageTestFactory.buildRackToPlate("PondPico",
                pondRegRackBarcode, pondRegTubeBarcodes, pondPico2Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, pondPico2Jaxb);

        return this;
    }
}
