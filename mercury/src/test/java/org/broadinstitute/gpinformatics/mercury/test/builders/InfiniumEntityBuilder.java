package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    private List<StaticPlate> hybChips = new ArrayList<>();

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
        infiniumJaxbBuilder = new InfiniumJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                sourceplate.getLabCentricName(), sourceplate.getSampleInstanceCount(),
                Arrays.asList(Triple.of("NaOH", "1234-NaOH", 1), Triple.of("MA1", "1234-MA1", 2)),
                Arrays.asList(Triple.of("MA2", "1234-MA2", 3), Triple.of("MSM", "1234-MSM", 4)),
                Arrays.asList(Triple.of("FMS", "1234-FMS", 5)),
                Arrays.asList(Triple.of("PM1", "1234-PM1", 6)),
                Arrays.asList(Triple.of("Isopropanol", "2345", 7)),
                Arrays.asList(Triple.of("RA1", "1234-RA1", 8)),
                Arrays.asList(Triple.of("RA1", "1234-RA1", 9), Triple.of("LX1", "1234-LX1", 10),
                        Triple.of("LX2", "1234-LX2", 11), Triple.of("XC3", "1234-XC3", 12),
                        Triple.of("XC4", "1234-XC4", 13), Triple.of("SML", "1234-SML", 14),
                        Triple.of("ATM", "1234-ATM", 15), Triple.of("EML", "1234-EML", 16),
                        Triple.of("PB1", "1234-PB1", 17))
        );
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
        for (PlateCherryPickEvent plateCherryPickEvent : infiniumJaxbBuilder.getInfiniumHybridizationJaxbs()) {
            LabEvent hybEvent = labEventFactory.buildFromBettaLims(plateCherryPickEvent, mapBarcodeToVessel);
            labEventHandler.processEvent(hybEvent);
            hybChips.add((StaticPlate) hybEvent.getTargetLabVessels().iterator().next());
        }

        int i = 0;
        for (PlateEventType plateEventType : infiniumJaxbBuilder.getInfiniumPostHybridizationHybOvenLoadedJaxbs()) {
            LabEvent hybridizationHybOvenLoadedEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(plateEventType,
                    hybChips.get(i));
            labEventHandler.processEvent(hybridizationHybOvenLoadedEvent);
            i++;
        }

        i = 0;
        for (PlateEventType plateEventType : infiniumJaxbBuilder.getInfiniumHybChamberLoadedJaxbs()) {
            LabEvent hybChamberLoadedEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(plateEventType,
                    hybChips.get(i));
            labEventHandler.processEvent(hybChamberLoadedEvent);
            i++;
        }

        i = 0;
        for (PlateEventType plateEventType : infiniumJaxbBuilder.getInfiniumXStainJaxbs()) {
            LabEvent xstainEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(plateEventType, hybChips.get(i));
            labEventHandler.processEvent(xstainEvent);
            i++;
        }

        return this;
    }

    public List<StaticPlate> getHybChips() {
        return hybChips;
    }
}
