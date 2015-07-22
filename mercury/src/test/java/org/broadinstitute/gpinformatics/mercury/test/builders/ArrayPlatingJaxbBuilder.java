package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds JAXB objects for Array Plating messages.
 */
public class ArrayPlatingJaxbBuilder {

    private final List<BettaLIMSMessage> messageList = new ArrayList<>();
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final List<String> tubeBarcodeList;
    private final String testPrefix;
    private String rackBarcode;
    private PlateTransferEventType arrayPlatingJaxb;

    public ArrayPlatingJaxbBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, List<String> tubeBarcodeList, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.tubeBarcodeList = tubeBarcodeList;
        this.testPrefix = testPrefix;
    }

    public ArrayPlatingJaxbBuilder invoke() {
        rackBarcode = "ArrayPlatingRack" + testPrefix;
        String arrayPlatingPlate = testPrefix + "ArrayPlatingPlate";

        arrayPlatingJaxb = bettaLimsMessageTestFactory.buildRackToPlate("ArrayPlatingDilution", rackBarcode,
                tubeBarcodeList, arrayPlatingPlate);
        PositionMapType sourcePositionMap = arrayPlatingJaxb.getSourcePositionMap();
        PositionMapType destinationPositionMap = new PositionMapType();
        destinationPositionMap.setBarcode(arrayPlatingPlate);
        for(ReceptacleType receptacleType: sourcePositionMap.getReceptacle()) {
            ReceptacleType destinationReceptacle = new ReceptacleType();
            destinationReceptacle.setReceptacleType("Well [200uL]");
            destinationReceptacle.setPosition(receptacleType.getPosition());
            destinationReceptacle.setVolume(BigDecimal.valueOf(8));
            destinationReceptacle.setConcentration(BigDecimal.valueOf(20));
            destinationPositionMap.getReceptacle().add(destinationReceptacle);
        }
        arrayPlatingJaxb.setPositionMap(destinationPositionMap);

        bettaLimsMessageTestFactory.addMessage(messageList, arrayPlatingJaxb);

        return this;
    }

    public PlateTransferEventType getArrayPlatingJaxb() {
        return arrayPlatingJaxb;
    }

    public List<BettaLIMSMessage> getMessageList() {
        return messageList;
    }
}
