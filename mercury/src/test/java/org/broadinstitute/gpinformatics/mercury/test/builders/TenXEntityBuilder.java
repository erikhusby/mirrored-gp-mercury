package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds entity graphs for the Array Plating process.
 */
public class TenXEntityBuilder {
    private final Map<String, BarcodedTube> mapBarcodeToTube;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String testPrefix;
    private StaticPlate makePlate;
    private List<StaticPlate> chips = new ArrayList<>();

    public TenXEntityBuilder( Map<String, BarcodedTube> mapBarcodeToTube,
                                      BettaLimsMessageTestFactory bettaLimsMessageTestFactory, LabEventFactory labEventFactory,
                                      LabEventHandler labEventHandler, String testPrefix) {
        this.mapBarcodeToTube = mapBarcodeToTube;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.testPrefix = testPrefix;
    }

    public TenXEntityBuilder invoke() {
        TenXJaxbBuilder tenXJaxbBuilder = new TenXJaxbBuilder(bettaLimsMessageTestFactory,
                new ArrayList<>(mapBarcodeToTube.keySet()), testPrefix);
        tenXJaxbBuilder.invoke();

        // Make Plate
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.putAll(mapBarcodeToTube);
        LabEventTest.validateWorkflow("10XMakePlate", mapBarcodeToTube.values());
        PlateTransferEventType makePlateJaxb = tenXJaxbBuilder.getTenXMakePlateJaxb();
        LabEvent makePlateEvent = labEventFactory.buildFromBettaLims(
                makePlateJaxb, mapBarcodeToVessel);
        labEventHandler.processEvent(makePlateEvent);
        makePlate = (StaticPlate) makePlateEvent.getTargetLabVessels().iterator().next();

        // Chip Loading
        LabEventTest.validateWorkflow("10XChipLoading", makePlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(makePlate.getLabel(), makePlate);
        for (PlateTransferEventType chipLoadingJaxb : tenXJaxbBuilder.getChipLoadingJaxb()) {
            LabEvent chipLoadingEvent = labEventFactory.buildFromBettaLims(chipLoadingJaxb, mapBarcodeToVessel);
            labEventHandler.processEvent(chipLoadingEvent);
            StaticPlate chip = (StaticPlate) chipLoadingEvent.getTargetLabVessels().iterator().next();
            LabEventTest.validateWorkflow("10XChipUnloading", chip);
            chips.add(chip);
        }

        // Chip Unloading
        StaticPlate unloadingPlate = null;
        int i = 0;
        for (PlateTransferEventType chipUnloadingJaxb : tenXJaxbBuilder.getChipUnloadingJaxb()) {
            StaticPlate chip = chips.get(i);
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(chip.getLabel(), chip);
            if (unloadingPlate != null) {
                mapBarcodeToVessel.put(unloadingPlate.getLabel(), unloadingPlate);
            }
            LabEvent chipUnloadingEvent = labEventFactory.buildFromBettaLims(chipUnloadingJaxb, mapBarcodeToVessel);
            labEventHandler.processEvent(chipUnloadingEvent);
            if (unloadingPlate == null) {
                unloadingPlate = (StaticPlate) chipUnloadingEvent.getTargetLabVessels().iterator().next();
            }
            i++;
        }

        // Emulsion Breaking
        LabEventTest.validateWorkflow("10XEmulsionBreaking", unloadingPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(unloadingPlate.getLabel(), unloadingPlate);
        LabEvent emulsionBreakingEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                tenXJaxbBuilder.getEmulsionBreakingJaxb(), unloadingPlate);
        labEventHandler.processEvent(emulsionBreakingEvent);

        // Dynabead Cleanup
        LabEventTest.validateWorkflow("10XDynabeadCleanup", unloadingPlate);
        LabEvent dynabeadCleanupEvent = labEventFactory.buildFromBettaLims(tenXJaxbBuilder.getDynabeadCleanupJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(dynabeadCleanupEvent);

        StaticPlate cleanupPlate = (StaticPlate) dynabeadCleanupEvent.getTargetLabVessels().iterator().next();

        // Pre LC Spri
        LabEventTest.validateWorkflow("10XPreLCSpri", cleanupPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(cleanupPlate.getLabel(), cleanupPlate);
        LabEvent preLcSpriEvent = labEventFactory.buildFromBettaLims(tenXJaxbBuilder.getPreLCSpriJaxb(),
                mapBarcodeToVessel);
        labEventHandler.processEvent(preLcSpriEvent);

        StaticPlate preLcSpriPlate = (StaticPlate) preLcSpriEvent.getTargetLabVessels().iterator().next();

        Assert.assertEquals(preLcSpriPlate.getSampleInstancesV2().size(), mapBarcodeToTube.size(),
                "Wrong number of sample instances");

        return this;
    }
}
