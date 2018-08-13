package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SingleCellSmartSeqJaxbBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> sourcePlates;
    private final List<String> indexPlateBarcodes;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private List<PlateEventType> singleCellSpriAdditions = new ArrayList<>();
    private List<PlateEventType> singleCellPolyASelections = new ArrayList<>();
    private List<PlateEventType> singleCellRTs = new ArrayList<>();
    private List<PlateEventType> singleCellWTAs = new ArrayList<>();
    private List<PlateEventType> singleCellWTASpris = new ArrayList<>();
    private List<PlateTransferEventType> singleCellElutions = new ArrayList<>();
    private List<String> elutionPlateBarcodes = new ArrayList<>();
    private List<PlateTransferEventType> singleCellPondPicos = new ArrayList<>();
    private List<PlateEventType> singleCellNormTransfers = new ArrayList<>();
    private List<PlateTransferEventType> singleCellTagmentations = new ArrayList<>();
    private PlateEventType stopTagmentation;
    private List<PlateTransferEventType> singleCellIndexAdapterLigations = new ArrayList<>();
    private String poolRackBarcode;
    private String poolTubeBarcode;
    private PlateCherryPickEvent poolingTransferJaxb;
    private String bulkSpriRackBarcode;
    private String bulkSpriTubeBarcode;
    private PlateTransferEventType bulkSpriTransferJaxb;
    private String pondPico1;
    private String pondPico2;

    public SingleCellSmartSeqJaxbBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
                                         List<String> sourcePlates, List<String> indexPlateBarcodes) {

        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.sourcePlates = sourcePlates;
        this.indexPlateBarcodes = indexPlateBarcodes;
    }

    public SingleCellSmartSeqJaxbBuilder invoke() {
        int sourcePlateCounter = 0;
        for (String sourcePlate: sourcePlates) {
            PlateEventType singleCellSpriAddition = bettaLimsMessageTestFactory.buildPlateEvent(
                    "SingleCellSPRIAddition", sourcePlate);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellSpriAddition);
            singleCellSpriAdditions.add(singleCellSpriAddition);

            PlateEventType singleCellPolyASelection = bettaLimsMessageTestFactory.buildPlateEvent(
                    "SingleCellPolyA", sourcePlate);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellPolyASelection);
            singleCellPolyASelections.add(singleCellPolyASelection);

            PlateEventType singleCellRT = bettaLimsMessageTestFactory.buildPlateEvent(
                    "SingleCellRT", sourcePlate);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellRT);
            singleCellRTs.add(singleCellRT);

            PlateEventType singleCellWTA = bettaLimsMessageTestFactory.buildPlateEvent(
                    "SingleCellWTA", sourcePlate);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellWTA);
            singleCellWTAs.add(singleCellWTA);

            PlateEventType singleCellWTASpri = bettaLimsMessageTestFactory.buildPlateEvent(
                    "SingleCellWTASpri", sourcePlate);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellWTASpri);
            singleCellWTASpris.add(singleCellWTASpri);

            String elutionPlateBarcode = "elutionTransferPlate_" + sourcePlateCounter + "_" + testPrefix;
            PlateTransferEventType singleCellElutionTransfer = bettaLimsMessageTestFactory.buildPlateToPlate(
                    "SingleCellElutionTransfer",
                    sourcePlate,
                    elutionPlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellElutionTransfer);
            singleCellElutions.add(singleCellElutionTransfer);
            elutionPlateBarcodes.add(elutionPlateBarcode);

            // Pond Pico
            pondPico1 = "scPondPico1_" + sourcePlateCounter + "_" + testPrefix;
            PlateTransferEventType singleCellPondPico1Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PondPico",
                    elutionPlateBarcode,
                    pondPico1);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellPondPico1Jaxb);
            singleCellPondPicos.add(singleCellPondPico1Jaxb);

            pondPico2 = "scPondPico2_" + sourcePlateCounter + "_" + testPrefix;
            PlateTransferEventType singleCellPondPico2Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PondPico",
                    elutionPlateBarcode,
                    pondPico2);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellPondPico2Jaxb);
            singleCellPondPicos.add(singleCellPondPico2Jaxb);

            // Norm Transfer
            PlateEventType singleCellNormTransfer = bettaLimsMessageTestFactory.buildPlateEvent(
                    "SingleCellNormalization", elutionPlateBarcode);
            bettaLimsMessageTestFactory.addMessage(messageList, singleCellNormTransfer);
            singleCellNormTransfers.add(singleCellNormTransfer);

            sourcePlateCounter++;
        }

        List<String> quadrantSections = Arrays.asList("P384_96TIP_1INTERVAL_A1", "P384_96TIP_1INTERVAL_A2",
                "P384_96TIP_1INTERVAL_B1", "P384_96TIP_1INTERVAL_B2");

        //Tagmentation: 4 96well plates to 384
        String tagmentationPlate = "scTagmentation" + testPrefix;
        for (int i = 0; i < elutionPlateBarcodes.size(); i++) {
            String elutionPlateBarcode = elutionPlateBarcodes.get(i);
            String section = quadrantSections.get(i);
            PlateTransferEventType tagmentation = bettaLimsMessageTestFactory.buildPlateToPlate("SingleCellTagmentation",
                    elutionPlateBarcode,
                    tagmentationPlate);
            tagmentation.getPlate().setSection(section);
            tagmentation.getPlate().setPhysType("Eppendorf384");
            bettaLimsMessageTestFactory.addMessage(messageList, tagmentation);
            singleCellTagmentations.add(tagmentation);
        }

        stopTagmentation = bettaLimsMessageTestFactory.buildPlateEvent(
                "SingleCellStopTagmentation", tagmentationPlate);
        bettaLimsMessageTestFactory.addMessage(messageList, stopTagmentation);

        //4 96 well plate indexes into one 384 well plate
        for (int i = 0; i < elutionPlateBarcodes.size(); i++) {
            String indexPlateBarcode = indexPlateBarcodes.get(i);
            String elutionPlateBarcode = elutionPlateBarcodes.get(i);
            PlateTransferEventType adapterLigation = bettaLimsMessageTestFactory.buildPlateToPlate("SingleCellIndexAdapterLigation",
                    indexPlateBarcode,
                    elutionPlateBarcode);
            String section = quadrantSections.get(i);
            adapterLigation.getPlate().setSection(section);
            adapterLigation.getPlate().setPhysType("Eppendorf384");
            bettaLimsMessageTestFactory.addMessage(messageList, adapterLigation);
            singleCellIndexAdapterLigations.add(adapterLigation);
        }

        // Pooling
        poolRackBarcode = testPrefix + "SC_PoolRack";
        poolTubeBarcode = testPrefix + "SC_PoolTube";
        List<String> poolTubes = Arrays.asList(poolTubeBarcode);
        List<BettaLimsMessageTestFactory.CherryPick> cherryPicks = new ArrayList<>();
        for (char row = 'A'; row <= 'P'; row++) {
            for (int col = 1; col <= 24; col++) {
                String colStr = (col < 10) ? "0" + col : "" + col;
                String well = row + colStr;
                cherryPicks.add(new BettaLimsMessageTestFactory.CherryPick(tagmentationPlate, well,
                        poolRackBarcode, "A01"));
            }
        }

        poolingTransferJaxb = bettaLimsMessageTestFactory.buildPlateToRackCherryPick("SingleCellPooling",
                Collections.singletonList(tagmentationPlate), poolRackBarcode,
                Collections.singletonList(poolTubes), cherryPicks);
        poolingTransferJaxb.getSourcePlate().get(0).setPhysType("Eppendorf384");

        bettaLimsMessageTestFactory.addMessage(messageList, poolingTransferJaxb);

        // Bulk SPRI cleanup
        bulkSpriRackBarcode = testPrefix + "SC_BulkSpriRack";
        bulkSpriTubeBarcode = testPrefix + "SC_BulkSpriTube";
        bulkSpriTransferJaxb = bettaLimsMessageTestFactory.buildRackToRack(
                "SingleCellBulkSPRICleanup", poolRackBarcode, poolTubes, bulkSpriRackBarcode,
                Collections.singletonList(bulkSpriTubeBarcode));

        return this;
    }

    public List<PlateEventType> getSingleCellSpriAdditions() {
        return singleCellSpriAdditions;
    }

    public List<PlateEventType> getSingleCellPolyAs() {
        return singleCellPolyASelections;
    }

    public List<PlateEventType> getSingleCellRTs() {
        return singleCellRTs;
    }

    public List<PlateEventType> getSingleCellWTAs() {
        return singleCellWTAs;
    }

    public List<PlateEventType> getSingleCellWTASpris() {
        return singleCellWTASpris;
    }

    public List<PlateTransferEventType> getSingleCellElutions() {
        return singleCellElutions;
    }

    public List<String> getElutionPlateBarcodes() {
        return elutionPlateBarcodes;
    }

    public List<PlateTransferEventType> getSingleCellPondPicos() {
        return singleCellPondPicos;
    }

    public List<PlateEventType> getSingleCellNormTransfers() {
        return singleCellNormTransfers;
    }

    public List<PlateTransferEventType> getSingleCellTagmentations() {
        return singleCellTagmentations;
    }

    public List<PlateTransferEventType> getSingleCellIndexAdapterLigations() {
        return singleCellIndexAdapterLigations;
    }

    public PlateEventType getStopTagmentation() {
        return stopTagmentation;
    }

    public PlateCherryPickEvent getPoolingTransferJaxb() {
        return poolingTransferJaxb;
    }

    public PlateTransferEventType getBulkSpriTransferJaxb() {
        return bulkSpriTransferJaxb;
    }

    public String getBulkSpriRackBarcode() {
        return bulkSpriRackBarcode;
    }

    public String getBulkSpriTubeBarcode() {
        return bulkSpriTubeBarcode;
    }

    public String getPondPico1() {
        return pondPico1;
    }

    public String getPondPico2() {
        return pondPico2;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
