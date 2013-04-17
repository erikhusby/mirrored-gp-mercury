package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for Hybrid Selection messages
 */
public class HybridSelectionJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String pondRegRackBarcode;
    private final List<String>                pondRegTubeBarcodes;
    private String normCatchRackBarcode;
    private List<String> normCatchBarcodes;
    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();

    private PlateTransferEventType       preSelPoolJaxb;
    private PlateTransferEventType preSelPoolJaxb2;
    private PlateTransferEventType hybridizationJaxb;
    private String baitTubeBarcode;
    private ReceptaclePlateTransferEvent baitSetupJaxb;
    private PlateTransferEventType baitAdditionJaxb;
    private PlateEventType               beadAdditionJaxb;
    private PlateEventType apWashJaxb;
    private PlateEventType gsWash1Jaxb;
    private PlateEventType gsWash2Jaxb;
    private PlateEventType gsWash3Jaxb;
    private PlateEventType gsWash4Jaxb;
    private PlateEventType gsWash5Jaxb;
    private PlateEventType gsWash6Jaxb;
    private PlateEventType catchEnrichmentSetupJaxb;
    private PlateTransferEventType catchEnrichmentCleanupJaxb;
    private PlateTransferEventType normCatchJaxb;

    public HybridSelectionJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                      String pondRegRackBarcode, List<String> pondRegTubeBarcodes,
                                      String baitTubeBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.pondRegRackBarcode = pondRegRackBarcode;
        this.pondRegTubeBarcodes = pondRegTubeBarcodes;
        this.baitTubeBarcode = baitTubeBarcode;
    }

    public PlateTransferEventType getPreSelPoolJaxb() {
        return preSelPoolJaxb;
    }

    public PlateTransferEventType getPreSelPoolJaxb2() {
        return preSelPoolJaxb2;
    }

    public PlateTransferEventType getHybridizationJaxb() {
        return hybridizationJaxb;
    }

    public String getBaitTubeBarcode() {
        return baitTubeBarcode;
    }

    public ReceptaclePlateTransferEvent getBaitSetupJaxb() {
        return baitSetupJaxb;
    }

    public PlateTransferEventType getBaitAdditionJaxb() {
        return baitAdditionJaxb;
    }

    public PlateEventType getBeadAdditionJaxb() {
        return beadAdditionJaxb;
    }

    public PlateEventType getApWashJaxb() {
        return apWashJaxb;
    }

    public PlateEventType getGsWash1Jaxb() {
        return gsWash1Jaxb;
    }

    public PlateEventType getGsWash2Jaxb() {
        return gsWash2Jaxb;
    }

    public PlateEventType getGsWash3Jaxb() {
        return gsWash3Jaxb;
    }

    public PlateEventType getGsWash4Jaxb() {
        return gsWash4Jaxb;
    }

    public PlateEventType getGsWash5Jaxb() {
        return gsWash5Jaxb;
    }

    public PlateEventType getGsWash6Jaxb() {
        return gsWash6Jaxb;
    }

    public PlateEventType getCatchEnrichmentSetupJaxb() {
        return catchEnrichmentSetupJaxb;
    }

    public PlateTransferEventType getCatchEnrichmentCleanupJaxb() {
        return catchEnrichmentCleanupJaxb;
    }

    public PlateTransferEventType getNormCatchJaxb() {
        return normCatchJaxb;
    }

    public String getNormCatchRackBarcode() {
        return normCatchRackBarcode;
    }

    public List<String> getNormCatchBarcodes() {
        return normCatchBarcodes;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public HybridSelectionJaxbBuilder invoke() {
        List<String> preSelPoolBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= pondRegTubeBarcodes.size() / 2; rackPosition++) {
            preSelPoolBarcodes.add("PreSelPool" + testPrefix + rackPosition);
        }
        String preSelPoolRackBarcode = "PreSelPool" + testPrefix;
        preSelPoolJaxb = bettaLimsMessageTestFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                pondRegTubeBarcodes.subList(0, pondRegTubeBarcodes
                        .size() / 2), preSelPoolRackBarcode,
                preSelPoolBarcodes);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, preSelPoolJaxb);

        preSelPoolJaxb2 = bettaLimsMessageTestFactory.buildRackToRack("PreSelectionPool", pondRegRackBarcode,
                pondRegTubeBarcodes.subList(
                        pondRegTubeBarcodes.size() / 2,
                        pondRegTubeBarcodes.size()),
                preSelPoolRackBarcode, preSelPoolBarcodes);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, preSelPoolJaxb2);

        String hybridizationPlateBarcode = "Hybrid" + testPrefix;
        hybridizationJaxb = bettaLimsMessageTestFactory.buildRackToPlate("Hybridization", preSelPoolRackBarcode,
                preSelPoolBarcodes, hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, hybridizationJaxb);

        String baitSetupBarcode = "BaitSetup" + testPrefix;
        baitSetupJaxb = bettaLimsMessageTestFactory.buildTubeToPlate("BaitSetup", baitTubeBarcode, baitSetupBarcode,
                LabEventFactory.PHYS_TYPE_EPPENDORF_96,
                LabEventFactory.SECTION_ALL_96, "tube");
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, baitSetupJaxb);

        baitAdditionJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("BaitAddition", baitSetupBarcode,
                hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, baitAdditionJaxb);

        beadAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("BeadAddition", hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, beadAdditionJaxb);

        apWashJaxb = bettaLimsMessageTestFactory.buildPlateEvent("APWash", hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, apWashJaxb);

        gsWash1Jaxb = bettaLimsMessageTestFactory.buildPlateEvent("GSWash1", hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, gsWash1Jaxb);

        gsWash2Jaxb = bettaLimsMessageTestFactory.buildPlateEvent("GSWash2", hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, gsWash2Jaxb);

        gsWash3Jaxb = bettaLimsMessageTestFactory.buildPlateEvent("GSWash3", hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, gsWash3Jaxb);

        gsWash4Jaxb = bettaLimsMessageTestFactory.buildPlateEvent("GSWash4", hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, gsWash4Jaxb);

        gsWash5Jaxb = bettaLimsMessageTestFactory.buildPlateEvent("GSWash5", hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, gsWash5Jaxb);

        gsWash6Jaxb = bettaLimsMessageTestFactory.buildPlateEvent("GSWash6", hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, gsWash6Jaxb);

        catchEnrichmentSetupJaxb = bettaLimsMessageTestFactory.buildPlateEvent("CatchEnrichmentSetup",
                hybridizationPlateBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, catchEnrichmentSetupJaxb);

        String catchCleanupBarcode = "catchCleanPlate" + testPrefix;
        catchEnrichmentCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("CatchEnrichmentCleanup",
                hybridizationPlateBarcode,
                catchCleanupBarcode);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, catchEnrichmentCleanupJaxb);

        normCatchBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= pondRegTubeBarcodes.size() / 2; rackPosition++) {
            normCatchBarcodes.add("NormCatch" + testPrefix + rackPosition);
        }
        normCatchRackBarcode = "NormCatchRack";
        normCatchJaxb = bettaLimsMessageTestFactory.buildPlateToRack("NormalizedCatchRegistration",
                hybridizationPlateBarcode, normCatchRackBarcode,
                normCatchBarcodes);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, normCatchJaxb);

        return this;
    }
}
