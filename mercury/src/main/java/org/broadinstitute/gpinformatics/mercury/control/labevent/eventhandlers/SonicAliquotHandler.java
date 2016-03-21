package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.CherryPickSourceType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Handles an aliquot message for Sonic.  The source rack is a mix of CRSP and BSP tubes.  Creates a transfer
 * containing only the BSP tubes, so BSP can create SM-IDs for the daughters, and export them back to Mercury.
 */
public class SonicAliquotHandler extends AbstractEventHandler {

    @Inject
    private SamplesDaughterPlateHandler samplesDaughterPlateHandler;

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        // Convert the section transfer to a cherry pick, because BSP doesn't require a matching source rack for
        // cherry picks.
        PlateTransferEventType plateTransferEventType = (PlateTransferEventType) stationEvent;

        PlateCherryPickEvent plateCherryPickEvent = new PlateCherryPickEvent();
        plateCherryPickEvent.setEventType(plateTransferEventType.getEventType());
        plateCherryPickEvent.setOperator(plateTransferEventType.getOperator());

        PlateType sourcePlate = new PlateType();
        sourcePlate.setBarcode(plateTransferEventType.getSourcePlate().getBarcode());
        sourcePlate.setSection("ALL96");
        sourcePlate.setPhysType(plateTransferEventType.getSourcePlate().getPhysType());
        plateCherryPickEvent.getSourcePlate().add(sourcePlate);

        PositionMapType sourcePosMap = new PositionMapType();
        sourcePosMap.setBarcode(plateTransferEventType.getSourcePlate().getBarcode());
        plateCherryPickEvent.getSourcePositionMap().add(sourcePosMap);

        PlateType destPlate = new PlateType();
        destPlate.setBarcode(plateTransferEventType.getPlate().getBarcode());
        destPlate.setSection("ALL96");
        destPlate.setPhysType(plateTransferEventType.getPlate().getPhysType());
        plateCherryPickEvent.getPlate().add(destPlate);

        PositionMapType destPosMap = new PositionMapType();
        destPosMap.setBarcode(plateTransferEventType.getPlate().getBarcode());
        plateCherryPickEvent.getPositionMap().add(destPosMap);

        // Allow random access to source tubes
        Map<String, LabVessel> mapBarcodeToVessel = new HashMap<>();
        TubeFormation tubeFormation = (TubeFormation) targetEvent.getSourceLabVessels().iterator().next();
        for (LabVessel labVessel : tubeFormation.getContainerRole().getContainedVessels()) {
            mapBarcodeToVessel.put(labVessel.getLabel(), labVessel);
        }

        // Allow random access to destination tubes by position
        Map<String, ReceptacleType> mapPositionToReceptacle = new HashMap<>();
        for (ReceptacleType receptacleType : plateTransferEventType.getPositionMap().getReceptacle()) {
            mapPositionToReceptacle.put(receptacleType.getPosition(), receptacleType);
        }

        // Transfer tubes that are in BSP
        for (ReceptacleType receptacleType : plateTransferEventType.getSourcePositionMap().getReceptacle()) {
            LabVessel labVessel = mapBarcodeToVessel.get(receptacleType.getBarcode());
            Set<SampleInstanceV2> sampleInstancesV2 = labVessel.getSampleInstancesV2();
            if (sampleInstancesV2.size() != 1) {
                throw new RuntimeException("Expected 1 sample, found " + sampleInstancesV2.size());
            }
            SampleInstanceV2 sampleInstanceV2 = sampleInstancesV2.iterator().next();
            if (sampleInstanceV2.getRootMercurySamples().iterator().next().getMetadataSource() ==
                    MercurySample.MetadataSource.BSP) {
                ReceptacleType sourceRecep = new ReceptacleType();
                sourceRecep.setBarcode(receptacleType.getBarcode());
                sourceRecep.setPosition(receptacleType.getPosition());
                sourceRecep.setVolume(receptacleType.getVolume());
                sourceRecep.setReceptacleType(receptacleType.getReceptacleType());
                sourcePosMap.getReceptacle().add(sourceRecep);

                ReceptacleType destRecep = new ReceptacleType();
                destRecep.setBarcode(mapPositionToReceptacle.get(receptacleType.getPosition()).getBarcode());
                destRecep.setPosition(receptacleType.getPosition());
                destRecep.setVolume(mapPositionToReceptacle.get(receptacleType.getPosition()).getVolume());
                destRecep.setReceptacleType(receptacleType.getReceptacleType());
                destPosMap.getReceptacle().add(destRecep);

                CherryPickSourceType cherryPickSourceType = new CherryPickSourceType();
                cherryPickSourceType.setBarcode(sourcePlate.getBarcode());
                cherryPickSourceType.setWell(receptacleType.getPosition());
                cherryPickSourceType.setDestinationBarcode(destPlate.getBarcode());
                cherryPickSourceType.setDestinationWell(receptacleType.getPosition());
                plateCherryPickEvent.getSource().add(cherryPickSourceType);
            }
        }

        // Forward to BSP
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.getPlateCherryPickEvent().add(plateCherryPickEvent);
        samplesDaughterPlateHandler.postToBsp(bettaLIMSMessage, SamplesDaughterPlateHandler.BSP_TRANSFER_REST_URL);
    }
}
