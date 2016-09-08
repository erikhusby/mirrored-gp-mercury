package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Builds JAXB objects for F.P. messages.
 */
public class FPJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> sourcePlates;
    private final String dualIndexPrimerPlateBarcode;
    private final int numSamples;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private List<PlateTransferEventType> pcr1TransferJaxbs = new ArrayList<>();
    private List<PlateTransferEventType> preSpriPoolingJaxbs = new ArrayList<>();
    private String pcr1Plate;
    private String pcr2Plate;
    private PlateTransferEventType indexPrimerTransferJaxb;
    private PlateTransferEventType pcr1ToPcr2TransferJaxb;
    private String preSPRIPoolPlate;
    private String finalPoolRackBarcode;
    private String finalPoolTubeBarcode;
    private PlateCherryPickEvent poolingTransferJaxb;

    public FPJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                               List<String> sourcePlates,  String dualIndexPrimerPlateBarcode, int numSamples) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.sourcePlates = sourcePlates;
        this.dualIndexPrimerPlateBarcode = dualIndexPrimerPlateBarcode;
        this.numSamples = numSamples;
    }

    public FPJaxbBuilder invoke() {
        pcr1Plate = testPrefix + "FP_PCR1";
        List<SBSSection> sections = Arrays.asList(
                SBSSection.P384_96TIP_1INTERVAL_A1, SBSSection.P384_96TIP_1INTERVAL_A2,
                SBSSection.P384_96TIP_1INTERVAL_B1, SBSSection.P384_96TIP_1INTERVAL_B2);
        for (int i = 0; i < sourcePlates.size(); i++) {
            PlateTransferEventType pcr1TransferJaxb =
                    bettaLimsMessageTestFactory.buildPlateToPlate("FP_PCR1", sourcePlates.get(i), pcr1Plate);
            pcr1TransferJaxb.getPlate().setSection(sections.get(i).getSectionName());
            pcr1TransferJaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
            pcr1TransferJaxbs.add(pcr1TransferJaxb);
            bettaLimsMessageTestFactory.addMessage(messageList, pcr1TransferJaxb);
        }

        pcr2Plate = testPrefix + "FP_PCR2";
        indexPrimerTransferJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("FP_IndexPrimerTransfer",
                dualIndexPrimerPlateBarcode,
                pcr2Plate);
        indexPrimerTransferJaxb.getPlate().setSection("ALL384");
        indexPrimerTransferJaxb.getSourcePlate().setSection("ALL384");
        indexPrimerTransferJaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        indexPrimerTransferJaxb.getSourcePlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        bettaLimsMessageTestFactory.addMessage(messageList, indexPrimerTransferJaxb);

        pcr1ToPcr2TransferJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("FP_PCRTail2",
                pcr1Plate,
                pcr2Plate);
        pcr1ToPcr2TransferJaxb.getPlate().setSection("ALL384");
        pcr1ToPcr2TransferJaxb.getSourcePlate().setSection("ALL384");
        pcr1ToPcr2TransferJaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        pcr1ToPcr2TransferJaxb.getSourcePlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        bettaLimsMessageTestFactory.addMessage(messageList, pcr1ToPcr2TransferJaxb);

        preSPRIPoolPlate = testPrefix + "FP_PreSPRIPool";
        for (int i = 0; i < sourcePlates.size(); i++) {
            PlateTransferEventType preSpriPoolingJaxb =
                    bettaLimsMessageTestFactory.buildPlateToPlate("FP_PreSPRIPooling", pcr2Plate, preSPRIPoolPlate);
            preSpriPoolingJaxb.getSourcePlate().setSection(sections.get(i).getSectionName());
            preSpriPoolingJaxb.getSourcePlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
            preSpriPoolingJaxbs.add(preSpriPoolingJaxb);
            bettaLimsMessageTestFactory.addMessage(messageList, preSpriPoolingJaxb);
        }

        finalPoolRackBarcode = testPrefix + "FP_PoolRack";
        finalPoolTubeBarcode = testPrefix + "FP_FinalPoolTube";
        List<String> finalPoolTubes = Arrays.asList(finalPoolTubeBarcode);
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        for (int rackPosition = 1; rackPosition <= numSamples; rackPosition++) {
            String sourceWell = bettaLimsMessageTestFactory.buildWellName(
                    BettaLimsMessageTestFactory.NUMBER_OF_RACK_COLUMNS, rackPosition,
                    BettaLimsMessageTestFactory.WellNameType.LONG);
            cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(preSPRIPoolPlate, sourceWell,
                    finalPoolRackBarcode, "A01"));
        }

        poolingTransferJaxb = bettaLimsMessageTestFactory.buildPlateToRackCherryPick("FP_PoolingTransfer",
                Collections.singletonList(preSPRIPoolPlate), finalPoolRackBarcode,
                Collections.singletonList(finalPoolTubes), cherryPicks);

        bettaLimsMessageTestFactory.addMessage(messageList, poolingTransferJaxb);

        return this;
    }

    public String getDualIndexPrimerPlateBarcode() {
        return dualIndexPrimerPlateBarcode;
    }

    public int getNumSamples() {
        return numSamples;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public List<PlateTransferEventType> getPcr1TransferJaxbs() {
        return pcr1TransferJaxbs;
    }

    public List<PlateTransferEventType> getPreSpriPoolingJaxbs() {
        return preSpriPoolingJaxbs;
    }

    public String getPcr1Plate() {
        return pcr1Plate;
    }

    public String getPcr2Plate() {
        return pcr2Plate;
    }

    public PlateTransferEventType getIndexPrimerTransferJaxb() {
        return indexPrimerTransferJaxb;
    }

    public PlateTransferEventType getPcr1ToPcr2TransferJaxb() {
        return pcr1ToPcr2TransferJaxb;
    }

    public String getPreSPRIPoolPlate() {
        return preSPRIPoolPlate;
    }

    public String getFinalPoolRackBarcode() {
        return finalPoolRackBarcode;
    }

    public String getFinalPoolTubeBarcode() {
        return finalPoolTubeBarcode;
    }

    public PlateCherryPickEvent getPoolingTransferJaxb() {
        return poolingTransferJaxb;
    }
}
