package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builds JAXB objects for QTP messages
 */
public class QtpJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<List<String>> listLcsetListNormCatchBarcodes;
    private final List<String> normCatchRackBarcodes;

    private String poolRackBarcode;
    private List<String> poolTubeBarcodes = new ArrayList<String>();
    private PlateCherryPickEvent   poolingTransferJaxb;
    private String denatureRackBarcode;
    private PlateCherryPickEvent denatureJaxb;
    private String stripTubeHolderBarcode;
    private PlateCherryPickEvent stripTubeTransferJaxb;
    private PlateTransferEventType flowcellTransferJaxb;
    private ReceptacleEventType    flowcellLoad;
    private final List<BettaLIMSMessage> messageList = new ArrayList<BettaLIMSMessage>();
    private String flowcellBarcode;
    private String stripTubeBarcode;
    private final WorkflowName workflowName;
    private String denatureTubeBarcode;

    public QtpJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageFactory, String testPrefix,
                          List<List<String>> listLcsetListNormCatchBarcodes, List<String> normCatchRackBarcodes,
                          WorkflowName workflowName) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageFactory;
        this.testPrefix = testPrefix;
        this.listLcsetListNormCatchBarcodes = listLcsetListNormCatchBarcodes;
        this.normCatchRackBarcodes = normCatchRackBarcodes;
        this.workflowName = workflowName;
    }

    public String getPoolRackBarcode() {
        return poolRackBarcode;
    }

    public List<String> getPoolTubeBarcodes() {
        return poolTubeBarcodes;
    }

    public PlateCherryPickEvent getPoolingTransferJaxb() {
        return poolingTransferJaxb;
    }

    public String getDenatureRackBarcode() {
        return denatureRackBarcode;
    }

    public PlateCherryPickEvent getDenatureJaxb() {
        return denatureJaxb;
    }

    public String getStripTubeHolderBarcode() {
        return stripTubeHolderBarcode;
    }

    public PlateCherryPickEvent getStripTubeTransferJaxb() {
        return stripTubeTransferJaxb;
    }

    public PlateTransferEventType getFlowcellTransferJaxb() {
        return flowcellTransferJaxb;
    }

    public ReceptacleEventType getFlowcellLoad() {
        return flowcellLoad;
    }

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getStripTubeBarcode() {
        return stripTubeBarcode;
    }

    public String getDenatureTubeBarcode() {
        return denatureTubeBarcode;
    }

    public QtpJaxbBuilder invoke() {
        int i = 0;
        for (List<String> normCatchBarcodes : listLcsetListNormCatchBarcodes) {
            // PoolingTransfer
            poolRackBarcode = "PoolRack" + testPrefix;
            List<BettaLimsMessageTestFactory.CherryPick> poolingCherryPicks =
                    new ArrayList<BettaLimsMessageTestFactory.CherryPick>();
            for (int rackPosition = 1; rackPosition <= normCatchBarcodes.size(); rackPosition++) {
                poolingCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(normCatchRackBarcodes.get(i),
                        bettaLimsMessageTestFactory.buildWellName(rackPosition,
                                BettaLimsMessageTestFactory.WellNameType.SHORT), poolRackBarcode,
                        "A1"));
            }
            poolTubeBarcodes.add("Pool" + testPrefix + i);
            poolingTransferJaxb = bettaLimsMessageTestFactory.buildCherryPick("PoolingTransfer",
                    Arrays.asList(normCatchRackBarcodes.get(i)), Collections.singletonList(normCatchBarcodes),
                    poolRackBarcode, Collections.singletonList(poolTubeBarcodes.get(i)), poolingCherryPicks);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, poolingTransferJaxb);
            i++;
        }

        // DenatureTransfer
        denatureRackBarcode = "DenatureRack" + testPrefix;
        List<BettaLimsMessageTestFactory.CherryPick> denatureCherryPicks = new ArrayList<BettaLimsMessageTestFactory.CherryPick>();
        List<String> denatureTubeBarcodes = new ArrayList<String>();
        for (int j = 0; j < poolTubeBarcodes.size(); j++) {
            denatureCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(
                    poolRackBarcode, bettaLimsMessageTestFactory.buildWellName(j + 1,
                    BettaLimsMessageTestFactory.WellNameType.SHORT),
                    denatureRackBarcode, bettaLimsMessageTestFactory.buildWellName(j + 1,
                    BettaLimsMessageTestFactory.WellNameType.SHORT)));
            denatureTubeBarcode = "DenatureTube" + testPrefix + j;
            denatureTubeBarcodes.add(denatureTubeBarcode);
        }
        denatureJaxb = bettaLimsMessageTestFactory.buildCherryPick("DenatureTransfer",
                Collections.singletonList(poolRackBarcode), Collections.singletonList(poolTubeBarcodes),
                denatureRackBarcode, denatureTubeBarcodes, denatureCherryPicks);
        LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, denatureJaxb);

        if (workflowName != WorkflowName.EXOME_EXPRESS) {
            // StripTubeBTransfer
            stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
            List<BettaLimsMessageTestFactory.CherryPick> stripTubeCherryPicks = new ArrayList<BettaLimsMessageTestFactory.CherryPick>();
            int sourcePosition = 0;
            // Transfer column 1 to 8 rows, using non-empty source rows
            for (int destinationPosition = 0; destinationPosition < 8; destinationPosition++) {
                stripTubeCherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(
                        denatureRackBarcode, Character.toString((char) ('A' + sourcePosition)) + "01",
                        stripTubeHolderBarcode, Character.toString((char) ('A' + destinationPosition)) + "01"));
                if (sourcePosition + 1 < poolTubeBarcodes.size()) {
                    sourcePosition++;
                }
            }
            stripTubeBarcode = "StripTube" + testPrefix + "1";

            stripTubeTransferJaxb = bettaLimsMessageTestFactory.buildCherryPickToStripTube("StripTubeBTransfer",
                    Arrays.asList(denatureRackBarcode),
                    Arrays.asList(denatureTubeBarcodes),
                    stripTubeHolderBarcode,
                    Arrays.asList(stripTubeBarcode),
                    stripTubeCherryPicks);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, stripTubeTransferJaxb);

            // FlowcellTransfer
            flowcellBarcode = "Flowcell" + testPrefix;
            flowcellTransferJaxb = bettaLimsMessageTestFactory.buildStripTubeToFlowcell("FlowcellTransfer",
                    stripTubeBarcode, flowcellBarcode);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, flowcellTransferJaxb);

            flowcellLoad = bettaLimsMessageTestFactory.buildReceptacleEvent(LabEventType.FLOWCELL_LOADED.getName(),
                    flowcellBarcode, LabEventFactory.PHYS_TYPE_FLOWCELL);
            LabEventTest.addMessage(messageList, bettaLimsMessageTestFactory, flowcellLoad);
        }
        return this;
    }
}
