package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;

import java.util.HashMap;
import java.util.Map;


/**
 * Builds entity graphs for the Stool Extraction to TNA process.
 */
public class StoolTNAEntityBuilder {
    private final StaticPlate sourcePlate;
    private final int numSamples;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String testPrefix;

    private LabEvent stoolExtractionSetupEvent;
    private StaticPlate stoolExtractionPlate;
    private LabEvent stoolFinalTransferEvent;
    private StaticPlate stoolFinalTransferPlate;
    private LabEvent stoolRNAAliquotEvent;
    private LabEvent stoolDNASpriCleanupEvent;
    private StaticPlate stoolRNAAliquotPlate;
    private LabEvent stoolRNASpriCleanupEvent;
    private TubeFormation stoolDNATubeRack;
    private TubeFormation stoolRNATubeRack;

    public StoolTNAEntityBuilder(StaticPlate sourcePlate, int numSamples,
                                 BettaLimsMessageTestFactory bettaLimsMessageTestFactory,
                                 LabEventFactory labEventFactory, LabEventHandler labEventHandler, String testPrefix) {
        this.sourcePlate = sourcePlate;
        this.numSamples = numSamples;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.testPrefix = testPrefix;
    }

    public StoolTNAEntityBuilder invoke() {
        StoolTNAJaxbBuilder stoolTNAJaxbBuilder = new StoolTNAJaxbBuilder(
                sourcePlate.getLabel(), numSamples, bettaLimsMessageTestFactory, testPrefix).invoke();
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();

        //StoolExtractionSetup
        mapBarcodeToVessel.put(sourcePlate.getLabel(), sourcePlate);
        stoolExtractionSetupEvent = labEventFactory.buildFromBettaLims(
                stoolTNAJaxbBuilder.getStoolExtractionSetupJaxb(), mapBarcodeToVessel);

        labEventHandler.processEvent(stoolExtractionSetupEvent);

        stoolExtractionPlate =
                (StaticPlate) stoolExtractionSetupEvent.getTargetLabVessels().iterator().next();

        //StoolFinalTransfer
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(stoolExtractionPlate.getLabel(), stoolExtractionPlate);
        stoolFinalTransferEvent = labEventFactory.buildFromBettaLims(
                stoolTNAJaxbBuilder.getStoolFinalTransferJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(stoolFinalTransferEvent);

        stoolFinalTransferPlate =
                (StaticPlate) stoolFinalTransferEvent.getTargetLabVessels().iterator().next();

        //RNA Aliquot
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(stoolFinalTransferPlate.getLabel(), stoolFinalTransferPlate);
        stoolRNAAliquotEvent = labEventFactory.buildFromBettaLims(
                stoolTNAJaxbBuilder.getStoolRnaAliquotJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(stoolRNAAliquotEvent);
        stoolRNAAliquotPlate =
                (StaticPlate) stoolRNAAliquotEvent.getTargetLabVessels().iterator().next();

        //RNase
        LabEvent rnaseEvent = labEventFactory.buildFromBettaLimsPlateEventDbFree(
                stoolTNAJaxbBuilder.getrNaseAdditionJaxb(), stoolFinalTransferPlate);
        labEventHandler.processEvent(rnaseEvent);

        //DNA Spri Cleanup
        stoolDNASpriCleanupEvent = labEventFactory.buildFromBettaLims(
                stoolTNAJaxbBuilder.getStoolDNACleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(stoolDNASpriCleanupEvent);
        stoolDNATubeRack = (TubeFormation) stoolDNASpriCleanupEvent.getTargetLabVessels().iterator().next();

        //RNA Spri Cleanup
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(stoolRNAAliquotPlate.getLabel(), stoolRNAAliquotPlate);
        stoolRNASpriCleanupEvent = labEventFactory.buildFromBettaLims(
                stoolTNAJaxbBuilder.getStoolRNACleanupJaxb(), mapBarcodeToVessel);
        labEventHandler.processEvent(stoolRNASpriCleanupEvent);
        stoolRNATubeRack = (TubeFormation) stoolRNASpriCleanupEvent.getTargetLabVessels().iterator().next();


        return this;
    }

    public TubeFormation getStoolDNATubeRack() {
        return stoolDNATubeRack;
    }

    public TubeFormation getStoolRNATubeRack() {
        return stoolRNATubeRack;
    }
}
