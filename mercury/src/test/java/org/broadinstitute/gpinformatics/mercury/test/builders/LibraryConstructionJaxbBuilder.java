package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
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
    public static final String DUAL_INDEX_PLATE_BARCODE = "000003175623";

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String shearCleanPlateBarcode;
    private final String p7IndexPlateBarcode;
    private final String p5IndexPlateBarcode;
    private int numSamples;

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
    private PondType pondType;
    private String pondNormRackBarcode;
    private List<String> pondNormTubeBarcodes = new ArrayList<>();
    private PlateCherryPickEvent pondNormJaxb;
    private PlateEventType endRepairAbaseJaxb;
    private PlateEventType wgsPCRCleanupJaxb;
    private PlateEventType cellFreePCRCleanupJaxb;

    public enum PondType {
        PCR_FREE("PCRFreePondRegistration"),
        PCR_PLUS("PCRPlusPondRegistration"),
        PCR_FREE_HYPER_PREP("PCRFreePondRegistration"),
        PCR_PLUS_HYPER_PREP("PCRPlusPondRegistration"),
        CELL_FREE("CFDnaPondRegistration"),
        REGULAR("PondRegistration");

        private String eventType;

        PondType(String eventType) {
            this.eventType = eventType;
        }

        public String getEventType() {
            return eventType;
        }
    }

    // todo jmt why do the reagents need to be parameters?  All callers supply the same values.
    public LibraryConstructionJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            String shearCleanPlateBarcode, String p7IndexPlateBarcode,
            String p5IndexPlateBarcode, int numSamples,
            List<Triple<String, String, Integer>> endRepairReagents,
            List<Triple<String, String, Integer>> endRepairCleanupReagents,
            List<Triple<String, String, Integer>> pondEnrichmentReagents,
            PondType pondType) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.shearCleanPlateBarcode = shearCleanPlateBarcode;
        this.p7IndexPlateBarcode = p7IndexPlateBarcode;
        this.p5IndexPlateBarcode = p5IndexPlateBarcode;
        this.numSamples = numSamples;
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

    public PlateEventType getEndRepairAbaseJaxb() {
        return endRepairAbaseJaxb;
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

    public PlateEventType getWgsPCRCleanupJaxb() {
        return wgsPCRCleanupJaxb;
    }

    public PlateEventType getCellFreePCRCleanupJaxb() {
        return cellFreePCRCleanupJaxb;
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

    public PlateCherryPickEvent getPondNormJaxb() {
        return pondNormJaxb;
    }

    public String getPondNormRackBarcode() {
        return pondNormRackBarcode;
    }

    public List<String> getPondNormTubeBarcodes() {
        return pondNormTubeBarcodes;
    }

    public LibraryConstructionJaxbBuilder invoke() {
        if (pondType == PondType.PCR_FREE_HYPER_PREP || pondType == PondType.PCR_PLUS_HYPER_PREP ||
            pondType == PondType.CELL_FREE) {
            endRepairAbaseJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EndRepair_ABase", shearCleanPlateBarcode,
                    BettaLimsMessageTestFactory.reagentList(endRepairReagents));
            bettaLimsMessageTestFactory.addMessage(messageList, endRepairAbaseJaxb);
        } else {
            endRepairJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EndRepair", shearCleanPlateBarcode,
                    BettaLimsMessageTestFactory.reagentList(endRepairReagents));
            bettaLimsMessageTestFactory.addMessage(messageList, endRepairJaxb);

            endRepairCleanupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EndRepairCleanup", shearCleanPlateBarcode,
                    BettaLimsMessageTestFactory.reagentList(endRepairCleanupReagents));
            bettaLimsMessageTestFactory.addMessage(messageList, endRepairCleanupJaxb);

            aBaseJaxb = bettaLimsMessageTestFactory.buildPlateEvent("ABase", shearCleanPlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, aBaseJaxb);

            postABaseThermoCyclerLoadedJaxb =
                    bettaLimsMessageTestFactory.buildPlateEvent("PostAbaseThermoCyclerLoaded", shearCleanPlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, postABaseThermoCyclerLoadedJaxb);


            aBaseCleanupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("ABaseCleanup", shearCleanPlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, aBaseCleanupJaxb);
        }

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
                    BettaLimsMessageTestFactory.reagentList(pondEnrichmentReagents));

            bettaLimsMessageTestFactory.addMessage(messageList, pondEnrichmentJaxb);
        } else {
            indexP5PondEnrichmentJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("IndexP5PondEnrichment",
                    p5IndexPlateBarcode, ligationCleanupBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, indexP5PondEnrichmentJaxb);
        }

        postPondEnrichmentThermoCyclerLoadedJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostPondEnrichmentThermoCyclerLoaded", ligationCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, postPondEnrichmentThermoCyclerLoadedJaxb);

        String pondCleanupBarcode = ligationCleanupBarcode;
        if (pondType ==  PondType.PCR_PLUS_HYPER_PREP) {
            wgsPCRCleanupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("WGSPCRCleanup",
                    ligationCleanupBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, wgsPCRCleanupJaxb);
        } else if (pondType == PondType.CELL_FREE) {
            cellFreePCRCleanupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("CFDnaPCRSetup",
                    ligationCleanupBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, cellFreePCRCleanupJaxb);
        } else if (pondType != PondType.PCR_FREE_HYPER_PREP){
            pondCleanupBarcode = "pondCleanupPlate" + testPrefix;
            pondCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("HybSelPondEnrichmentCleanup",
                    ligationCleanupBarcode, pondCleanupBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, pondCleanupJaxb);
        }

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

        if (pondType == PondType.PCR_PLUS) {
            pondNormRackBarcode = "PondNorm" + testPrefix;
            pondNormTubeBarcodes = new ArrayList<>();
            List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
            for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
                pondNormTubeBarcodes.add("PondNorm" + testPrefix + rackPosition);
                String sourceWell = bettaLimsMessageTestFactory.buildWellName(
                        BettaLimsMessageTestFactory.NUMBER_OF_RACK_COLUMNS, rackPosition,
                        BettaLimsMessageTestFactory.WellNameType.LONG);
                cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(pondRegRackBarcode, sourceWell,
                        pondNormRackBarcode, sourceWell));
            }
            pondNormJaxb = bettaLimsMessageTestFactory.buildCherryPick("PCRPlusPondNormalization",
                    Collections.singletonList(pondRegRackBarcode), Collections.singletonList(pondRegTubeBarcodes),
                    Collections.singletonList(pondNormRackBarcode), Collections.singletonList(pondNormTubeBarcodes),
                    cherryPicks);
            for (ReceptacleType receptacleType : pondRegistrationJaxb.getPositionMap().getReceptacle()) {
                receptacleType.setVolume(new BigDecimal("50"));
            }
            bettaLimsMessageTestFactory.addMessage(messageList, pondNormJaxb);
        }
        return this;
    }
}
