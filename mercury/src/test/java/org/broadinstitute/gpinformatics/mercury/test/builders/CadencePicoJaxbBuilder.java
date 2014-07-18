package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.MetadataType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReagentType;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * User: jowalsh
 * Date: 5/7/14
 */
public class CadencePicoJaxbBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String testPrefix;
    private final List<String> sampleTubeBarcodes;
    private final String sampleRackBarcode;
    private final double dilutionFactor;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();

    private PlateTransferEventType picoDilutionTransferJaxb;
    private PlateTransferEventType picoMicrofluorTransferJaxb;
    private PlateEventType picoBufferAdditionJaxb;
    private String picoDilutionBarcode;
    private String picoMicrofluorBarcode;

    public CadencePicoJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, String testPrefix,
            List<String> sourceTubeBarcodes, String sourceRackBarcode, double dilutionFactor){
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.testPrefix = testPrefix;
        this.sampleTubeBarcodes = sourceTubeBarcodes;
        this.sampleRackBarcode = sourceRackBarcode;
        this.dilutionFactor = dilutionFactor;
    }

    public CadencePicoJaxbBuilder invoke(){
        picoDilutionBarcode = "picoDilutionPlate" + testPrefix;

        picoDilutionTransferJaxb = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer",
                sampleRackBarcode,
                sampleTubeBarcodes, picoDilutionBarcode);

        MetadataType dilutionFactorType = new MetadataType();
        dilutionFactorType.setName("DilutionFactor");
        dilutionFactorType.setValue(String.valueOf(this.dilutionFactor));
        picoDilutionTransferJaxb.getMetadata().add(dilutionFactorType);

        bettaLimsMessageTestFactory.addMessage(messageList, picoDilutionTransferJaxb);

        picoMicrofluorBarcode = "picoMicrofluorBarcode" + testPrefix;
        picoMicrofluorTransferJaxb = bettaLimsMessageTestFactory.buildPlateToPlate("PicoMicrofluorTransfer",
                picoDilutionBarcode, picoMicrofluorBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, picoMicrofluorTransferJaxb);

        picoBufferAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("PicoBufferAddition", picoMicrofluorBarcode);
        String bufferReagentBarcode = "PicoReagent" + testPrefix;
        ReagentType bufferReagent = new ReagentType();
        bufferReagent.setKitType("PICO");
        bufferReagent.setBarcode(bufferReagentBarcode);
        try {
            GregorianCalendar gregorianCalendar = new GregorianCalendar();
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            XMLGregorianCalendar now =
                    datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
            bufferReagent.setExpiration(now);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        picoBufferAdditionJaxb.getReagent().add(bufferReagent);
        bettaLimsMessageTestFactory.addMessage(messageList, picoBufferAdditionJaxb);

        return this;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public PlateTransferEventType getPicoDilutionTransferJaxb() {
        return picoDilutionTransferJaxb;
    }

    public PlateTransferEventType getPicoMicrofluorTransferJaxb() {
        return picoMicrofluorTransferJaxb;
    }

    public PlateEventType getPicoBufferAdditionJaxb() {
        return picoBufferAdditionJaxb;
    }

    public String getPicoDilutionBarcode() {
        return picoDilutionBarcode;
    }

    public String getPicoMicrofluorBarcode() {
        return picoMicrofluorBarcode;
    }
}