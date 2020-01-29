package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SingleCellVdjEnrichmentEntityBuilder {

    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String testPrefix;
    private final TubeFormation tcrTubeFormation;
    private final TubeFormation bcrTubeFormation;
    private final Map<String, BarcodedTube> mapBarcodeToTubeTcr;
    private final Map<String, BarcodedTube> mapBarcodeToTubeBcr;
    private StaticPlate tcrPcr1Plate;
    private StaticPlate tcrSpriPlate;
    private StaticPlate tcrPcr2Plate;
    private StaticPlate tcrSpri2Plate;
    private TubeFormation tcrRegistrationTubeFormation;
    private StaticPlate bcrPcr1Plate;
    private StaticPlate bcrSpriPlate;
    private StaticPlate bcrPcr2Plate;
    private StaticPlate bcrSpri2Plate;
    private TubeFormation bcrRegistrationTubeFormation;

    public SingleCellVdjEnrichmentEntityBuilder(TubeFormation tcrTubeFormation, TubeFormation bcrTubeFormation,
                                                Map<String, BarcodedTube> mapBarcodeToTubeTcr, Map<String, BarcodedTube> mapBarcodeToTubeBcr,
                                                BettaLimsMessageTestFactory bettaLimsMessageTestFactory, LabEventFactory labEventFactory,
                                                LabEventHandler labEventHandler, String testPrefix) {
        this.tcrTubeFormation = tcrTubeFormation;
        this.bcrTubeFormation = bcrTubeFormation;
        this.mapBarcodeToTubeTcr = mapBarcodeToTubeTcr;
        this.mapBarcodeToTubeBcr = mapBarcodeToTubeBcr;
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.testPrefix = testPrefix;
    }

    public SingleCellVdjEnrichmentEntityBuilder invoke() {
        final String indexPlateBarcode = "Vdj10XIndexPlate" + testPrefix;
        List<StaticPlate> indexPlatesList = LabEventTest.buildIndexPlate(null, null,
                new ArrayList<MolecularIndexingScheme.IndexPosition>() {{
                    add(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7);
                }},
                new ArrayList<String>() {{
                    add(indexPlateBarcode);
                }}
        );

        String tcrRackBarcode = testPrefix + "TcrDaughterRack";
        String bcrRackBarcode = testPrefix + "BcrDaughterRack";
        SingleCellVDJJaxbBuilder jaxbBuilder = new SingleCellVDJJaxbBuilder(bettaLimsMessageTestFactory, testPrefix,
                tcrRackBarcode, new ArrayList<>(mapBarcodeToTubeTcr.keySet()), bcrRackBarcode,
                new ArrayList<>(mapBarcodeToTubeBcr.keySet()), indexPlateBarcode).invoke();

        // Validate TCR First
        LabEventTest.validateWorkflow("TcrPcr1", mapBarcodeToTubeTcr.values());
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(tcrTubeFormation.getLabel(), tcrTubeFormation);
        for (BarcodedTube barcodedTube : tcrTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        PlateTransferEventType tcrPcr1 = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("TcrPcr1");
        LabEvent tcrPcr1Event = labEventFactory.buildFromBettaLims(
                tcrPcr1, mapBarcodeToVessel);
        labEventHandler.processEvent(tcrPcr1Event);
        tcrPcr1Plate = (StaticPlate) tcrPcr1Event.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("TcrSpri1", tcrPcr1Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(tcrPcr1Plate.getLabel(), tcrPcr1Plate);
        PlateTransferEventType tcrSpri1 = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("TcrSpri1");
        LabEvent tcrSpri1Event = labEventFactory.buildFromBettaLims(tcrSpri1, mapBarcodeToVessel);
        labEventHandler.processEvent(tcrSpri1Event);
        tcrSpriPlate = (StaticPlate) tcrSpri1Event.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("TcrPcr2", tcrSpriPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(tcrSpriPlate.getLabel(), tcrSpriPlate);
        PlateTransferEventType tcrPcr2 = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("TcrPcr2");
        LabEvent tcrPcr2Event = labEventFactory.buildFromBettaLims(tcrPcr2, mapBarcodeToVessel);
        labEventHandler.processEvent(tcrPcr2Event);
        tcrPcr2Plate = (StaticPlate) tcrPcr2Event.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("TcrSpri2", tcrPcr2Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(tcrPcr2Plate.getLabel(), tcrPcr2Plate);
        PlateTransferEventType tcrSpri2 = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("TcrSpri2");
        LabEvent tcrSpri2Event = labEventFactory.buildFromBettaLims(tcrSpri2, mapBarcodeToVessel);
        labEventHandler.processEvent(tcrSpri2Event);
        tcrSpri2Plate = (StaticPlate) tcrSpri2Event.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("TcrRegistration", tcrSpri2Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(tcrSpri2Plate.getLabel(), tcrSpri2Plate);
        PlateTransferEventType tcrRegistration = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("TcrRegistration");
        LabEvent tcrRegistrationEvent = labEventFactory.buildFromBettaLims(tcrRegistration, mapBarcodeToVessel);
        labEventHandler.processEvent(tcrRegistrationEvent);
        tcrRegistrationTubeFormation = (TubeFormation) tcrRegistrationEvent.getTargetLabVessels().iterator().next();

        // BCR Plates
        LabEventTest.validateWorkflow("BcrPcr1", mapBarcodeToTubeBcr.values());
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(bcrTubeFormation.getLabel(), bcrTubeFormation);
        for (BarcodedTube barcodedTube : bcrTubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }

        PlateTransferEventType BcrPcr1 = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("BcrPcr1");
        LabEvent BcrPcr1Event = labEventFactory.buildFromBettaLims(
                BcrPcr1, mapBarcodeToVessel);
        labEventHandler.processEvent(BcrPcr1Event);
        bcrPcr1Plate = (StaticPlate) BcrPcr1Event.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("BcrSpri1", bcrPcr1Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(bcrPcr1Plate.getLabel(), bcrPcr1Plate);
        PlateTransferEventType BcrSpri1 = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("BcrSpri1");
        LabEvent BcrSpri1Event = labEventFactory.buildFromBettaLims(BcrSpri1, mapBarcodeToVessel);
        labEventHandler.processEvent(BcrSpri1Event);
        bcrSpriPlate = (StaticPlate) BcrSpri1Event.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("BcrPcr2", bcrSpriPlate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(bcrSpriPlate.getLabel(), bcrSpriPlate);
        PlateTransferEventType BcrPcr2 = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("BcrPcr2");
        LabEvent BcrPcr2Event = labEventFactory.buildFromBettaLims(BcrPcr2, mapBarcodeToVessel);
        labEventHandler.processEvent(BcrPcr2Event);
        bcrPcr2Plate = (StaticPlate) BcrPcr2Event.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("BcrSpri2", bcrPcr2Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(bcrPcr2Plate.getLabel(), bcrPcr2Plate);
        PlateTransferEventType BcrSpri2 = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("BcrSpri2");
        LabEvent BcrSpri2Event = labEventFactory.buildFromBettaLims(BcrSpri2, mapBarcodeToVessel);
        labEventHandler.processEvent(BcrSpri2Event);
        bcrSpri2Plate = (StaticPlate) BcrSpri2Event.getTargetLabVessels().iterator().next();

        LabEventTest.validateWorkflow("BcrRegistration", bcrSpri2Plate);
        mapBarcodeToVessel.clear();
        mapBarcodeToVessel.put(bcrSpri2Plate.getLabel(), bcrSpri2Plate);
        PlateTransferEventType BcrRegistration = (PlateTransferEventType) jaxbBuilder.getJaxbFromName("BcrRegistration");
        LabEvent BcrRegistrationEvent = labEventFactory.buildFromBettaLims(BcrRegistration, mapBarcodeToVessel);
        labEventHandler.processEvent(BcrRegistrationEvent);
        bcrRegistrationTubeFormation = (TubeFormation) BcrRegistrationEvent.getTargetLabVessels().iterator().next();

        return this;
    }
}
