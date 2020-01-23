package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleCellVDJJaxbBuilder {

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String tcrRackBarcode;
    private final List<String> tcrTubeBarcodes;
    private final String bcrRackBarcode;
    private final List<String> bcrTubeBarcodes;
    private final String indexPlateBarcode;
    private PlateTransferEventType vdjEndRepairAbase;
    private PlateTransferEventType vdjAdapterLigation;
    private PlateTransferEventType vdjLigationCleanupJaxb;
    private PlateTransferEventType vdjIndexAdapterPCRJaxb;
    private List<String> vdjSpriTubeBarcodes;
    private PlateTransferEventType singleSidedSpriJaxb;

    private Map<String, StationEventType> mapEventToJax = new HashMap<>();

    public SingleCellVDJJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                    String tcrRackBarcode, List<String> tcrTubeBarcodes, String bcrRackBarcode, List<String> bcrTubeBarcodes,
                                    String indexPlateBarcode) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.tcrRackBarcode = tcrRackBarcode;
        this.tcrTubeBarcodes = tcrTubeBarcodes;
        this.bcrRackBarcode = bcrRackBarcode;
        this.bcrTubeBarcodes = bcrTubeBarcodes;
        this.indexPlateBarcode = indexPlateBarcode;
    }

    public SingleCellVDJJaxbBuilder invoke() {
        List<String> tcrTubes = buildLCEvents("Tcr", tcrRackBarcode, tcrTubeBarcodes);
        List<String> bcrTubes = buildLCEvents("Bcr", bcrRackBarcode, bcrTubeBarcodes);

        List<String> consolidatedTubes = tcrTubes.subList(0, tcrTubes.size() / 2);
        consolidatedTubes.addAll(bcrTubes.subList(0, tcrTubes.size() / 2));

        String consolidatedRackBarcode = "ConsolidatedVdjRack" + testPrefix;
        String aTailPlate = "VdjaTailPlate" + testPrefix;
        vdjEndRepairAbase = bettaLimsMessageTestFactory.buildRackToPlate("VdjEndRepairABase",
                consolidatedRackBarcode, consolidatedTubes, aTailPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, vdjEndRepairAbase);

        String vdjLigationPlate = "vdjLigationPlate" + testPrefix;
        vdjAdapterLigation = bettaLimsMessageTestFactory.buildPlateToPlate("VdjAdapterLigation",
                aTailPlate,
                vdjLigationPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, vdjAdapterLigation);

        String ligationCleanupPlate = "vdjligationCleanupPlate" + testPrefix;
        vdjLigationCleanupJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("VdjLigationCleanup",
                vdjLigationPlate,
                ligationCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, vdjLigationCleanupJaxb);

        vdjIndexAdapterPCRJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("SingleCellIndexAdapterPCR",
                indexPlateBarcode,
                ligationCleanupPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, vdjIndexAdapterPCRJaxb);

        String vdjRack = "vdjSingleSidedSpriRack" + testPrefix;
        vdjSpriTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < tcrTubeBarcodes.size(); i++) {
            vdjSpriTubeBarcodes.add("SCSpriTube_" + i);
        }
        singleSidedSpriJaxb = bettaLimsMessageTestFactory.buildPlateToRack("SingleCellDoubleSidedCleanup",
                ligationCleanupPlate, vdjRack, vdjSpriTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, singleSidedSpriJaxb);

        return this;
    }

    private List<String> buildLCEvents(String eventPrefix, String sourceRackBarcode, List<String> sourceTubeBarcodes) {
        String pcr1Plate = eventPrefix + "pcr1Plate" + testPrefix;
        String eventType = eventPrefix + "Pcr1";
        PlateTransferEventType pcr1Jaxb = bettaLimsMessageTestFactory.buildRackToPlate(eventType,
                sourceRackBarcode, sourceTubeBarcodes, pcr1Plate);
        bettaLimsMessageTestFactory.addMessage(messageList, pcr1Jaxb);
        mapEventToJax.put(eventType, pcr1Jaxb);

        String spri1Plate = eventPrefix + "spri1Plate" + testPrefix;
        eventType = eventPrefix + "Spri1";
        PlateTransferEventType spri1Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(eventType,
                pcr1Plate, spri1Plate);
        bettaLimsMessageTestFactory.addMessage(messageList, spri1Jaxb);
        mapEventToJax.put(eventType, spri1Jaxb);

        String pcr2Plate = eventPrefix + "pcr2Plate" + testPrefix;
        eventType = eventPrefix + "Pcr2";
        PlateTransferEventType pcr2Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(eventType,
                spri1Plate, pcr2Plate);
        bettaLimsMessageTestFactory.addMessage(messageList, pcr2Jaxb);
        mapEventToJax.put(eventType, pcr2Jaxb);

        String spri2Plate = eventPrefix + "spri2Plate" + testPrefix;
        eventType =  eventPrefix + "Spri2";
        PlateTransferEventType spri2Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate(eventType,
                pcr2Plate, spri2Plate);
        bettaLimsMessageTestFactory.addMessage(messageList, spri2Jaxb);
        mapEventToJax.put(eventType, spri2Jaxb);

        String spriRack = "vdjRegRack" + testPrefix;
        List<String> consolidationTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < sourceTubeBarcodes.size(); i++) {
            consolidationTubeBarcodes.add(eventPrefix + "VdjRegTube_" + i);
        }
        eventType = eventPrefix + "Registration";
        PlateTransferEventType registrationJaxb =
                bettaLimsMessageTestFactory.buildPlateToRack(eventType,
                        spri2Plate, spriRack, consolidationTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, registrationJaxb);
        mapEventToJax.put(eventType, registrationJaxb);

        return consolidationTubeBarcodes;
    }

    public StationEventType getJaxbFromName(String eventName) {
        return mapEventToJax.get(eventName);
    }
}
