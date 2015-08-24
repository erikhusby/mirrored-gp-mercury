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
    private String riboDilutionPlateBarcode;
    private String riboMicrofluorPlateBarcode;
    private PlateEventType riboPlatingBucket;
    private PlateTransferEventType riboDilutionEventJaxb;
    private PlateTransferEventType riboMicrofluorEventJaxb;
    private PlateEventType riboBufferAdditionJaxb;
    private PlateEventType initialNormalizationJaxb;
    private String polyAAliquotRackBarcode;
    private PlateTransferEventType polyATSAliquot;

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

    public String getRiboDilutionPlateBarcode() {
        return riboDilutionPlateBarcode;
    }

    public String getRiboMicrofluorPlateBarcode() {
        return riboMicrofluorPlateBarcode;
    }

    public PlateEventType getRiboPlatingBucket() {
        return riboPlatingBucket;
    }

    public PlateTransferEventType getRiboDilutionEventJaxb() {
        return riboDilutionEventJaxb;
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

    public PlateEventType getInitialNormalizationJaxb() {
        return initialNormalizationJaxb;
    }

    public String getPolyAAliquotRackBarcode() {
        return polyAAliquotRackBarcode;
    }

    public PlateTransferEventType getPolyATSAliquot() {
        return polyATSAliquot;
    }

    public CrspRiboPlatingJaxbBuilder invoke() {
        riboPlatingBucket = bettaLimsMessageTestFactory
                .buildRackEvent("RiboPlatingBucket", rackBarcode, tubeBarcodes);
        bettaLimsMessageTestFactory.addMessage(messageList, riboPlatingBucket);

        riboDilutionPlateBarcode = "RiboDilutionPlate" + testPrefix;
        riboDilutionEventJaxb = bettaLimsMessageTestFactory.buildRackToPlate("PicoDilutionTransfer", rackBarcode,
                tubeBarcodes, riboDilutionPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, riboDilutionEventJaxb);

        riboMicrofluorPlateBarcode = "RiboMicrofluorTransfer" + testPrefix;
        riboMicrofluorEventJaxb = bettaLimsMessageTestFactory.buildPlateToPlate(
                "RiboMicrofluorTransfer", riboDilutionPlateBarcode, riboMicrofluorPlateBarcode);
        bettaLimsMessageTestFactory.addMessage(messageList, riboMicrofluorEventJaxb);

        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.add(Calendar.MONTH, 6);
        Date expiration = gregorianCalendar.getTime();

        riboBufferAdditionJaxb =
                bettaLimsMessageTestFactory.buildPlateEvent("RiboBufferAddition", riboMicrofluorPlateBarcode,
                        Arrays.asList(new BettaLimsMessageTestFactory.ReagentDto("RiboGreen", "1234-RiboGreen", expiration)));
        bettaLimsMessageTestFactory.addMessage(messageList, riboBufferAdditionJaxb);

        initialNormalizationJaxb = bettaLimsMessageTestFactory.buildRackEvent(LabEventType.INITIAL_NORMALIZATION.getName(),
                rackBarcode, tubeBarcodes);
        for (ReceptacleType receptacleType : initialNormalizationJaxb.getPositionMap().getReceptacle()) {
            BigDecimal conc = new BigDecimal("65.0");
            receptacleType.setConcentration(conc);
        }

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

        return this;
    }
}
