package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

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
    private PlateTransferEventType riboMicrofluorEventJaxbA2;
    private PlateTransferEventType riboMicrofluorEventJaxbB1;
    private PlateEventType riboBufferAdditionJaxb;
    private String polyAAliquotRackBarcode;
    private PlateTransferEventType polyATSAliquot;
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

    public PlateTransferEventType getRiboMicrofluorEventJaxbA2() {
        return riboMicrofluorEventJaxbA2;
    }

    public PlateTransferEventType getRiboMicrofluorEventJaxbB1() {
        return riboMicrofluorEventJaxbB1;
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

    public CrspRiboPlatingJaxbBuilder invoke() {
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
        for (ReceptacleType receptacleType : polyATSAliquot.getPositionMap().getReceptacle()) {
            receptacleType.setVolume(new BigDecimal("50"));
        }
        bettaLimsMessageTestFactory.addMessage(messageList, polyATSAliquot);

        // RiboGreen
        // Pico plate barcode must be all numeric, otherwise upload parser will ignore it
        riboMicrofluorPlateBarcode = "55" + testPrefix;
        riboMicrofluorEventJaxbA2 = bettaLimsMessageTestFactory.buildRackToPlate(
                "RiboMicrofluorTransfer", rackBarcode, polyAAliquotTubes, riboMicrofluorPlateBarcode);
        riboMicrofluorEventJaxbA2.getPlate().setSection("P384_96TIP_1INTERVAL_A2");
        riboMicrofluorEventJaxbA2.getPlate().setPhysType("Eppendorf384");

        riboMicrofluorEventJaxbB1 = bettaLimsMessageTestFactory.buildRackToPlate(
                "RiboMicrofluorTransfer", rackBarcode, polyAAliquotTubes, riboMicrofluorPlateBarcode);
        riboMicrofluorEventJaxbB1.getPlate().setSection("P384_96TIP_1INTERVAL_B1");
        riboMicrofluorEventJaxbB1.getPlate().setPhysType("Eppendorf384");
        bettaLimsMessageTestFactory.addMessage(messageList, riboMicrofluorEventJaxbA2, riboMicrofluorEventJaxbB1);

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.add(Calendar.MONTH, 6);
        Date expiration = gregorianCalendar.getTime();

        riboBufferAdditionJaxb = bettaLimsMessageTestFactory.buildPlateEvent("RiboBufferAddition",
                riboMicrofluorPlateBarcode,
                Arrays.asList(new BettaLimsMessageTestFactory.ReagentDto("RiboGreen", "1234-RiboGreen", expiration)));
        bettaLimsMessageTestFactory.addMessage(messageList, riboBufferAdditionJaxb);

        polyASpikeJaxb = bettaLimsMessageTestFactory.buildRackEvent( "PolyATSAliquotSpike", rackBarcode, tubeBarcodes);
        for (ReceptacleType receptacleType : polyASpikeJaxb.getPositionMap().getReceptacle()) {
            BigDecimal conc = new BigDecimal("5.5");
            BigDecimal vol = new BigDecimal("56");
            receptacleType.setConcentration(conc);
            receptacleType.setVolume(vol);
        }
        bettaLimsMessageTestFactory.addMessage(messageList, polyASpikeJaxb);

        return this;
    }
}
