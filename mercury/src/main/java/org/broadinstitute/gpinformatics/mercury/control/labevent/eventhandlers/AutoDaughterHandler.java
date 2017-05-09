package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;

import javax.inject.Inject;

/**
 * Handles an aliquot message.
 */
public class AutoDaughterHandler extends AbstractEventHandler {
    @Inject
    private BSPRestSender bspRestSender;

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        if (targetEvent.getSectionTransfers().size() != 1) {
            throw new RuntimeException("Found " + targetEvent.getSectionTransfers().size() + " section transfers.");
        }
        stationEvent.setEventType(LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName());
        // Forwards to BSP only if BSP knows the target plate.
        SectionTransfer sectionTransfer = targetEvent.getSectionTransfers().iterator().next();
        if (bspRestSender.vesselExists(sectionTransfer.getTargetVesselContainer().getSourceRack().getLabel())) {
            BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
            bettaLIMSMessage.getPlateTransferEvent().add((PlateTransferEventType)stationEvent);
            bspRestSender.postToBsp(bettaLIMSMessage, BSPRestSender.BSP_TRANSFER_REST_URL);
        }
    }
}
