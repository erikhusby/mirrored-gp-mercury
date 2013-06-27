package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for library construction messages
 */
public class LibraryConstructionJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String shearCleanPlateBarcode;

    private final String p7IndexPlateBarcode;
    private final String p5IndexPlateBarcode;
    private String pondRegRackBarcode;
    private List<String> pondRegTubeBarcodes;

    private PlateEventType         endRepairJaxb;
    private PlateEventType endRepairCleanupJaxb;
    private PlateEventType aBaseJaxb;
    private PlateEventType aBaseCleanupJaxb;
    private PlateTransferEventType indexedAdapterLigationJaxb;
    private PlateTransferEventType ligationCleanupJaxb;
    private PlateEventType pondEnrichmentJaxb;
    private PlateTransferEventType indexP5PondEnrichmentJaxb;
    private PlateTransferEventType pondCleanupJaxb;
    private PlateTransferEventType pondRegistrationJaxb;
    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
    private int numSamples;

    public LibraryConstructionJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            String shearCleanPlateBarcode, String p7IndexPlateBarcode, String p5IndexPlateBarcode, int numSamples) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.shearCleanPlateBarcode = shearCleanPlateBarcode;
        this.p7IndexPlateBarcode = p7IndexPlateBarcode;
        this.p5IndexPlateBarcode = p5IndexPlateBarcode;
        this.numSamples = numSamples;
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

    public LibraryConstructionJaxbBuilder invoke() {
        endRepairJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EndRepair", shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, endRepairJaxb);

        endRepairCleanupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("EndRepairCleanup", shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, endRepairCleanupJaxb);

        aBaseJaxb = bettaLimsMessageTestFactory.buildPlateEvent("ABase", shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, aBaseJaxb);

        aBaseCleanupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("ABaseCleanup", shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, aBaseCleanupJaxb);

//            indexPlateBarcode = "IndexPlate" + testPrefix;
        indexedAdapterLigationJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("IndexedAdapterLigation",
                p7IndexPlateBarcode,
                shearCleanPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, indexedAdapterLigationJaxb);

        String ligationCleanupBarcode = "ligationCleanupPlate" + testPrefix;
        ligationCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("AdapterLigationCleanup",
                shearCleanPlateBarcode,
                ligationCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, ligationCleanupJaxb);

        if (p5IndexPlateBarcode == null) {
            pondEnrichmentJaxb = bettaLimsMessageTestFactory.buildPlateEvent("PondEnrichment", ligationCleanupBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, pondEnrichmentJaxb);
        } else {
            indexP5PondEnrichmentJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("IndexP5PondEnrichment",
                    p5IndexPlateBarcode, ligationCleanupBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, indexP5PondEnrichmentJaxb);
        }

        String pondCleanupBarcode = "pondCleanupPlate" + testPrefix;
        pondCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("HybSelPondEnrichmentCleanup",
                ligationCleanupBarcode, pondCleanupBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, pondCleanupJaxb);

        pondRegRackBarcode = "PondReg" + testPrefix;
        pondRegTubeBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            pondRegTubeBarcodes.add(LabEventTest.POND_REGISTRATION_TUBE_PREFIX + testPrefix + rackPosition);
        }
        pondRegistrationJaxb = bettaLimsMessageTestFactory.buildPlateToRack("PondRegistration", pondCleanupBarcode,
                pondRegRackBarcode, pondRegTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, pondRegistrationJaxb);

        return this;
    }
}
