package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Builds entity graphs for the Fingerprinting process.
 */
public class FingerprintingEntityBuilder {
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final StaticPlate sourceplate;
    private final String testPrefix;
    private FingerprintingJaxbBuilder fingerprintingJaxbBuilder;
    private LabEvent staAdditionEvent;
    private LabEvent slrDilutionEvent;
    private StaticPlate slrDilutionPlate;
    private LabEvent chipTransferEvent;
    private StaticPlate ifcChip;

    public FingerprintingEntityBuilder(
            BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
            LabEventFactory labEventFactory,
            LabEventHandler labEventHandler, StaticPlate sourceplate, String testPrefix) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.sourceplate = sourceplate;
        this.testPrefix = testPrefix;
    }

    public FingerprintingEntityBuilder invoke() {
        fingerprintingJaxbBuilder = new FingerprintingJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                sourceplate.getLabel(), sourceplate.getSampleInstanceCount(),
                Collections.singletonList(Triple.of("STA", "1234-STA", 1))
        );
        fingerprintingJaxbBuilder.invoke();

        LabEventTest.validateWorkflow("FingerprintingSTAAddition", sourceplate);
        staAdditionEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                fingerprintingJaxbBuilder.getFingerprintingSTAAdditionJaxb(), sourceplate);
        labEventHandler.processEvent(staAdditionEvent);

        LabEventTest.validateWorkflow("FingerprintingPostSTAAdditionThermoCyclerLoaded", sourceplate);
        LabEvent thermocyclerEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                fingerprintingJaxbBuilder.getFingerprintingPostSTAAdditionThermoCyclerJaxb(), sourceplate);
        labEventHandler.processEvent(thermocyclerEvent);

        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(sourceplate.getLabel(), sourceplate);

        LabEventTest.validateWorkflow("FingerprintingSLRDilution", sourceplate);
        slrDilutionEvent = labEventFactory.buildFromBettaLims(
                fingerprintingJaxbBuilder.getFingerprintingSlrDilution(), mapBarcodeToVessel);
        labEventHandler.processEvent(slrDilutionEvent);

        slrDilutionPlate = (StaticPlate) slrDilutionEvent.getTargetLabVessels().iterator().next();
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(slrDilutionPlate.getLabel(), slrDilutionPlate);

        LabEventTest.validateWorkflow("FingerprintingIFCTransfer", slrDilutionPlate);
        chipTransferEvent = labEventFactory.buildFromBettaLims(
                fingerprintingJaxbBuilder.getFingerprintingIFCTransfer(), mapBarcodeToVessel);
        labEventHandler.processEvent(chipTransferEvent);
        ifcChip = (StaticPlate) chipTransferEvent.getTargetLabVessels().iterator().next();
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(ifcChip.getLabel(), ifcChip);

        LabEventTest.validateWorkflow("FingerprintingFC1Loaded", ifcChip);
        LabEvent fc1LoadedEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                fingerprintingJaxbBuilder.getFingerprintingFC1Loaded(), ifcChip);
        labEventHandler.processEvent(fc1LoadedEvent);
        return this;
    }

    public LabEvent getStaAdditionEvent() {
        return staAdditionEvent;
    }

    public LabEvent getSlrDilutionEvent() {
        return slrDilutionEvent;
    }

    public StaticPlate getSlrDilutionPlate() {
        return slrDilutionPlate;
    }

    public LabEvent getChipTransferEvent() {
        return chipTransferEvent;
    }

    public StaticPlate getIfcChip() {
        return ifcChip;
    }
}
