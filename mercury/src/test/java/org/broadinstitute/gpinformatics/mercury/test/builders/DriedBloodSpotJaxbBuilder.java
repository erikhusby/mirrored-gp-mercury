package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptaclePlateTransferEvent;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB BettaLIMS DTOs to test messaging for Dried Blood Spot extraction.
 */
public class DriedBloodSpotJaxbBuilder {
    private List<BettaLIMSMessage> messageList = new ArrayList<>();
    private List<ReceptaclePlateTransferEvent> samplePunchJaxbs = new ArrayList<>();
    private PlateEventType incubationMixJaxb;
    private PlateEventType lysisBufferJaxb;
    private PlateEventType magneticResinJaxb;
    private PlateTransferEventType dbs1stPurificationJaxb;
    private PlateEventType dbsWashBufferJaxb;
    private PlateEventType dbsElutionBufferJaxb;
    private PlateTransferEventType dbsFinalTransferJaxb;
    private List<String> ftaPaperBarcodes;
    private String labBatchId;
    private String timestamp;

    public DriedBloodSpotJaxbBuilder(List<String> ftaPaperBarcodes, String labBatchId, String timestamp) {
        this.ftaPaperBarcodes = ftaPaperBarcodes;
        this.labBatchId = labBatchId;
        this.timestamp = timestamp;
    }

    public void buildJaxb() {
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

        String incubationPlateBarcode = "DBSIncPlate" + timestamp;
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
        int paperNum = 1;
        for (String ftaPaperBarcode : ftaPaperBarcodes) {
            // DBSSamplePunch receptacle -> plate A01 etc.
            ReceptaclePlateTransferEvent samplePunchJaxb = bettaLimsMessageTestFactory.buildTubeToPlate(
                    "DBSSamplePunch", ftaPaperBarcode, incubationPlateBarcode, "96DeepWell",
                    bettaLimsMessageTestFactory.buildWellName(paperNum,
                            BettaLimsMessageTestFactory.WellNameType.LONG), "FTAPaper");
            paperNum++;
            samplePunchJaxb.setBatchId(labBatchId);
            samplePunchJaxbs.add(samplePunchJaxb);
            bettaLIMSMessage.getReceptaclePlateTransferEvent().add(samplePunchJaxb);
            bettaLimsMessageTestFactory.advanceTime();
        }
        messageList.add(bettaLIMSMessage);

        // DBSIncubationMix plateEvent
        incubationMixJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSIncubationMix", incubationPlateBarcode);
        ReagentType reagentType = new ReagentType();
        reagentType.setKitType("Incubation Mix");
        reagentType.setBarcode("IncubationMix1234");
        // todo jmt make reagents work in database free tests, GPLIM-3388
//        incubationMixJaxb.getReagent().add(reagentType);
        bettaLimsMessageTestFactory.addMessage(messageList, incubationMixJaxb);

        // DBSLysisBuffer plateEvent
        lysisBufferJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSLysisBuffer", incubationPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, lysisBufferJaxb);

        // DBSMagneticResin plateEVent
        magneticResinJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSMagneticResin", incubationPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, magneticResinJaxb);

        String firstPurificationBarcode = "DBS1stPur" + timestamp;
        // DBS1stPurification plate -> plate
        dbs1stPurificationJaxb = bettaLimsMessageTestFactory
                .buildPlateToPlate("DBS1stPurification", incubationPlateBarcode, firstPurificationBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, dbs1stPurificationJaxb);

        // DBSWashBuffer plateEvent
        dbsWashBufferJaxb = bettaLimsMessageTestFactory.buildPlateEvent("DBSWashBuffer", firstPurificationBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, dbsWashBufferJaxb);

        // DBSElutionBuffer plateEvent
        dbsElutionBufferJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("DBSElutionBuffer", firstPurificationBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, dbsElutionBufferJaxb);

        // DBSFinalTransfer plate -> rack
        List<String> finalTubeBarcodes = new ArrayList<>();
        for (int i = 0; i < ftaPaperBarcodes.size(); i++) {
            finalTubeBarcodes.add("DBSFinal" + i + timestamp);
        }
        dbsFinalTransferJaxb =
                bettaLimsMessageTestFactory.buildPlateToRack("DBSFinalTransfer", firstPurificationBarcode,
                        "DBSFinal" + timestamp, finalTubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, dbsFinalTransferJaxb);
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public List<ReceptaclePlateTransferEvent> getSamplePunchJaxbs() {
        return samplePunchJaxbs;
    }

    public PlateEventType getIncubationMixJaxb() {
        return incubationMixJaxb;
    }

    public PlateEventType getLysisBufferJaxb() {
        return lysisBufferJaxb;
    }

    public PlateEventType getMagneticResinJaxb() {
        return magneticResinJaxb;
    }

    public PlateTransferEventType getDbs1stPurificationJaxb() {
        return dbs1stPurificationJaxb;
    }

    public PlateEventType getDbsWashBufferJaxb() {
        return dbsWashBufferJaxb;
    }

    public PlateEventType getDbsElutionBufferJaxb() {
        return dbsElutionBufferJaxb;
    }

    public PlateTransferEventType getDbsFinalTransferJaxb() {
        return dbsFinalTransferJaxb;
    }
}
