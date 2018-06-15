package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SelectionJaxbBuilder {
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> pondRegRackBarcodes;
    private final List<List<String>> listPondRegTubeBarcodes;
    private final String baitTubeBarcode;
    private List<String> poolTubeBarcodes = new ArrayList<>();
    private String poolPlateBarcode;
    private PlateCherryPickEvent selectionPoolingTransfer;
    private String hybPlateBarcode;
    private PlateTransferEventType selectionHybSetup;
    private String baitRackBarcode;
    private PlateCherryPickEvent baitAddition;

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

        // Hyb Setup
        hybPlateBarcode = testPrefix + "SelectionHybPlate";
        selectionHybSetup = bettaLimsMessageTestFactory.buildPlateToPlate("SelectionHybSetup",
                poolPlateBarcode, hybPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, selectionHybSetup);

        // Bait Pick
        baitRackBarcode = testPrefix + "SelectionBaitRack";
        baitAddition = IceJaxbBuilder.makeBaitPick(baitRackBarcode, hybPlateBarcode, "SelectionBaitPick", baitTubeBarcode,
                bettaLimsMessageTestFactory);


        return this;
    }
}
