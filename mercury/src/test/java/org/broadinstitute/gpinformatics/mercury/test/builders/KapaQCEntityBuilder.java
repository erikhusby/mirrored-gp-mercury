package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * User: jowalsh
 * Date: 6/2/14
 */
public class KapaQCEntityBuilder {
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final TubeFormation platingRack;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final String rackBarcode;
    private final String testPrefix;
    private StaticPlate dilutionPlate;
    private StaticPlate kapaQCPlate;

    public KapaQCEntityBuilder(Map<String, BarcodedTube> mapBarcodeToTube, TubeFormation platingRack,
                               BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                               LabEventFactory labEventFactory, LabEventHandler labEventHandler,
                               String rackBarcode, String testPrefix) {
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.platingRack = platingRack;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.rackBarcode = rackBarcode;
        this.testPrefix = testPrefix;
    }

    public KapaQCEntityBuilder invoke() {
        return invoke(false);
    }

    public KapaQCEntityBuilder invoke(boolean do48Sample) {
        KapaQCJaxbBuilder kapaQCJaxbBuilder = new KapaQCJaxbBuilder(
                bettaLimsMessageTestFactory, new ArrayList<>(mapBarcodeToTube.keySet()), rackBarcode, testPrefix
        ).invoke(do48Sample);

        LabEventTest.validateWorkflow("KapaQCDilutionPlateTransfer", mapBarcodeToTube.values());

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(platingRack.getLabel(), platingRack);
        for (BarcodedTube barcodedTube : platingRack.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent kapaDilutionTransfer = labEventFactory.buildFromBettaLims(
                kapaQCJaxbBuilder.getKapaQCDilutionPlateJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(kapaDilutionTransfer);

        //asserts
        dilutionPlate = (StaticPlate) kapaDilutionTransfer.getTargetLabVessels().iterator().next();
        Assert.assertEquals(dilutionPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        LabEventTest.validateWorkflow("KapaQCPlateSetup", mapBarcodeToTube.values());

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(dilutionPlate.getLabel(), dilutionPlate);

        //KapaQCSetup messages
        if(do48Sample) {
            LabEvent kapaQCSetup48A1Event = labEventFactory.buildFromBettaLims(
                    kapaQCJaxbBuilder.getKapaQCSetup48EventA1Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(kapaQCSetup48A1Event);
            kapaQCPlate = (StaticPlate) kapaQCSetup48A1Event.getTargetLabVessels().iterator().next();

            Set<SampleInstance> sampleInstancesInWell =
                    kapaQCPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");

            //shouldn't goto A02 yet
            sampleInstancesInWell =
                    kapaQCPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A02);
            Assert.assertEquals(sampleInstancesInWell.size(), 0, "Wrong number of sample instances in well");

            LabEvent kapaQCSetup48A2Event = labEventFactory.buildFromBettaLims(
                    kapaQCJaxbBuilder.getKapaQCSetup48EventA2Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(kapaQCSetup48A2Event);

            sampleInstancesInWell =
                    kapaQCPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A02);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");

            LabEvent kapaQCSetup48A13Event = labEventFactory.buildFromBettaLims(
                    kapaQCJaxbBuilder.getKapaQCSetup48EventA13Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(kapaQCSetup48A13Event);

            sampleInstancesInWell =
                    kapaQCPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A13);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");

            LabEvent kapaQCSetup48A14Event = labEventFactory.buildFromBettaLims(
                    kapaQCJaxbBuilder.getKapaQCSetup48EventA14Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(kapaQCSetup48A14Event);

            sampleInstancesInWell =
                    kapaQCPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A14);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");

        } else {
            LabEvent kapaQCSetup96A1Event = labEventFactory.buildFromBettaLims(
                    kapaQCJaxbBuilder.getKapaQCSetup96EventA1Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(kapaQCSetup96A1Event);
            kapaQCPlate = (StaticPlate) kapaQCSetup96A1Event.getTargetLabVessels().iterator().next();

            //Assertions
            Assert.assertEquals(kapaQCPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");
            Set<SampleInstance> sampleInstancesInWell =
                    kapaQCPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A01);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");

            //Transfer to 2nd quadrant
            LabEvent kapaQCSetup96A2Event = labEventFactory.buildFromBettaLims(
                    kapaQCJaxbBuilder.getKapaQCSetup96EventA2Jaxb(), mapBarcodeToVessel);
            labEventHandler.processEvent(kapaQCSetup96A2Event);
            kapaQCPlate = (StaticPlate) kapaQCSetup96A2Event.getTargetLabVessels().iterator().next();
            Assert.assertEquals(kapaQCPlate.getSampleInstances().size(), mapBarcodeToTube.size(),
                    "Wrong number of sample instances");
            sampleInstancesInWell =
                    kapaQCPlate.getContainerRole().getSampleInstancesAtPosition(VesselPosition.A02);
            Assert.assertEquals(sampleInstancesInWell.size(), 1, "Wrong number of sample instances in well");
        }

        return this;
    }

    public StaticPlate getKapaQCPlate() {
        return kapaQCPlate;
    }

    public StaticPlate getDilutionPlate() {
        return dilutionPlate;
    }
}
