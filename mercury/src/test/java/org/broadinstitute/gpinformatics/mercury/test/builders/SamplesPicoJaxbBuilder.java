package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * Build the messages used in Samples (BSP) Pico batches
 */
@SuppressWarnings("FeatureEnvy")
public class SamplesPicoJaxbBuilder {
    private final List<String> tubeBarcodes;
    private final String labBatchId;
    private final String timestamp;

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private PlateTransferEventType picoDilutionTransferJaxbA1;
    private PlateTransferEventType picoDilutionTransferJaxbA2;
    private PlateTransferEventType picoDilutionTransferJaxbB1;
    private PlateEventType picoBufferAdditionJaxb;
    private PlateTransferEventType picoMicrofluorTransferJaxb;

    public SamplesPicoJaxbBuilder(List<String> tubeBarcodes, String labBatchId, String timestamp) {
        this.tubeBarcodes = tubeBarcodes;
        this.labBatchId = labBatchId;
        this.timestamp = timestamp;
    }

    /**
     * Build JAXB messages.  These messages can be sent to BettaLimsMessageResource, or used to build entity
     * graphs.
     */
    public void buildJaxb() {
        BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

        // 3 x  PicoDilutionTransfer
        String picoDilutionPlateBarcode = "PicoDilutionPlate" + timestamp;
        picoDilutionTransferJaxbA1 = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                "PicoRack" + timestamp, tubeBarcodes, picoDilutionPlateBarcode);
        picoDilutionTransferJaxbA1.getPlate().setSection("P384_96TIP_1INTERVAL_A1");
        picoDilutionTransferJaxbA1.getPlate().setPhysType("Eppendorf384");
        picoDilutionTransferJaxbA1.setBatchId(labBatchId);

        picoDilutionTransferJaxbA2 = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                "PicoRack" + timestamp, tubeBarcodes, picoDilutionPlateBarcode);
        picoDilutionTransferJaxbA2.getPlate().setSection("P384_96TIP_1INTERVAL_A2");
        picoDilutionTransferJaxbA2.getPlate().setPhysType("Eppendorf384");
        picoDilutionTransferJaxbA2.setBatchId(labBatchId);

        picoDilutionTransferJaxbB1 = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                "PicoRack" + timestamp, tubeBarcodes, picoDilutionPlateBarcode);
        picoDilutionTransferJaxbB1.getPlate().setSection("P384_96TIP_1INTERVAL_B1");
        picoDilutionTransferJaxbB1.getPlate().setPhysType("Eppendorf384");
        picoDilutionTransferJaxbB1.setBatchId(labBatchId);

        bettaLimsMessageTestFactory.addMessage(messageList, picoDilutionTransferJaxbA1,
                picoDilutionTransferJaxbA2, picoDilutionTransferJaxbB1);

        /*
                    // PicoBufferTransfer
                    PlateTransferEventType picoBufferTransferJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                            "PicoBufferTransfer", "PicoBufferPlate", picoDilutionPlateBarcode);
                    BettaLIMSMessage bettaLIMSMessage2 = new BettaLIMSMessage();
                    bettaLIMSMessage2.getPlateTransferEvent().add(picoBufferTransferJaxb);
                    messageList.add(bettaLIMSMessage2);

        */
        // plateEvent PicoBufferAddition
        picoBufferAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("PicoBufferAddition",
                picoDilutionPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoBufferAdditionJaxb);

        // PicoMicrofluorTransfer
        String picoMicrofluorPlateBarcode = "PicoMicrofluorPlate" + timestamp;
        picoMicrofluorTransferJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoMicrofluorTransfer",
                picoDilutionPlateBarcode,
                picoMicrofluorPlateBarcode);
        picoMicrofluorTransferJaxb.getSourcePlate().setSection("ALL384");
        picoMicrofluorTransferJaxb.getSourcePlate().setPhysType("Eppendorf384");
        picoMicrofluorTransferJaxb.getPlate().setSection("ALL384");
        picoMicrofluorTransferJaxb.getPlate().setPhysType("Eppendorf384");
        // todo jmt batch ID is set only for the first message?
        picoMicrofluorTransferJaxb.setBatchId(labBatchId);
        bettaLimsMessageTestFactory.addMessage(messageList, picoMicrofluorTransferJaxb);
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateTransferEventType getPicoMicrofluorTransferJaxb() {
        return picoMicrofluorTransferJaxb;
    }

    public PlateTransferEventType getPicoDilutionTransferJaxbA1() {
        return picoDilutionTransferJaxbA1;
    }

    public PlateTransferEventType getPicoDilutionTransferJaxbA2() {
        return picoDilutionTransferJaxbA2;
    }

    public PlateTransferEventType getPicoDilutionTransferJaxbB1() {
        return picoDilutionTransferJaxbB1;
    }

    public PlateEventType getPicoBufferAdditionJaxb() {
        return picoBufferAdditionJaxb;
    }
}
