package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;

import javax.inject.Inject;

/**
 * BettaLims messages that are processed in mercury are, for the most part, generic.  There is no specific validation
 * or processing per message that needs to happen.  The purpose of this method is to provide a structure for those
 * special cases where specific validation or handling needs to happen.
 *
 * By triggering off of the {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType}, we can
 * target each message at its own message validator.
 */
public class EventHandlerSelector {

    DenatureToDilutionTubeHandler denatureToDilutionTubeHandler;

    private FlowcellMessageHandler flowcellMessageHandler;
    private SamplesDaughterPlateHandler samplesDaughterPlateHandler;


    @Inject
    public EventHandlerSelector(
            DenatureToDilutionTubeHandler denatureToDilutionTubeHandler,
            FlowcellMessageHandler flowcellMessageHandler, SamplesDaughterPlateHandler samplesDaughterPlateHandler) {
        this.denatureToDilutionTubeHandler = denatureToDilutionTubeHandler;
        this.flowcellMessageHandler = flowcellMessageHandler;
        this.samplesDaughterPlateHandler = samplesDaughterPlateHandler;
    }

    /**
     * Primarily called in
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory#buildFromBettaLims}, this
     * method routes message specific handling of lab events to their appropriate handler based on the
     * {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType} associated with the given lab
     * event
     *
     * @param targetEvent Event that is to be processed.  This will have access to the existing/created entities that
     *                    need some action performed based on the message they were processed with
     * @param stationEvent This is the original JAXB message that was sent for processing.  It will typically contain
     *                     some extra information that may not make sense to store with the {@link LabEvent}.
     */
    public void applyEventSpecificHandling(LabEvent targetEvent, StationEventType stationEvent) {

        switch (targetEvent.getLabEventType()) {
        case DENATURE_TO_DILUTION_TRANSFER:
            denatureToDilutionTubeHandler.handleEvent(targetEvent, stationEvent);
            break;
        case DILUTION_TO_FLOWCELL_TRANSFER:
        case FLOWCELL_TRANSFER:
        case DENATURE_TO_FLOWCELL_TRANSFER:
        case REAGENT_KIT_TO_FLOWCELL_TRANSFER:
            flowcellMessageHandler.handleEvent(targetEvent, stationEvent);
            break;

        case AUTO_DAUGHTER_PLATE_CREATION:
            stationEvent.setEventType(LabEventType.SAMPLES_DAUGHTER_PLATE_CREATION.getName());
            break;
        }

        // For automated plate transfers in BSP, post the message to BSP PlateTransferResource.
        if (targetEvent.getLabEventType().isSendToBsp()) {
            samplesDaughterPlateHandler.postToBsp(stationEvent, SamplesDaughterPlateHandler.BSP_TRANSFER_REST_URL);
        }
    }

    public FlowcellMessageHandler getFlowcellMessageHandler() {
        return flowcellMessageHandler;
    }
}
