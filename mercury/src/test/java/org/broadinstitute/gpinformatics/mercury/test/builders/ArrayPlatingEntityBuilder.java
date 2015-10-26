package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds entity graphs for the Array Plating process.
 */
public class ArrayPlatingEntityBuilder {
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String testPrefix;

    public ArrayPlatingEntityBuilder( Map<String, BarcodedTube> mapBarcodeToTube,
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory, LabEventFactory labEventFactory,
            LabEventHandler labEventHandler, String testPrefix) {
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.testPrefix = testPrefix;
    }

    public ArrayPlatingEntityBuilder invoke() {
        ArrayPlatingJaxbBuilder arrayPlatingJaxbBuilder = new ArrayPlatingJaxbBuilder(bettaLimsMessageTestFactory,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix);
        arrayPlatingJaxbBuilder.invoke();

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        PlateTransferEventType arrayPlatingJaxb = arrayPlatingJaxbBuilder.getArrayPlatingJaxb();
        LabEvent arrayPlatingEvent = labEventFactory.buildFromBettaLims(
                arrayPlatingJaxb, mapBarcodeToVessel);
        labEventHandler.processEvent(arrayPlatingEvent);
        // asserts
        StaticPlate arrayPlatingPlate = (StaticPlate) arrayPlatingEvent.getTargetLabVessels().iterator().next();
        Assert.assertEquals(arrayPlatingPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        return this;
    }
}
