package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for 10X messages.
 */
public class TenXJaxbBuilder {
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final List<String> tubeBarcodeList;
    private final String testPrefix;
    private String rackBarcode;
    private PlateTransferEventType tenXMakePlateJaxb;
    private List<PlateTransferEventType> chipLoadingJaxb = new ArrayList<>();
    private List<PlateTransferEventType> chipUnloadingJaxb = new ArrayList<>();
    private PlateEventType emulsionBreakingJaxb;
    private PlateTransferEventType dynabeadCleanupJaxb;
    private PlateTransferEventType preLCSpriJaxb;

    public TenXJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, List<String> tubeBarcodeList, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.tubeBarcodeList = tubeBarcodeList;
        this.testPrefix = testPrefix;
    }

    public TenXJaxbBuilder invoke() {
        rackBarcode = "10X" + testPrefix;
        String makePlate = testPrefix + "10XMakePlate";

        tenXMakePlateJaxb = bettaLimsMessageTestFactory.buildRackToPlate("10XMakePlate", rackBarcode,
                tubeBarcodeList, makePlate);

        bettaLimsMessageTestFactory.addMessage(messageList, tenXMakePlateJaxb);

        // Chip Loading - 1 column per chip
        List<String> chipBarcodes = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            String section = "P96_COL" + i;
            String chipBarcode = testPrefix + "10XChip" + i;
            PlateTransferEventType chipLoadingEvent = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "10XChipLoading", makePlate, chipBarcode);
            chipLoadingEvent.getSourcePlate().setSection(section);
            chipLoadingEvent.getPlate().setSection("ALL8");
            chipLoadingEvent.getPlate().setPhysType("10XChip");
            chipBarcodes.add(chipBarcode);
            chipLoadingJaxb.add(chipLoadingEvent);
            bettaLimsMessageTestFactory.addMessage(messageList, chipLoadingEvent);
        }

        // Chip Unloading
        String chipUnloadPlate = testPrefix + "10XUnloadPlate";
        for (int i = 1; i <= 12; i++) {
            String section = "P96_COL" + i;
            String chipBarcode = chipBarcodes.get(i - 1);
            PlateTransferEventType chipUnloadingJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "10XChipUnloading", chipBarcode, chipUnloadPlate);
            chipUnloadingJaxb.getSourcePlate().setSection("ALL8");
            chipUnloadingJaxb.getPlate().setSection(section);
            chipUnloadingJaxb.getSourcePlate().setPhysType("10XChip");
            this.chipUnloadingJaxb.add(chipUnloadingJaxb);
            bettaLimsMessageTestFactory.addMessage(messageList, chipUnloadingJaxb);
        }

        // Emulsion breaking
        emulsionBreakingJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "10XEmulsionBreaking", chipUnloadPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, emulsionBreakingJaxb);

        // Dynabead cleanup
        String cleanupPlate = testPrefix + "10XCleanupPlate";
        dynabeadCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                "10XDynabeadCleanup", chipUnloadPlate, cleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, dynabeadCleanupJaxb);

        // Pre LC Sprip
        String preLCPlate = testPrefix + "10XPreLCPlate";
        preLCSpriJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                "10XPreLCSpri", cleanupPlate, preLCPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, preLCSpriJaxb);

        return this;
    }

    public PlateTransferEventType getTenXMakePlateJaxb() {
        return tenXMakePlateJaxb;
    }

    public List<PlateTransferEventType> getChipLoadingJaxb() {
        return chipLoadingJaxb;
    }

    public List<PlateTransferEventType> getChipUnloadingJaxb() {
        return chipUnloadingJaxb;
    }

    public PlateEventType getEmulsionBreakingJaxb() {
        return emulsionBreakingJaxb;
    }

    public PlateTransferEventType getDynabeadCleanupJaxb() {
        return dynabeadCleanupJaxb;
    }

    public PlateTransferEventType getPreLCSpriJaxb() {
        return preLCSpriJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
