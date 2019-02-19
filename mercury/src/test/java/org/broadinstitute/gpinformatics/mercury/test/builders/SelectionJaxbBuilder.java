package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectionJaxbBuilder {
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> pondRegRackBarcodes;
    private final List<List<String>> listPondRegTubeBarcodes;
    private List<String> catchTubeBarcodes = new ArrayList<>();
    private final String baitTubeBarcode;
    private List<String> poolTubeBarcodes = new ArrayList<>();
    private String poolPlateBarcode;
    private PlateCherryPickEvent selectionPoolingTransfer;
    private String concentrationPlateBarcode;
    private PlateTransferEventType concentrationJaxb;
    private String hybPlateBarcode;
    private PlateTransferEventType selectionHybSetup;
    private String mastermixPlateBarcode;
    private String baitRackBarcode;
    private PlateCherryPickEvent baitPickJaxb;
    private PlateEventType beadBindingJaxb;
    private PlateTransferEventType baitAdditionJaxb;
    private PlateEventType captureJaxb;
    private PlateEventType catchPcrJaxb;
    private String catchRackBarcode;
    private PlateTransferEventType catchCleanup;

    public SelectionJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                List<String> pondRegRackBarcodes, List<List<String>> listPondRegTubeBarcodes, String baitTubeBarcode) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.pondRegRackBarcodes = pondRegRackBarcodes;
        this.listPondRegTubeBarcodes = listPondRegTubeBarcodes;
        this.baitTubeBarcode = baitTubeBarcode;
    }
    public SelectionJaxbBuilder invoke() {

        // SelectionPoolingTransfer
        List<BettaLimsMessageTestFactory.CherryPick> poolCherryPicks = new ArrayList<>();
        poolPlateBarcode = testPrefix + "SelectionPool";
        for (int j = 0; j < listPondRegTubeBarcodes.size(); j++) {
            List<String> pondRegTubeBarcodes = listPondRegTubeBarcodes.get(j);
            // pool each row into a single well of a Deep Well
            for (int i = 1; i <= pondRegTubeBarcodes.size(); i++) {
                catchTubeBarcodes.add(i % 12 == 1 ? testPrefix + "selectionCatch" + (i / 12) : null);
                String sourceWell = bettaLimsMessageTestFactory.buildWellName(i,
                        BettaLimsMessageTestFactory.WellNameType.SHORT);
                @SuppressWarnings({"NumericCastThatLosesPrecision"})
                String destinationWell = bettaLimsMessageTestFactory.buildWellName(
                        (((int) Math.ceil(i / 12.0) - 1) * 12) + 1, BettaLimsMessageTestFactory.WellNameType.SHORT);
                poolCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(pondRegRackBarcodes.get(j), sourceWell,
                        poolPlateBarcode, destinationWell));
            }
        }

        selectionPoolingTransfer = bettaLimsMessageTestFactory.buildCherryPickToPlate("SelectionPoolingTransfer",
                "TubeRack", pondRegRackBarcodes, listPondRegTubeBarcodes,
                Collections.singletonList(poolPlateBarcode), poolCherryPicks);
        bettaLimsMessageTestFactory.addMessage(messageList, selectionPoolingTransfer);

        //Concentration
        concentrationPlateBarcode = testPrefix + "SelectionConcentrationPlate";
        concentrationJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("SelectionConcentrationTransfer",
                poolPlateBarcode, concentrationPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, concentrationJaxb);

        // Hyb Setup
        hybPlateBarcode = testPrefix + "SelectionHybPlate";
        selectionHybSetup = bettaLimsMessageTestFactory.buildPlateToPlate("SelectionHybSetup",
                concentrationPlateBarcode, hybPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, selectionHybSetup);

        // Bait Pick
        mastermixPlateBarcode = testPrefix + "SelectionMasterMixPlate";
        baitRackBarcode = testPrefix + "SelectionBaitRack";
        baitPickJaxb = IceJaxbBuilder.makeBaitPick(baitRackBarcode, mastermixPlateBarcode, "SelectionBaitPick", baitTubeBarcode,
                bettaLimsMessageTestFactory);
        bettaLimsMessageTestFactory.addMessage(messageList, baitPickJaxb);

        // Bait Addition
        baitAdditionJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("SelectionHybSetup",
                mastermixPlateBarcode, hybPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, selectionHybSetup);

        beadBindingJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "SelectionBeadBinding", hybPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, beadBindingJaxb);

        captureJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "SelectionCapture", hybPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, captureJaxb);

        catchPcrJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                "SelectionCatchPCR", hybPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, catchPcrJaxb);

        catchRackBarcode = testPrefix + "SelectionCatchRack";
        catchCleanup = bettaLimsMessageTestFactory.buildPlateToRack(
                "SelectionCatchRegistration", hybPlateBarcode, catchRackBarcode,
                catchTubeBarcodes);
        for (ReceptacleType receptacleType : catchCleanup.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(new BigDecimal("50"));
        }
        bettaLimsMessageTestFactory.addMessage(messageList, catchCleanup);

        return this;
    }

    public List<String> getCatchTubeBarcodes() {
        return catchTubeBarcodes;
    }

    public String getBaitTubeBarcode() {
        return baitTubeBarcode;
    }

    public List<String> getPoolTubeBarcodes() {
        return poolTubeBarcodes;
    }

    public String getPoolPlateBarcode() {
        return poolPlateBarcode;
    }

    public PlateCherryPickEvent getSelectionPoolingTransfer() {
        return selectionPoolingTransfer;
    }

    public String getConcentrationPlateBarcode() {
        return concentrationPlateBarcode;
    }

    public PlateTransferEventType getConcentrationJaxb() {
        return concentrationJaxb;
    }

    public String getHybPlateBarcode() {
        return hybPlateBarcode;
    }

    public PlateTransferEventType getSelectionHybSetup() {
        return selectionHybSetup;
    }

    public String getMastermixPlateBarcode() {
        return mastermixPlateBarcode;
    }

    public String getBaitRackBarcode() {
        return baitRackBarcode;
    }

    public PlateCherryPickEvent getBaitPickJaxb() {
        return baitPickJaxb;
    }

    public PlateEventType getBeadBindingJaxb() {
        return beadBindingJaxb;
    }

    public PlateTransferEventType getBaitAdditionJaxb() {
        return baitAdditionJaxb;
    }

    public PlateEventType getCaptureJaxb() {
        return captureJaxb;
    }

    public PlateEventType getCatchPcrJaxb() {
        return catchPcrJaxb;
    }

    public String getCatchRackBarcode() {
        return catchRackBarcode;
    }

    public PlateTransferEventType getCatchCleanup() {
        return catchCleanup;
    }
}
