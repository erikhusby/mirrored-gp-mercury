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
    private Map<String, TwoDBarcodedTube> mapBarcodeToSageUnloadTubes;

    public SageEntityBuilder(BettaLimsMessageTestFactory bettaLimsMessageTestFactory, LabEventFactory labEventFactory,
                             LabEventHandler labEventHandler, String pondRegRackBarcode, TubeFormation pondRegRack, List<String> pondRegTubeBarcodes) {
        this.bettaLimsMessageTestFactory = bettaLimsMessageTestFactory;
        this.labEventFactory = labEventFactory;
        this.labEventHandler = labEventHandler;
        this.pondRegRackBarcode = pondRegRackBarcode;
        this.pondRegRack = pondRegRack;
        this.pondRegTubeBarcodes = pondRegTubeBarcodes;
    }


    public SageEntityBuilder invoke() {
        List<String> sageUnloadTubeBarcodes = new ArrayList<String>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageUnloadTubeBarcodes.add("SageUnload" + i);
        }
        String sageUnloadBarcode = "SageUnload";
        mapBarcodeToSageUnloadTubes = new HashMap<String, TwoDBarcodedTube>();
        RackOfTubes targetRackOfTubes = null;
        for (int i = 0; i < NUM_POSITIONS_IN_RACK / 4; i++) {
            // SageLoading
            String sageCassetteBarcode = "SageCassette" + i;
            PlateTransferEventType sageLoadingJaxb = bettaLimsMessageTestFactory.buildRackToPlate("SageLoading",
                    pondRegRackBarcode,
                    pondRegTubeBarcodes.subList(i * 4, i * 4 + 4),
                    sageCassetteBarcode);
            // todo jmt SAGE section
            LabEvent sageLoadingEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(sageLoadingJaxb,
                    pondRegRack, null);
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
            LabEvent sageUnloadEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(sageUnloadingJaxb,
                    sageCassette, mapBarcodeToSageUnloadTubes, targetRackOfTubes);
            labEventHandler.processEvent(sageUnloadEntity);
            sageUnloadEntity.getTargetLabVessels().iterator().next();
        }

        // SageCleanup
        sageCleanupTubeBarcodes = new ArrayList<String>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageCleanupTubeBarcodes.add("SageCleanup" + i);
        }
        sageCleanupBarcode = "SageCleanup";
        PlateTransferEventType sageCleanupJaxb = bettaLimsMessageTestFactory.buildRackToRack("SageCleanup", sageUnloadBarcode,
                sageUnloadTubeBarcodes, sageCleanupBarcode, sageCleanupTubeBarcodes);
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
        List<TwoDBarcodedTube> sageUnloadTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToSageUnloadTubes.values());
        for (int i = 0; i < NUM_POSITIONS_IN_RACK; i++) {
            mapPositionToTube.put(VesselPosition.getByName(bettaLimsMessageTestFactory.buildWellName(i + 1)),
                    sageUnloadTubes.get(i));
        }
        TubeFormation sageUnloadRackRearrayed = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        sageUnloadRackRearrayed.addRackOfTubes(new RackOfTubes("sageUnloadRearray", RackOfTubes.RackType.Matrix96));
        LabEvent sageCleanupEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(sageCleanupJaxb,
                sageUnloadRackRearrayed, new HashMap<String, TwoDBarcodedTube>(), targetRackOfTubes);
        labEventHandler.processEvent(sageCleanupEntity);
        sageCleanupRack = (TubeFormation) sageCleanupEntity.getTargetLabVessels().iterator().next();

        return this;
    }

    public TubeFormation getSageCleanupRack() {
        return sageCleanupRack;
    }

    public String getSageCleanupBarcode() {
        return sageCleanupBarcode;
    }

    public List<String> getSageCleanupTubeBarcodes() {
        return sageCleanupTubeBarcodes;
    }

    public Map<String, TwoDBarcodedTube> getMapBarcodeToSageUnloadTubes() {
        return mapBarcodeToSageUnloadTubes;
    }
}
