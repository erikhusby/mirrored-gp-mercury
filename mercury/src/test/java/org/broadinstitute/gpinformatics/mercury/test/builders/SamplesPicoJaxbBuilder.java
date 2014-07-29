package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.SBSSection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;

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
    private PlateTransferEventType picoStandardsTransferCol2Jaxb;
    private PlateTransferEventType picoStandardsTransferCol4Jaxb;
    private PlateTransferEventType picoStandardsTransferCol6Jaxb;
    private PlateTransferEventType picoStandardsTransferCol8Jaxb;
    private PlateTransferEventType picoStandardsTransferCol10Jaxb;
    private PlateTransferEventType picoStandardsTransferCol12Jaxb;

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
        picoDilutionTransferJaxbA1.getPlate().setSection(SBSSection.P384_96TIP_1INTERVAL_A1.getSectionName());
        picoDilutionTransferJaxbA1.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoDilutionTransferJaxbA1.setBatchId(labBatchId);

        picoDilutionTransferJaxbA2 = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                "PicoRack" + timestamp, tubeBarcodes, picoDilutionPlateBarcode);
        picoDilutionTransferJaxbA2.getPlate().setSection(SBSSection.P384_96TIP_1INTERVAL_A2.getSectionName());
        picoDilutionTransferJaxbA2.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoDilutionTransferJaxbA2.setBatchId(labBatchId);

        picoDilutionTransferJaxbB1 = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                "PicoRack" + timestamp, tubeBarcodes, picoDilutionPlateBarcode);
        picoDilutionTransferJaxbB1.getPlate().setSection(SBSSection.P384_96TIP_1INTERVAL_B1.getSectionName());
        picoDilutionTransferJaxbB1.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
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
        picoMicrofluorTransferJaxb.getSourcePlate().setSection(SBSSection.ALL384.getSectionName());
        picoMicrofluorTransferJaxb.getSourcePlate().setPhysType(
                StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoMicrofluorTransferJaxb.getPlate().setSection(SBSSection.ALL384.getSectionName());
        picoMicrofluorTransferJaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        // todo jmt batch ID is set only for the first message?
        picoMicrofluorTransferJaxb.setBatchId(labBatchId);
        bettaLimsMessageTestFactory.addMessage(messageList, picoMicrofluorTransferJaxb);

        // 6 x PicoStandardsTransfer
        picoStandardsTransferCol2Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
        picoStandardsTransferCol2Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
        picoStandardsTransferCol2Jaxb.getPlate().setSection(SBSSection.P384_COL2_1INTERVAL_B.getSectionName());
        picoStandardsTransferCol2Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoStandardsTransferCol2Jaxb.setBatchId(labBatchId);

        picoStandardsTransferCol4Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
        picoStandardsTransferCol4Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
        picoStandardsTransferCol4Jaxb.getPlate().setSection(SBSSection.P384_COL4_1INTERVAL_B.getSectionName());
        picoStandardsTransferCol4Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoStandardsTransferCol4Jaxb.setBatchId(labBatchId);

        picoStandardsTransferCol6Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
        picoStandardsTransferCol6Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
        picoStandardsTransferCol6Jaxb.getPlate().setSection(SBSSection.P384_COL6_1INTERVAL_B.getSectionName());
        picoStandardsTransferCol6Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoStandardsTransferCol6Jaxb.setBatchId(labBatchId);

        picoStandardsTransferCol8Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
        picoStandardsTransferCol8Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
        picoStandardsTransferCol8Jaxb.getPlate().setSection(SBSSection.P384_COL8_1INTERVAL_B.getSectionName());
        picoStandardsTransferCol8Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoStandardsTransferCol8Jaxb.setBatchId(labBatchId);

        picoStandardsTransferCol10Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
        picoStandardsTransferCol10Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
        picoStandardsTransferCol10Jaxb.getPlate().setSection(SBSSection.P384_COL10_1INTERVAL_B.getSectionName());
        picoStandardsTransferCol10Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoStandardsTransferCol10Jaxb.setBatchId(labBatchId);

        picoStandardsTransferCol12Jaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoStandardsTransfer",
                "PicoStandardsPlate" + timestamp, picoMicrofluorPlateBarcode);
        picoStandardsTransferCol12Jaxb.getSourcePlate().setSection(SBSSection.P96_COL1.getSectionName());
        picoStandardsTransferCol12Jaxb.getPlate().setSection(SBSSection.P384_COL12_1INTERVAL_B.getSectionName());
        picoStandardsTransferCol12Jaxb.getPlate().setPhysType(StaticPlate.PlateType.Eppendorf384.getAutomationName());
        picoStandardsTransferCol12Jaxb.setBatchId(labBatchId);

        bettaLimsMessageTestFactory
                .addMessage(messageList, picoStandardsTransferCol2Jaxb,
                        picoStandardsTransferCol4Jaxb, picoStandardsTransferCol6Jaxb, picoStandardsTransferCol8Jaxb,
                        picoStandardsTransferCol10Jaxb, picoStandardsTransferCol12Jaxb);
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

    public PlateTransferEventType getPicoStandardsTransferCol2Jaxb() {
        return picoStandardsTransferCol2Jaxb;
    }

    public PlateTransferEventType getPicoStandardsTransferCol4Jaxb() {
        return picoStandardsTransferCol4Jaxb;
    }

    public PlateTransferEventType getPicoStandardsTransferCol6Jaxb() {
        return picoStandardsTransferCol6Jaxb;
    }

    public PlateTransferEventType getPicoStandardsTransferCol8Jaxb() {
        return picoStandardsTransferCol8Jaxb;
    }

    public PlateTransferEventType getPicoStandardsTransferCol10Jaxb() {
        return picoStandardsTransferCol10Jaxb;
    }

    public PlateTransferEventType getPicoStandardsTransferCol12Jaxb() {
        return picoStandardsTransferCol12Jaxb;
    }
}
