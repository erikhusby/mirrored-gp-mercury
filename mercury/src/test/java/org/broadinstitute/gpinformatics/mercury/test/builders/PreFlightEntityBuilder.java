package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds entity graph for Pre-flight events
 */
public class PreFlightEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory             labEventFactory;
    private final LabEventHandler             labEventHandler;

    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private       TubeFormation                 tubeFormation;
    private String rackBarcode;
    private String testPrefix;

    public PreFlightEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                  LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                                  Map<String, BarcodedTube> mapBarcodeToTube, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.testPrefix = testPrefix;
    }

    public PreFlightEntityBuilder invoke() {
        PreFlightJaxbBuilder
                preFlightJaxbBuilder = new PreFlightJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                new ArrayList<>(
                        mapBarcodeToTube.keySet()));
        preFlightJaxbBuilder.invoke();
        rackBarcode = preFlightJaxbBuilder.getRackBarcode();

        // PreflightPicoSetup 1
        LabEventTest.validateWorkflow("PreflightPicoSetup", mapBarcodeToTube.values());
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        LabEvent preflightPicoSetup1Entity = labEventFactory.buildFromBettaLims(
                preFlightJaxbBuilder.getPreflightPicoSetup1(), mapBarcodeToVessel);
        labEventHandler.processEvent(preflightPicoSetup1Entity);
        // asserts
        tubeFormation = (TubeFormation) preflightPicoSetup1Entity.getSourceLabVessels().iterator().next();
        StaticPlate preflightPicoSetup1Plate = (StaticPlate) preflightPicoSetup1Entity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(preflightPicoSetup1Plate.getSampleInstancesV2().size(),
                                   LabEventTest.NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

        // PreflightPicoSetup 2
        LabEventTest.validateWorkflow("PreflightPicoSetup", mapBarcodeToTube.values());
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(tubeFormation.getLabel(), tubeFormation);
        for (BarcodedTube barcodedTube : tubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent preflightPicoSetup2Entity = labEventFactory.buildFromBettaLims(
                preFlightJaxbBuilder.getPreflightPicoSetup2(), mapBarcodeToVessel);
        labEventHandler.processEvent(preflightPicoSetup2Entity);
        // asserts
        StaticPlate preflightPicoSetup2Plate =
                (StaticPlate) preflightPicoSetup2Entity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(preflightPicoSetup2Plate.getSampleInstancesV2().size(), LabEventTest.NUM_POSITIONS_IN_RACK,
                "Wrong number of sample instances");

        // PreflightNormalization
        LabEventTest.validateWorkflow("PreflightNormalization", mapBarcodeToTube.values());
        LabEvent preflightNormalization = labEventFactory.buildFromBettaLimsRackEventDbFree(
                preFlightJaxbBuilder.getPreflightNormalization(), tubeFormation, mapBarcodeToTube, null);
        labEventHandler.processEvent(preflightNormalization);
        // asserts
        Assert.assertEquals(tubeFormation.getSampleInstancesV2().size(),
                LabEventTest.NUM_POSITIONS_IN_RACK, "Wrong number of sample instances");

        // PreflightPostNormPicoSetup 1
        LabEventTest.validateWorkflow("PreflightPostNormPicoSetup", mapBarcodeToTube.values());
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(tubeFormation.getLabel(), tubeFormation);
        for (BarcodedTube barcodedTube : tubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        LabEvent preflightPostNormPicoSetup1Entity = labEventFactory.buildFromBettaLims(
                preFlightJaxbBuilder.getPreflightPostNormPicoSetup1(), mapBarcodeToVessel);
        labEventHandler.processEvent(preflightPostNormPicoSetup1Entity);
        // asserts
        StaticPlate preflightPostNormPicoSetup1Plate =
                (StaticPlate) preflightPostNormPicoSetup1Entity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(preflightPostNormPicoSetup1Plate.getSampleInstancesV2().size(), LabEventTest.NUM_POSITIONS_IN_RACK,
                "Wrong number of sample instances");

        // PreflightPostNormPicoSetup 2
        LabEventTest.validateWorkflow("PreflightPostNormPicoSetup", mapBarcodeToTube.values());
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(tubeFormation.getLabel(), tubeFormation);
        for (BarcodedTube barcodedTube : tubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        LabEvent preflightPostNormPicoSetup2Entity = labEventFactory.buildFromBettaLims(
                preFlightJaxbBuilder.getPreflightPostNormPicoSetup2(), mapBarcodeToVessel);
        labEventHandler.processEvent(preflightPostNormPicoSetup2Entity);
        // asserts
        StaticPlate preflightPostNormPicoSetup2Plate =
                (StaticPlate) preflightPostNormPicoSetup2Entity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(preflightPostNormPicoSetup2Plate.getSampleInstancesV2().size(), LabEventTest.NUM_POSITIONS_IN_RACK,
                "Wrong number of sample instances");

        return this;
    }

    public String getRackBarcode() {
        return rackBarcode;
    }

    public TubeFormation getTubeFormation() {
        return tubeFormation;
    }
}
