package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Builds JAXB objects for Illumina Content Exome messages.
 */
public class IceJaxbBuilder {

    public static final String ICE_CATCH_ENRICH = "IceCatchEnrich";

    public enum PlexType {
        PLEX12,
        PLEX96
    }

    public enum PrepType {
        ICE,
        HYPER_PREP_ICE
    }

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> pondRegRackBarcodes;
    private final List<List<String>> listPondRegTubeBarcodes;
    private String baitTube1Barcode;
    private String baitTube2Barcode;
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
    private List<String> catchEnrichSparseTubeBarcodes = new ArrayList<>();
    private String catchPico1Barcode;
    private String catchPico2Barcode;
    private PlateCherryPickEvent icePoolingTransfer;
    private PlateTransferEventType iceSPRIConcentration;
    private PlateCherryPickEvent ice96PlexSpriConcentration;
    private PlateTransferEventType ice1stHybridization;
    private ReceptaclePlateTransferEvent ice1stBaitAddition;
    private PlateCherryPickEvent ice1stBaitPick;
    private PlateEventType postIce1stHybridizationThermoCyclerLoaded;
    private PlateTransferEventType ice1stCapture;
    private PlateEventType postIce1stCaptureThermoCyclerLoaded;
    private PlateEventType ice2ndHybridization;
    private ReceptaclePlateTransferEvent ice2ndBaitAddition;
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
    private List<Triple<String, String, Integer>> ice2ndHybReagents;
    private List<Triple<String, String, Integer>> iceCatchEnrichmentSetupReagents;
    private PrepType prepType;
    private PlateTransferEventType iceHyperPrep1stBaitAddition;
    private PlateTransferEventType iceHyperPrep2ndBaitAddition;

    /** Constructor for ICE or HYPER_PREP_ICE. */
    public IceJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            List<String> pondRegRackBarcodes, List<List<String>> listPondRegTubeBarcodes, String baitTube1Barcode,
            String baitTube2Barcode, PlexType plexType,
            List<Triple<String, String, Integer>> ice2ndHybReagentTypeLotYearOffset,
            List<Triple<String, String, Integer>> iceCatchEnrichmentSetupReagentTypeLotOffset, PrepType prepType) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.pondRegRackBarcodes = pondRegRackBarcodes;
        this.listPondRegTubeBarcodes = listPondRegTubeBarcodes;
        this.baitTube1Barcode = baitTube1Barcode;
        this.baitTube2Barcode = baitTube2Barcode;
        this.plexType = plexType;
        this.ice2ndHybReagents = ice2ndHybReagentTypeLotYearOffset;
        this.iceCatchEnrichmentSetupReagents = iceCatchEnrichmentSetupReagentTypeLotOffset;
        this.prepType = prepType;
    }

    /** Defaults to ICE. */
    public IceJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            List<String> pondRegRackBarcodes, List<List<String>> listPondRegTubeBarcodes, String baitTube1Barcode,
            String baitTube2Barcode, PlexType plexType,
            List<Triple<String, String, Integer>> ice2ndHybReagentTypeLotYearOffset,
            List<Triple<String, String, Integer>> iceCatchEnrichmentSetupReagentTypeLotOffset) {
        this(bettaLimsMessageTestFactory, testPrefix, pondRegRackBarcodes, listPondRegTubeBarcodes,
                baitTube1Barcode, baitTube2Barcode, plexType, ice2ndHybReagentTypeLotYearOffset,
                iceCatchEnrichmentSetupReagentTypeLotOffset, PrepType.ICE);
    }

    public IceJaxbBuilder invoke() {

        // IcePoolingTransfer
        List<BettaLimsMessageTestFactory.CherryPick> poolCherryPicks = new ArrayList<>();
        poolRackBarcode = testPrefix + "IcePool";
        switch (plexType) {
        case PLEX12:
            // todo jmt this doesn't work for multiple LCSETs
            for (int j = 0; j < listPondRegTubeBarcodes.size(); j++) {
                List<String> pondRegTubeBarcodes = listPondRegTubeBarcodes.get(j);
                // pool each row into a single tube
                for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
                    poolTubeBarcodes.add(i % 12 == 1 ? testPrefix + "IcePool" + (i / 12) : null);
                    String sourceWell = bettaLimsMessageTestFactory.buildWellName(i,
                            BettaLimsMessageTestFactory.WellNameType.SHORT);
                    @SuppressWarnings({"NumericCastThatLosesPrecision"})
                    String destinationWell = bettaLimsMessageTestFactory.buildWellName(
                            (((int) Math.ceil(i / 12.0) - 1) * 12) + 1, BettaLimsMessageTestFactory.WellNameType.SHORT);
                    poolCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(pondRegRackBarcodes.get(j), sourceWell,
                            poolRackBarcode, destinationWell));
                }
                for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
                    spriTubeBarcodes.add(i % 12 == 1 ? testPrefix + "IceSpri" + (i / 12) : null);
                    catchEnrichSparseTubeBarcodes.add(i % 12 == 1 ? testPrefix + ICE_CATCH_ENRICH + (i / 12) : null);
                    if (i % 12 == 1) {
                        catchEnrichTubeBarcodes.add(testPrefix + ICE_CATCH_ENRICH + (i / 12));
                    }
                }
            }
            break;
        case PLEX96:
            for (int j = 0; j < listPondRegTubeBarcodes.size(); j++) {
                List<String> pondRegTubeBarcodes = listPondRegTubeBarcodes.get(j);
                // pool all samples into a single tube
                for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
                    String sourceWell = bettaLimsMessageTestFactory.buildWellName(i,
                            BettaLimsMessageTestFactory.WellNameType.SHORT);
                    poolCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(pondRegRackBarcodes.get(j), sourceWell,
                            poolRackBarcode, bettaLimsMessageTestFactory.buildWellName(
                            j + 1, BettaLimsMessageTestFactory.WellNameType.SHORT)));
                }
                poolTubeBarcodes.add(testPrefix + "IcePool1" + j);
            }

            // Need a column of SPRI tubes for each pond rack
            for (int row = 0; row < 8; row++) {
                for(int column = 0; column < 12; column++) {
                    if (column % 2 == 0 && column / 2 < pondRegRackBarcodes.size()) {
                        spriTubeBarcodes.add(testPrefix + "IceSpri" + ((row * 12) + column));
                        catchEnrichSparseTubeBarcodes.add(testPrefix + ICE_CATCH_ENRICH + ((row * 12) + column));
                        catchEnrichTubeBarcodes.add(testPrefix + ICE_CATCH_ENRICH + ((row * 12) + column));
                    } else {
                        spriTubeBarcodes.add(null);
                        catchEnrichSparseTubeBarcodes.add(null);
                    }
                }
            }
            break;
        }
        icePoolingTransfer = bettaLimsMessageTestFactory.buildCherryPick("IcePoolingTransfer",
                pondRegRackBarcodes, listPondRegTubeBarcodes,
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
            for (int j = 0; j < poolTubeBarcodes.size(); j++) {
                for (int i = 0; i < 8; i++) {
                    cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(poolRackBarcode,
                            bettaLimsMessageTestFactory.buildWellName(j + 1, BettaLimsMessageTestFactory.WellNameType.SHORT),
                            spriRackBarcode,
                            bettaLimsMessageTestFactory.buildWellName((j * 2 + 1) + i * 12, BettaLimsMessageTestFactory.WellNameType.SHORT)));
                }
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
                        bettaLimsMessageTestFactory.buildWellName(i + 1, BettaLimsMessageTestFactory.WellNameType.SHORT),
                        poolTestRackBarcode, "A1"));
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

        if (prepType == PrepType.HYPER_PREP_ICE) {
            // IceHyperPrep1stBaitPick (bait tubes to empty plate)
            String firstBaitPlateBarcode = "B1B" + testPrefix;
            ice1stBaitPick = makeBaitPick("B1R" + testPrefix, firstBaitPlateBarcode, "IceHyperPrep1stBaitPick",
                    baitTube1Barcode, bettaLimsMessageTestFactory);
            bettaLimsMessageTestFactory.addMessage(messageList, ice1stBaitPick);

            // IceHyperPrep1stBaitAddition (bait plate to empty plate)
            iceHyperPrep1stBaitAddition = bettaLimsMessageTestFactory.buildPlateToPlate("IceHyperPrep1stBaitAddition",
                firstBaitPlateBarcode, firstHybPlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, iceHyperPrep1stBaitAddition);

            // IceHyperPrep1stHybridization (SPRI rack merges into bait-filled plate)
            ice1stHybridization = bettaLimsMessageTestFactory.buildRackToPlate(
                "IceHyperPrep1stHybridization", spriRackBarcode, spriTubeBarcodes, firstHybPlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, ice1stHybridization);
            // (There is no PostIce1stHybridizationThermoCyclerLoaded)

        } else {
            ice1stHybridization = bettaLimsMessageTestFactory.buildRackToPlate(
                    "Ice1stHybridization", spriRackBarcode, spriTubeBarcodes, firstHybPlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, ice1stHybridization);

            // Ice1stBaitPick
            ice1stBaitPick = makeBaitPick("B1R" + testPrefix, firstHybPlateBarcode, "Ice1stBaitPick",
                    baitTube1Barcode, bettaLimsMessageTestFactory);
            bettaLimsMessageTestFactory.addMessage(messageList, ice1stBaitPick);

            //PostIce1stHybridizationThermoCyclerLoaded
            postIce1stHybridizationThermoCyclerLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                    "PostIce1stHybridizationThermoCyclerLoaded", firstHybPlateBarcode);
            postIce1stHybridizationThermoCyclerLoaded.setStation("WALDORF");
            bettaLimsMessageTestFactory.addMessage(messageList, postIce1stHybridizationThermoCyclerLoaded);
        }

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

        if (prepType == PrepType.HYPER_PREP_ICE) {
            // IceHyperPrep2ndBaitPick (bait tubes to empty plate)
            String secondBaitPlateBarcode = "B2B" + testPrefix;
            ice2ndBaitPick = makeBaitPick("B2R" + testPrefix, secondBaitPlateBarcode, "IceHyperPrep2ndBaitPick",
                    baitTube2Barcode, bettaLimsMessageTestFactory);
            bettaLimsMessageTestFactory.addMessage(messageList, ice2ndBaitPick);

            // Ice2ndHybridization in-place event
            ice2ndHybridization = bettaLimsMessageTestFactory.buildPlateEvent("Ice2ndHybridization",
                    firstCapturePlateBarcode, BettaLimsMessageTestFactory.reagentList(ice2ndHybReagents));
            bettaLimsMessageTestFactory.addMessage(messageList, ice2ndHybridization);

            // IceHyperPrep2ndBaitAddition (bait plate merges into 2nd hyb plate)
            iceHyperPrep2ndBaitAddition = bettaLimsMessageTestFactory.buildPlateToPlate("IceHyperPrep2ndBaitAddition",
                    secondBaitPlateBarcode, firstCapturePlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, iceHyperPrep2ndBaitAddition);

        } else {
            // Ice2ndHybridization in-place event
            ice2ndHybridization = bettaLimsMessageTestFactory.buildPlateEvent("Ice2ndHybridization",
                    firstCapturePlateBarcode, BettaLimsMessageTestFactory.reagentList(ice2ndHybReagents));
            bettaLimsMessageTestFactory.addMessage(messageList, ice2ndHybridization);

            // Ice2ndBaitPick
            List<BettaLimsMessageTestFactory.CherryPick> bait2CherryPicks = new ArrayList<>();
            String bait2RackBarcode = "B2R" + testPrefix;
            for (int i = 0; i < 8; i++) {
                bait2CherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(bait2RackBarcode, "A01",
                        firstCapturePlateBarcode, (char) ('A' + i) + "01"));
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
        }

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
        iceCatchEnrichmentSetup = bettaLimsMessageTestFactory.buildPlateEvent("IceCatchEnrichmentSetup",
                catchCleanupPlateBarcode, BettaLimsMessageTestFactory.reagentList(iceCatchEnrichmentSetupReagents));
        bettaLimsMessageTestFactory.addMessage(messageList, iceCatchEnrichmentSetup);

        postIceCatchEnrichmentSetupThermoCyclerLoaded = bettaLimsMessageTestFactory.buildPlateEvent(
                "PostIceCatchEnrichmentSetupThermoCyclerLoaded", catchCleanupPlateBarcode);
        postIceCatchEnrichmentSetupThermoCyclerLoaded.setStation("WALDORF");
        bettaLimsMessageTestFactory.addMessage(messageList, postIceCatchEnrichmentSetupThermoCyclerLoaded);

        // IceCatchEnrichmentCleanup
        catchEnrichRackBarcode = testPrefix + ICE_CATCH_ENRICH;
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

    public List<String> getCatchEnrichSparseTubeBarcodes() {
        return catchEnrichSparseTubeBarcodes;
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

    public PlateTransferEventType getIceHyperPrep1stBaitAddition() {
        return iceHyperPrep1stBaitAddition;
    }

    public PlateTransferEventType getIceHyperPrep2ndBaitAddition() {
        return iceHyperPrep2ndBaitAddition;
    }

    protected static PlateCherryPickEvent makeBaitPick(String baitRackBarcode, String targetPlateBarcode,
            String eventName, String baitTubeBarcode, BettaLimsMessageTestFactory bettaLimsMessageTestFactory) {
        PlateCherryPickEvent pickEvent;
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(baitRackBarcode, "A01",
                    targetPlateBarcode, (char) ('A' + i) + "01"));
        }
        pickEvent = bettaLimsMessageTestFactory.buildCherryPickToPlate(eventName,
                LabEventFactory.PHYS_TYPE_TUBE_RACK, Collections.singletonList(baitRackBarcode),
                Collections.singletonList(Collections.singletonList(baitTubeBarcode)),
                Collections.singletonList(targetPlateBarcode), cherryPicks);
        ReagentType baitReagent = new ReagentType();
        baitReagent.setBarcode("Bait" + Long.toString(System.currentTimeMillis()));
        baitReagent.setKitType("Rapid Capture Kit Box 4 (Bait)");
        MetadataType metadataType = new MetadataType();
        metadataType.setName("Bait Well");
        metadataType.setValue("A1");
        baitReagent.getMetadata().add(metadataType);
        pickEvent.getReagent().add(baitReagent);
        return pickEvent;
    }
}
