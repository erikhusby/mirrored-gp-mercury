package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Builds JAXB objects for Illumina Content Exome messages.
 */
public class IceJaxbBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String pondRegRackBarcode;
    private final List<String> pondRegTubeBarcodes;
    private String baitTube1Barcode;
    private String baitTube2Barcode;

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private String poolRackBarcode;
    private List<String> poolTubeBarcodes = new ArrayList<>();
    private String poolTestRackBarcode;
    private String poolTestTubeBarcode;
    private String spriRackBarcode;
    private List<String> spriTubeBarcodes = new ArrayList<>();
    private String firstHybPlateBarcode;
    private String firstCapturePlateBarcode;
    private String secondCapturePlateBarcode;
    private String catchCleanupPlateBarcode;
    private String catchEnrichRackBarcode;
    private List<String> catchEnrichTubeBarcodes = new ArrayList<>();
    private String catchPico1Barcode;
    private String catchPico2Barcode;
    private PlateCherryPickEvent icePoolingTransfer;
    private PlateTransferEventType iceSPRIConcentration;
    private PlateTransferEventType ice1stHybridization;
    private ReceptaclePlateTransferEvent ice1stBaitAddition;
    private PlateTransferEventType ice1stCapture;
    private PlateEventType ice2ndHybridization;
    private ReceptaclePlateTransferEvent ice2ndBaitAddition;
    private PlateTransferEventType ice2ndCapture;
    private PlateTransferEventType iceCatchCleanup;
    private PlateEventType iceCatchEnrichmentSetup;
    private PlateTransferEventType iceCatchEnrichmentCleanup;
    private PlateTransferEventType iceCatchPico1;
    private PlateTransferEventType iceCatchPico2;
    private PlateCherryPickEvent icePoolTest;

    public IceJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            String pondRegRackBarcode, List<String> pondRegTubeBarcodes,
            String baitTube1Barcode, String baitTube2Barcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.pondRegRackBarcode = pondRegRackBarcode;
        this.pondRegTubeBarcodes = pondRegTubeBarcodes;
        this.baitTube1Barcode = baitTube1Barcode;
        this.baitTube2Barcode = baitTube2Barcode;
    }

    public IceJaxbBuilder invoke() {

        // IcePoolingTransfer, pool each row into a single tube
        List<BettaLimsMessageTestFactory.CherryPick> poolCherryPicks = new ArrayList<>();
        poolRackBarcode = testPrefix + "IcePool";
        for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
            String sourceWell = bettaLimsMessageTestFactory.buildWellName(i,
                    BettaLimsMessageTestFactory.WellNameType.LONG);
            @SuppressWarnings({"NumericCastThatLosesPrecision"})
            String destinationWell = bettaLimsMessageTestFactory.buildWellName(
                    (((int) Math.ceil(i / 12.0) - 1) * 12) + 1, BettaLimsMessageTestFactory.WellNameType.LONG);
            poolCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(pondRegRackBarcode, sourceWell, poolRackBarcode,
                    destinationWell));
        }
        List<String> catchEnrichSparseTubeBarcodes = new ArrayList<>();
        for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
            poolTubeBarcodes.add(i % 12 == 1 ? testPrefix + "IcePool" + (i / 12) : null);
            spriTubeBarcodes.add(i % 12 == 1 ? testPrefix + "IceSpri" + (i / 12) : null);
            catchEnrichSparseTubeBarcodes.add(i % 12 == 1 ? testPrefix + "IceCatchEnrich" + (i / 12) : null);
            if (i % 12 == 1) {
                catchEnrichTubeBarcodes.add(testPrefix + "IceCatchEnrich" + (i / 12));
            }
        }
        icePoolingTransfer = bettaLimsMessageTestFactory.buildCherryPick("IcePoolingTransfer",
                Collections.singletonList(pondRegRackBarcode), Collections.singletonList(pondRegTubeBarcodes),
                Collections.singletonList(poolRackBarcode), Collections.singletonList(poolTubeBarcodes), poolCherryPicks);
        bettaLimsMessageTestFactory.addMessage(messageList, icePoolingTransfer);

        // IceSPRIConcentration
        spriRackBarcode = testPrefix + "SpriRack";
        iceSPRIConcentration = bettaLimsMessageTestFactory.buildRackToRack(
                "IceSPRIConcentration", poolRackBarcode, poolTubeBarcodes, spriRackBarcode, spriTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, iceSPRIConcentration);

        // IcePoolTest
        poolTestRackBarcode = testPrefix + "IcePoolTestRack";
        poolTestTubeBarcode = testPrefix + "IcePoolTest";
        List<BettaLimsMessageTestFactory.CherryPick> poolTestCherryPicks = new ArrayList<>();
        for (int i = 0; i < spriTubeBarcodes.size(); i++) {
            if (spriTubeBarcodes.get(i) != null) {
                poolTestCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(spriRackBarcode,
                        bettaLimsMessageTestFactory.buildWellName(i + 1, BettaLimsMessageTestFactory.WellNameType.LONG),
                        poolTestRackBarcode, "A01"));
            }
        }
        icePoolTest = bettaLimsMessageTestFactory.buildCherryPick("IcePoolTest",
                Collections.singletonList(spriRackBarcode), Collections.singletonList(spriTubeBarcodes),
                Collections.singletonList(poolTestRackBarcode),
                Collections.singletonList(Collections.singletonList(poolTestTubeBarcode)), poolTestCherryPicks);
        bettaLimsMessageTestFactory.addMessage(messageList, icePoolTest);

        // todo jmt IcePoolTest is followed by NormalizationTransfer, DenatureTransfer, MiSeq?

        // Ice1stHybridization
        firstHybPlateBarcode = testPrefix + "1stHyb";
        ice1stHybridization = bettaLimsMessageTestFactory.buildRackToPlate(
                "Ice1stHybridization", spriRackBarcode, spriTubeBarcodes, firstHybPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, ice1stHybridization);

        // Ice1stBaitAddition
        ice1stBaitAddition = bettaLimsMessageTestFactory.buildTubeToPlate(
                "Ice1stBaitAddition", baitTube1Barcode, firstHybPlateBarcode, LabEventFactory.PHYS_TYPE_EPPENDORF_96,
                LabEventFactory.SECTION_ALL_96, "tube");
        bettaLimsMessageTestFactory.addMessage(messageList, ice1stBaitAddition);

        // Ice1stCapture
        firstCapturePlateBarcode = testPrefix + "Ice1stCap";
        ice1stCapture = bettaLimsMessageTestFactory.buildPlateToPlate("Ice1stCapture",
                firstHybPlateBarcode, firstCapturePlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, ice1stCapture);

        // Ice2ndHybridization
        List<BettaLimsMessageTestFactory.ReagentDto> reagentDtos = Arrays.asList(
                new BettaLimsMessageTestFactory.ReagentDto("CT3", "0009763452", new Date()),
                new BettaLimsMessageTestFactory.ReagentDto("Rapid Capture Kit bait", "0009773452", new Date()),
                new BettaLimsMessageTestFactory.ReagentDto("Rapid Capture Kit Resuspension Buffer", "0009783452", new Date()));
        ice2ndHybridization = bettaLimsMessageTestFactory.buildPlateEvent("Ice2ndHybridization",
                firstCapturePlateBarcode, reagentDtos);
        bettaLimsMessageTestFactory.addMessage(messageList, ice2ndHybridization);

        ice2ndBaitAddition = bettaLimsMessageTestFactory
                .buildTubeToPlate("Ice2ndBaitAddition", baitTube2Barcode, firstCapturePlateBarcode,
                        LabEventFactory.PHYS_TYPE_EPPENDORF_96, LabEventFactory.SECTION_ALL_96, "tube");
        bettaLimsMessageTestFactory.addMessage(messageList, ice2ndBaitAddition);

        // Ice2ndCapture
        secondCapturePlateBarcode = testPrefix + "Ice2ndCap";
        ice2ndCapture = bettaLimsMessageTestFactory.buildPlateToPlate("Ice2ndCapture",
                firstCapturePlateBarcode, secondCapturePlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, ice2ndCapture);

        // IceCatchCleanup
        catchCleanupPlateBarcode = testPrefix + "IceCatchClean";
        iceCatchCleanup = bettaLimsMessageTestFactory.buildPlateToPlate("IceCatchCleanup",
                secondCapturePlateBarcode, catchCleanupPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, iceCatchCleanup);

        // IceCatchEnrichmentSetup
        List<BettaLimsMessageTestFactory.ReagentDto> reagentDtos1 = Arrays.asList(
                new BettaLimsMessageTestFactory.ReagentDto("Dual Index Primers Lot", "0009764452", new Date()),
                new BettaLimsMessageTestFactory.ReagentDto("Rapid Capture Enrichment Amp Lot Barcode", "0009765452", new Date()));
        iceCatchEnrichmentSetup = bettaLimsMessageTestFactory.buildPlateEvent("IceCatchEnrichmentSetup",
                catchCleanupPlateBarcode, reagentDtos1);
        bettaLimsMessageTestFactory.addMessage(messageList, iceCatchEnrichmentSetup);

        // IceCatchEnrichmentCleanup
        catchEnrichRackBarcode = testPrefix + "IceCatchEnrich";
        iceCatchEnrichmentCleanup = bettaLimsMessageTestFactory.buildPlateToRack(
                "IceCatchEnrichmentCleanup", catchCleanupPlateBarcode, catchEnrichRackBarcode,
                catchEnrichSparseTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, iceCatchEnrichmentCleanup);

        // IceCatchPico
        // Pico plate barcodes must be all numeric to be accepted by the Varioskan parser
        catchPico1Barcode = testPrefix + "881";
        iceCatchPico1 = bettaLimsMessageTestFactory.buildRackToPlate("IceCatchPico",
                catchEnrichRackBarcode, catchEnrichSparseTubeBarcodes, catchPico1Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, iceCatchPico1);
        catchPico2Barcode = testPrefix + "882";
        iceCatchPico2 = bettaLimsMessageTestFactory.buildRackToPlate("IceCatchPico",
                catchEnrichRackBarcode, catchEnrichSparseTubeBarcodes, catchPico2Barcode);
        bettaLimsMessageTestFactory.addMessage(messageList, iceCatchPico2);

        return this;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getPoolRackBarcode() {
        return poolRackBarcode;
    }

    public List<String> getPoolTubeBarcodes() {
        return poolTubeBarcodes;
    }

    public String getSpriRackBarcode() {
        return spriRackBarcode;
    }

    public List<String> getSpriTubeBarcodes() {
        return spriTubeBarcodes;
    }

    public String getFirstHybPlateBarcode() {
        return firstHybPlateBarcode;
    }

    public String getFirstCapturePlateBarcode() {
        return firstCapturePlateBarcode;
    }

    public String getSecondCapturePlateBarcode() {
        return secondCapturePlateBarcode;
    }

    public String getCatchCleanupPlateBarcode() {
        return catchCleanupPlateBarcode;
    }

    public String getCatchEnrichRackBarcode() {
        return catchEnrichRackBarcode;
    }

    public List<String> getCatchEnrichTubeBarcodes() {
        return catchEnrichTubeBarcodes;
    }

    public String getCatchPico1Barcode() {
        return catchPico1Barcode;
    }

    public String getCatchPico2Barcode() {
        return catchPico2Barcode;
    }

    public PlateCherryPickEvent getIcePoolingTransfer() {
        return icePoolingTransfer;
    }

    public PlateCherryPickEvent getIcePoolTest() {
        return icePoolTest;
    }

    public PlateTransferEventType getIceSPRIConcentration() {
        return iceSPRIConcentration;
    }

    public PlateTransferEventType getIce1stHybridization() {
        return ice1stHybridization;
    }

    public ReceptaclePlateTransferEvent getIce1stBaitAddition() {
        return ice1stBaitAddition;
    }

    public PlateTransferEventType getIce1stCapture() {
        return ice1stCapture;
    }

    public PlateEventType getIce2ndHybridization() {
        return ice2ndHybridization;
    }

    public ReceptaclePlateTransferEvent getIce2ndBaitAddition() {
        return ice2ndBaitAddition;
    }

    public PlateTransferEventType getIce2ndCapture() {
        return ice2ndCapture;
    }

    public PlateTransferEventType getIceCatchCleanup() {
        return iceCatchCleanup;
    }

    public PlateEventType getIceCatchEnrichmentSetup() {
        return iceCatchEnrichmentSetup;
    }

    public PlateTransferEventType getIceCatchEnrichmentCleanup() {
        return iceCatchEnrichmentCleanup;
    }

    public PlateTransferEventType getIceCatchPico1() {
        return iceCatchPico1;
    }

    public String getBaitTube1Barcode() {
        return baitTube1Barcode;
    }

    public String getBaitTube2Barcode() {
        return baitTube2Barcode;
    }
}
