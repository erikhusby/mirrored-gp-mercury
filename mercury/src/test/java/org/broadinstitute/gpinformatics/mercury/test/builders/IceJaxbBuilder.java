package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Builds JAXB objects for Illumina Content Exome messages.
 */
public class IceJaxbBuilder {

    public enum PlexType {
        PLEX12,
        PLEX96
    }

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final String pondRegRackBarcode;
    private final List<String> pondRegTubeBarcodes;
    private String baitTube1Barcode;
    private String baitTube2Barcode;
    private LibraryConstructionJaxbBuilder.TargetSystem targetSystem;
    private final PlexType plexType;

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
    private PlateCherryPickEvent ice96PlexSpriConcentration;
    private PlateTransferEventType ice1stHybridization;
    private PlateCherryPickEvent ice1stBaitPick;
    private PlateEventType postIce1stHybridizationThermoCyclerLoaded;
    private PlateTransferEventType ice1stCapture;
    private PlateEventType postIce1stCaptureThermoCyclerLoaded;
    private PlateEventType ice2ndHybridization;
    private PlateCherryPickEvent ice2ndBaitPick;
    private PlateEventType postIce2ndHybridizationThermoCyclerLoaded;
    private PlateTransferEventType ice2ndCapture;
    private PlateEventType postIce2ndCaptureThermoCyclerLoaded;
    private PlateTransferEventType iceCatchCleanup;
    private PlateEventType iceCatchEnrichmentSetup;
    private PlateEventType postIceCatchEnrichmentSetupThermoCyclerLoaded;
    private PlateTransferEventType iceCatchEnrichmentCleanup;
    private PlateTransferEventType iceCatchPico1;
    private PlateTransferEventType iceCatchPico2;
    private PlateCherryPickEvent icePoolTest;

    public IceJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            String pondRegRackBarcode, List<String> pondRegTubeBarcodes, String baitTube1Barcode,
            String baitTube2Barcode, LibraryConstructionJaxbBuilder.TargetSystem targetSystem, PlexType plexType) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.pondRegRackBarcode = pondRegRackBarcode;
        this.pondRegTubeBarcodes = pondRegTubeBarcodes;
        this.baitTube1Barcode = baitTube1Barcode;
        this.baitTube2Barcode = baitTube2Barcode;
        this.targetSystem = targetSystem;
        this.plexType = plexType;
    }

    public IceJaxbBuilder invoke() {

        // IcePoolingTransfer
        List<BettaLimsMessageTestFactory.CherryPick> poolCherryPicks = new ArrayList<>();
        poolRackBarcode = testPrefix + "IcePool";
        List<String> catchEnrichSparseTubeBarcodes = new ArrayList<>();
        switch (plexType) {
        case PLEX12:
            // pool each row into a single tube
            for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
                poolTubeBarcodes.add(i % 12 == 1 ? testPrefix + "IcePool" + (i / 12) : null);
                String sourceWell = bettaLimsMessageTestFactory.buildWellName(i,
                        BettaLimsMessageTestFactory.WellNameType.LONG);
                @SuppressWarnings({"NumericCastThatLosesPrecision"})
                String destinationWell = bettaLimsMessageTestFactory.buildWellName(
                        (((int) Math.ceil(i / 12.0) - 1) * 12) + 1, BettaLimsMessageTestFactory.WellNameType.LONG);
                poolCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(pondRegRackBarcode, sourceWell,
                        poolRackBarcode, destinationWell));
            }
            break;
        case PLEX96:
            // pool all samples into a single tube
            for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
                String sourceWell = bettaLimsMessageTestFactory.buildWellName(i,
                        BettaLimsMessageTestFactory.WellNameType.LONG);
                poolCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(pondRegRackBarcode, sourceWell,
                        poolRackBarcode, "A01"));
            }
            poolTubeBarcodes.add(testPrefix + "IcePool1");
            break;
        }
        for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
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

        switch (plexType) {
        case PLEX12:
            // IceSPRIConcentration
            spriRackBarcode = testPrefix + "SpriRack";
            iceSPRIConcentration = bettaLimsMessageTestFactory.buildRackToRack(
                    "IceSPRIConcentration", poolRackBarcode, poolTubeBarcodes, spriRackBarcode, spriTubeBarcodes);
            bettaLimsMessageTestFactory.addMessage(messageList, iceSPRIConcentration);
            break;
        case PLEX96:
            // Ice96PlexSpriConcentration
            spriRackBarcode = testPrefix + "SpriRack";
            List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
            for (int i = 0; i < 8; i++) {
                //noinspection NumericCastThatLosesPrecision
                cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(poolRackBarcode, "A01",
                        spriRackBarcode, (char)('A' + i) + "01"));
            }
            ice96PlexSpriConcentration = bettaLimsMessageTestFactory.buildCherryPick("Ice96PlexSpriConcentration",
                    Collections.singletonList(poolRackBarcode), Collections.singletonList(poolTubeBarcodes),
                    Collections.singletonList(spriRackBarcode), Collections.singletonList(spriTubeBarcodes),
                    cherryPicks);
            bettaLimsMessageTestFactory.addMessage(messageList, ice96PlexSpriConcentration);
            break;
        }

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

        // Ice1stBaitPick
        List<BettaLimsMessageTestFactory.CherryPick> bait1CherryPicks = new ArrayList<>();
        String bait1RackBarcode = "B1R" + testPrefix;
        for (int i = 0; i < 8; i++) {
            bait1CherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(bait1RackBarcode, "A01",
                    firstHybPlateBarcode, (char)('A' + i) + "01"));
        }
        ice1stBaitPick = bettaLimsMessageTestFactory.buildCherryPickToPlate("Ice1stBaitPick",
                LabEventFactory.PHYS_TYPE_TUBE_RACK, Collections.singletonList(bait1RackBarcode),
                Collections.singletonList(Collections.singletonList(baitTube1Barcode)),
                Collections.singletonList(firstHybPlateBarcode), bait1CherryPicks);
        bettaLimsMessageTestFactory.addMessage(messageList, ice1stBaitPick);

        //PostIce1stHybridizationThermoCyclerLoaded
        postIce1stHybridizationThermoCyclerLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostIce1stHybridizationThermoCyclerLoaded", firstHybPlateBarcode);
        postIce1stHybridizationThermoCyclerLoaded.setStation("WALDORF");
        bettaLimsMessageTestFactory.addMessage(messageList, postIce1stHybridizationThermoCyclerLoaded);

        // Ice1stCapture
        firstCapturePlateBarcode = testPrefix + "Ice1stCap";
        ice1stCapture = bettaLimsMessageTestFactory.buildPlateToPlate("Ice1stCapture",
                firstHybPlateBarcode, firstCapturePlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, ice1stCapture);

        //PostIce1stCaptureThermoCyclerLoaded
        postIce1stCaptureThermoCyclerLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostIce1stCaptureThermoCyclerLoaded", firstCapturePlateBarcode);
        postIce1stCaptureThermoCyclerLoaded.setStation("WALDORF");
        bettaLimsMessageTestFactory.addMessage(messageList, postIce1stCaptureThermoCyclerLoaded);

        // Ice2ndHybridization
        List<BettaLimsMessageTestFactory.ReagentDto> reagentDtos = new ArrayList<>();
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.add(Calendar.MONTH, 6);
        Date expiration = gregorianCalendar.getTime();
        if (targetSystem == LibraryConstructionJaxbBuilder.TargetSystem.MERCURY_ONLY) {
            reagentDtos.add(new BettaLimsMessageTestFactory.ReagentDto("CT3", "0009763452", expiration));
            reagentDtos.add(new BettaLimsMessageTestFactory.ReagentDto("Rapid Capture Kit bait", "0009773452",
                    expiration));
            reagentDtos.add(new BettaLimsMessageTestFactory.ReagentDto("Rapid Capture Kit Resuspension Buffer",
                    "0009783452", expiration));
        }
        ice2ndHybridization = bettaLimsMessageTestFactory.buildPlateEvent("Ice2ndHybridization",
                firstCapturePlateBarcode, reagentDtos);
        bettaLimsMessageTestFactory.addMessage(messageList, ice2ndHybridization);

        // Ice2ndBaitPick
        List<BettaLimsMessageTestFactory.CherryPick> bait2CherryPicks = new ArrayList<>();
        String bait2RackBarcode = "B2R" + testPrefix;
        for (int i = 0; i < 8; i++) {
            bait2CherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(bait2RackBarcode, "A01",
                    firstCapturePlateBarcode, (char)('A' + i) + "01"));
        }
        ice2ndBaitPick = bettaLimsMessageTestFactory.buildCherryPickToPlate("Ice2ndBaitPick",
                LabEventFactory.PHYS_TYPE_TUBE_RACK, Collections.singletonList(bait2RackBarcode),
                Collections.singletonList(Collections.singletonList(baitTube2Barcode)),
                Collections.singletonList(firstCapturePlateBarcode), bait2CherryPicks);
        bettaLimsMessageTestFactory.addMessage(messageList, ice2ndBaitPick);

        //PostIce2ndHybridizationThermoCyclerLoaded
        postIce2ndHybridizationThermoCyclerLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostIce2ndHybridizationThermoCyclerLoaded", firstCapturePlateBarcode);
        postIce1stCaptureThermoCyclerLoaded.setStation("WALDORF");
        bettaLimsMessageTestFactory.addMessage(messageList, postIce2ndHybridizationThermoCyclerLoaded);

        // Ice2ndCapture
        secondCapturePlateBarcode = testPrefix + "Ice2ndCap";
        ice2ndCapture = bettaLimsMessageTestFactory.buildPlateToPlate("Ice2ndCapture",
                firstCapturePlateBarcode, secondCapturePlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, ice2ndCapture);

        //PostIce2ndCaptureThermoCyclerLoaded
        postIce2ndCaptureThermoCyclerLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostIce2ndCaptureThermoCyclerLoaded", secondCapturePlateBarcode);
        postIce1stCaptureThermoCyclerLoaded.setStation("WALDORF");
        bettaLimsMessageTestFactory.addMessage(messageList, postIce2ndCaptureThermoCyclerLoaded);

        // IceCatchCleanup
        catchCleanupPlateBarcode = testPrefix + "IceCatchClean";
        iceCatchCleanup = bettaLimsMessageTestFactory.buildPlateToPlate("IceCatchCleanup",
                secondCapturePlateBarcode, catchCleanupPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, iceCatchCleanup);

        // IceCatchEnrichmentSetup
        reagentDtos.clear();
        if (targetSystem == LibraryConstructionJaxbBuilder.TargetSystem.MERCURY_ONLY) {
            reagentDtos.add(new BettaLimsMessageTestFactory.ReagentDto("Dual Index Primers Lot", "0009764452",
                    expiration));
            reagentDtos.add(new BettaLimsMessageTestFactory.ReagentDto("Rapid Capture Enrichment Amp Lot Barcode",
                    "0009765452", expiration));
        }
        iceCatchEnrichmentSetup = bettaLimsMessageTestFactory.buildPlateEvent("IceCatchEnrichmentSetup",
                catchCleanupPlateBarcode, reagentDtos);
        bettaLimsMessageTestFactory.addMessage(messageList, iceCatchEnrichmentSetup);

        postIceCatchEnrichmentSetupThermoCyclerLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostIceCatchEnrichmentSetupThermoCyclerLoaded", catchCleanupPlateBarcode);
        postIceCatchEnrichmentSetupThermoCyclerLoaded.setStation("WALDORF");
        bettaLimsMessageTestFactory.addMessage(messageList, postIceCatchEnrichmentSetupThermoCyclerLoaded);

        // IceCatchEnrichmentCleanup
        catchEnrichRackBarcode = testPrefix + "IceCatchEnrich";
        iceCatchEnrichmentCleanup = bettaLimsMessageTestFactory.buildPlateToRack(
                "IceCatchEnrichmentCleanup", catchCleanupPlateBarcode, catchEnrichRackBarcode,
                catchEnrichSparseTubeBarcodes);
        for (ReceptacleType receptacleType : iceCatchEnrichmentCleanup.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(new BigDecimal("50"));
        }
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

    public PlateCherryPickEvent getIce96PlexSpriConcentration() {
        return ice96PlexSpriConcentration;
    }

    public PlateTransferEventType getIce1stHybridization() {
        return ice1stHybridization;
    }

    public PlateCherryPickEvent getIce1stBaitPick() {
        return ice1stBaitPick;
    }

    public PlateEventType getPostIce1stHybridizationThermoCyclerLoaded() {
        return postIce1stHybridizationThermoCyclerLoaded;
    }

    public PlateTransferEventType getIce1stCapture() {
        return ice1stCapture;
    }

    public PlateEventType getPostIce1stCaptureThermoCyclerLoaded() {
        return postIce1stCaptureThermoCyclerLoaded;
    }

    public PlateEventType getIce2ndHybridization() {
        return ice2ndHybridization;
    }

    public PlateCherryPickEvent getIce2ndBaitPick() {
        return ice2ndBaitPick;
    }

    public PlateEventType getPostIce2ndHybridizationThermoCyclerLoaded() {
        return postIce2ndHybridizationThermoCyclerLoaded;
    }

    public PlateTransferEventType getIce2ndCapture() {
        return ice2ndCapture;
    }

    public PlateEventType getPostIce2ndCaptureThermoCyclerLoaded() {
        return postIce2ndCaptureThermoCyclerLoaded;
    }

    public PlateTransferEventType getIceCatchCleanup() {
        return iceCatchCleanup;
    }

    public PlateEventType getIceCatchEnrichmentSetup() {
        return iceCatchEnrichmentSetup;
    }

    public PlateEventType getPostIceCatchEnrichmentSetupThermoCyclerLoaded() {
        return postIceCatchEnrichmentSetupThermoCyclerLoaded;
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
