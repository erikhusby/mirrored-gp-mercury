package org.broadinstitute.gpinformatics.mercury.test.builders;

import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class handles setting up entites for the Sage process events.
 */
public class SageEntityBuilder {
    private static final int NUM_POSITIONS_IN_RACK = 96;
    private final BettaLimsMessageTestFactory bettaLimsMessageTestFactory;
    private final LabEventFactory labEventFactory;
    private final LabEventHandler labEventHandler;
    private final String pondRegRackBarcode;
    private final TubeFormation pondRegRack;
    private final List<String> pondRegTubeBarcodes;
    private TubeFormation sageCleanupRack;
    private String sageCleanupBarcode;
    private List<String> sageCleanupTubeBarcodes;
    private Map<String, BarcodedTube> mapBarcodeToSageUnloadTubes;

    /**
     * Constructs a new SageEntityBuilder with entities from the previous process.
     *
     * @param bettaLimsMessageTestFactory The betta lims message factory that will create betta lims messages for this process.
     * @param labEventFactory             The lab event factory that will create the events for this process.
     * @param labEventHandler             The lab event handler that will process the events.
     * @param pondRegRack                 The pond registration rack coming out of the library construction process.
     * @param pondRegRackBarcode          The pond registration rack barcode.
     * @param pondRegTubeBarcodes         A list of pond registration tube barcodes.
     */
    public SageEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, LabEventFactory labEventFactory,
                             LabEventHandler labEventHandler, String pondRegRackBarcode, TubeFormation pondRegRack, List<String> pondRegTubeBarcodes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.pondRegRackBarcode = pondRegRackBarcode;
        this.pondRegRack = pondRegRack;
        this.pondRegTubeBarcodes = pondRegTubeBarcodes;
    }

    /**
     * Runs the entities passed in to the constructor through the Sage process.
     *
     * @return Returns the entity builder that contains the entities after this process has been invoked.
     */
    public SageEntityBuilder invoke() {
        List<String> sageUnloadTubeBarcodes = new ArrayList<>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageUnloadTubeBarcodes.add("SageUnload" + i);
        }
        mapBarcodeToSageUnloadTubes = new HashMap<>();
        String sageUnloadBarcode = "SageUnload";
        for (int i = 0; i < NUM_POSITIONS_IN_RACK / 4; i++) {
            // SageLoading
            String sageCassetteBarcode = "SageCassette" + i;
            PlateTransferEventType sageLoadingJaxb = bettaLimsMessageTestFactory.buildRackToPlate("SageLoading",
                    pondRegRackBarcode,
                    pondRegTubeBarcodes.subList(i * 4, i * 4 + 4),
                    sageCassetteBarcode);
            // todo jmt SAGE section
            Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
            mapBarcodeToVessel.put(pondRegRack.getLabel(), pondRegRack);
            for (BarcodedTube barcodedTube : pondRegRack.getContainerRole().getContainedVessels()) {
                mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
            }

            LabEvent sageLoadingEntity = labEventFactory.buildFromBettaLims(sageLoadingJaxb, mapBarcodeToVessel);
            labEventHandler.processEvent(sageLoadingEntity);
            StaticPlate sageCassette = (StaticPlate) sageLoadingEntity.getTargetLabVessels().iterator().next();

            // SageLoaded
            PlateEventType sageLoadedJaxb = bettaLimsMessageTestFactory.buildPlateEvent(
                    LabEventType.SAGE_LOADED.getName(), sageCassetteBarcode);
            LabEvent sageLoadedEntity = labEventFactory.buildFromBettaLimsPlateEventDbFree(sageLoadedJaxb, sageCassette);
            labEventHandler.processEvent(sageLoadedEntity);

            // SageUnloading
            PlateTransferEventType sageUnloadingJaxb = bettaLimsMessageTestFactory.buildPlateToRack("SageUnloading",
                    sageCassetteBarcode, sageUnloadBarcode, sageUnloadTubeBarcodes.subList(i * 4, i * 4 + 4));
            mapBarcodeToVessel.clear();
            mapBarcodeToVessel.put(sageCassette.getLabel(), sageCassette);
            LabEvent sageUnloadEntity = labEventFactory.buildFromBettaLims(sageUnloadingJaxb, mapBarcodeToVessel);
            labEventHandler.processEvent(sageUnloadEntity);
            TubeFormation unloadTubeFormation =
                    (TubeFormation) sageUnloadEntity.getTargetLabVessels().iterator().next();
            for (BarcodedTube barcodedTube : unloadTubeFormation.getContainerRole().getContainedVessels()) {
                mapBarcodeToSageUnloadTubes.put(barcodedTube.getLabel(), barcodedTube);
            }

        }

        // SageCleanup
        sageCleanupTubeBarcodes = new ArrayList<>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageCleanupTubeBarcodes.add("SageCleanup" + i);
        }
        sageCleanupBarcode = "SageCleanup";
        PlateTransferEventType sageCleanupJaxb = bettaLimsMessageTestFactory.buildRackToRack("SageCleanup", sageUnloadBarcode,
                sageUnloadTubeBarcodes, sageCleanupBarcode, sageCleanupTubeBarcodes);
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        List<BarcodedTube> sageUnloadTubes = new ArrayList<>(mapBarcodeToSageUnloadTubes.values());
        for (int i = 0; i < NUM_POSITIONS_IN_RACK; i++) {
            mapPositionToTube.put(VesselPosition.getByName(bettaLimsMessageTestFactory.buildWellName(i + 1,
                    BettaLimsMessageTestFactory.WellNameType.SHORT)),
                    sageUnloadTubes.get(i));
        }
        TubeFormation sageUnloadRackRearrayed = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        sageUnloadRackRearrayed.addRackOfTubes(new RackOfTubes("sageUnloadRearray", RackOfTubes.RackType.Matrix96));
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        mapBarcodeToVessel.put(sageUnloadRackRearrayed.getLabel(), sageUnloadRackRearrayed);
        for (BarcodedTube barcodedTube : sageUnloadRackRearrayed.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(barcodedTube.getLabel(), barcodedTube);
        }
        LabEvent sageCleanupEntity = labEventFactory.buildFromBettaLims(sageCleanupJaxb, mapBarcodeToVessel);
        labEventHandler.processEvent(sageCleanupEntity);
        sageCleanupRack = (TubeFormation) sageCleanupEntity.getTargetLabVessels().iterator().next();

        return this;
    }

    public TubeFormation getSageCleanupRack() {
        return sageCleanupRack;
    }

    public List<String> getSageCleanupTubeBarcodes() {
        return sageCleanupTubeBarcodes;
    }

    public Map<String, BarcodedTube> getMapBarcodeToSageUnloadTubes() {
        return mapBarcodeToSageUnloadTubes;
    }
}
