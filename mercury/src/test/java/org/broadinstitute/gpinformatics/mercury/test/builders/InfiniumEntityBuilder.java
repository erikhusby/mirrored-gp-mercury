package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds entity graphs for the Infinium process.
 */
public class InfiniumEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final StaticPlate sourceplate;
    private final String testPrefix;
    private InfiniumJaxbBuilder infiniumJaxbBuilder;
    private LabEvent amplifcationEvent;
    private StaticPlate amplificationPlate;
    private StaticPlate hybChip;

    public InfiniumEntityBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory,
            LabEventHandler labEventHandler, StaticPlate sourceplate, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.sourceplate = sourceplate;
        this.testPrefix = testPrefix;
    }

    public InfiniumEntityBuilder invoke() {
        infiniumJaxbBuilder =
                new InfiniumJaxbBuilder(bettaLimsMessageTestFactory, testPrefix, sourceplate.getLabCentricName());
        infiniumJaxbBuilder.invoke();

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(sourceplate.getLabel(), sourceplate);
        amplifcationEvent = labEventFactory.buildFromBettaLims(
                infiniumJaxbBuilder.getInfiniumAmplificationJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(amplifcationEvent);

        amplificationPlate = (StaticPlate) amplifcationEvent.getTargetLabVessels().iterator().next();

        LabEvent ampReagentAdditionEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumAmplificationReagentAdditionJaxb(), amplificationPlate);
        labEventHandler.processEvent(ampReagentAdditionEvent);

        LabEvent fragmentationEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumFragmentationJaxb(), amplificationPlate);
        labEventHandler.processEvent(fragmentationEvent);

        LabEvent fragmentationHybOvenEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumPostFragmentationHybOvenLoadedJaxb(), amplificationPlate);
        labEventHandler.processEvent(fragmentationHybOvenEvent);

        LabEvent precipitationEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumPrecipitationJaxb(), amplificationPlate);
        labEventHandler.processEvent(precipitationEvent);

        LabEvent precipitationHeatBlockEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumPostPrecipitationHeatBlockLoadedJaxb(), amplificationPlate);
        labEventHandler.processEvent(precipitationHeatBlockEvent);

        LabEvent precipitationIsopropanolEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumPrecipitationIsopropanolAdditionJaxb(), amplificationPlate);
        labEventHandler.processEvent(precipitationIsopropanolEvent);

        LabEvent resuspensionEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumResuspensionJaxb(), amplificationPlate);
        labEventHandler.processEvent(resuspensionEvent);

        LabEvent resuspensionHybOvenEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumPostResuspensionHybOvenJaxb(), amplificationPlate);
        labEventHandler.processEvent(resuspensionHybOvenEvent);

        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(amplificationPlate.getLabel(), amplificationPlate);
        LabEvent hybEvent = labEventFactory.buildFromBettaLims(
                infiniumJaxbBuilder.getInfiniumHybridizationJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(hybEvent);
        hybChip = (StaticPlate) hybEvent.getTargetLabVessels().iterator().next();

        LabEvent hybridizationHybOvenLoadedEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumPostHybridizationHybOvenLoadedJaxb(), hybChip);
        labEventHandler.processEvent(hybridizationHybOvenLoadedEvent);

        LabEvent hybChamberLoadedEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumHybChamberLoadedJaxb(), hybChip);
        labEventHandler.processEvent(hybChamberLoadedEvent);

        LabEvent washEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumWashJaxb(), hybChip);
        labEventHandler.processEvent(washEvent);

        LabEvent xstainEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                infiniumJaxbBuilder.getInfiniumXStainJaxb(), hybChip);
        labEventHandler.processEvent(xstainEvent);

        return this;
    }

    public StaticPlate getHybChip() {
        return hybChip;
    }
}
