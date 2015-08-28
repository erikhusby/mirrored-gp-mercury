package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * Builds JAXB objects for Ribo Plating messages.
 */
public class CrspRiboPlatingJaxbBuilder {

    private final String rackBarcode;
    private final List<String> tubeBarcodes;
    private final String testPrefix;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private String riboMicrofluorPlateBarcode;
    private PlateTransferEventType riboMicrofluorEventJaxb;
    private PlateEventType riboBufferAdditionJaxb;
    private String polyAAliquotRackBarcode;
    private PlateTransferEventType polyATSAliquot;
    private PlateEventType truSeqStrandSpecificBucket;
    private PlateEventType polyASpikeJaxb;

    public CrspRiboPlatingJaxbBuilder(String rackBarcode, List<String> tubeBarcodes, String testPrefix,
                                      BettaLimsMessageTestFactory bettaLimsMessageTestFactory) {
        this.rackBarcode = rackBarcode;
        this.tubeBarcodes = tubeBarcodes;
        this.testPrefix = testPrefix;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public List<String> getTubeBarcodes() {
        return tubeBarcodes;
    }

    public String getTestPrefix() {
        return testPrefix;
    }

    public BettaLimsMessageTestFactory getBettaLimsMessageTestFactory() {
        return bettaLimsMessageTestFactory;
    }

    public String getRiboMicrofluorPlateBarcode() {
        return riboMicrofluorPlateBarcode;
    }

    public PlateTransferEventType getRiboMicrofluorEventJaxb() {
        return riboMicrofluorEventJaxb;
    }

    public PlateEventType getRiboBufferAdditionJaxb() {
        return riboBufferAdditionJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }

    public String getPolyAAliquotRackBarcode() {
        return polyAAliquotRackBarcode;
    }

    public PlateTransferEventType getPolyATSAliquot() {
        return polyATSAliquot;
    }

    public PlateEventType getPolyASpikeJaxb() {
        return polyASpikeJaxb;
    }

    public PlateEventType getTruSeqStrandSpecificBucket() {
        return truSeqStrandSpecificBucket;
    }

    public CrspRiboPlatingJaxbBuilder invoke() {
        truSeqStrandSpecificBucket = bettaLimsMessageTestFactory
                .buildRackEvent(LabEventType.TRU_SEQ_STRAND_SPECIFIC_BUCKET.getName(), rackBarcode, tubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, truSeqStrandSpecificBucket);

        //PolyATSAliquot
        List<String> polyAAliquotTubes = new ArrayList<>(tubeBarcodes.size());
        int i = 0;
        for(String tubeBarcode: tubeBarcodes) {
            polyAAliquotTubes.add(testPrefix + "PolyAAliquot" + i);
            i++;
        }
        polyAAliquotRackBarcode = "polyATSAliquot" + testPrefix;
        polyATSAliquot = bettaLimsMessageTestFactory.buildRackToRack("PolyATSAliquot",
                rackBarcode, tubeBarcodes, polyAAliquotRackBarcode, polyAAliquotTubes);

        //RiboGreen
        riboMicrofluorPlateBarcode = "RiboMicrofluorTransfer" + testPrefix;
        riboMicrofluorEventJaxb = bettaLimsMessageTestFactory.buildRackToPlate(
                "RiboMicrofluorTransfer", rackBarcode, polyAAliquotTubes, riboMicrofluorPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, riboMicrofluorEventJaxb);

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.add(Calendar.MONTH, 6);
        Date expiration = gregorianCalendar.getTime();

        riboBufferAdditionJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("RiboBufferAddition", riboMicrofluorPlateBarcode,
                        Arrays.asList(new BettaLimsMessageTestFactory.ReagentDto("RiboGreen", "1234-RiboGreen", expiration)));
        bettaLimsMessageTestFactory.addMessage(messageList, riboBufferAdditionJaxb);

        polyASpikeJaxb = bettaLimsMessageTestFactory.buildRackEvent(
                LabEventType.POLY_A_TS_ALIQUOT_SPIKE.getName(),
                rackBarcode, tubeBarcodes);
        for (ReceptacleType receptacleType : polyASpikeJaxb.getPositionMap().getReceptacle()) {
            BigDecimal conc = new BigDecimal("5.5");
            BigDecimal vol = new BigDecimal("56");
            receptacleType.setConcentration(conc);
            receptacleType.setVolume(vol);
        }

        return this;
    }
}
