package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import javax.inject.Inject;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class EventHandlerSelector {

    @Inject
    DenatureToDilutionTubeHandler denatureToDilutionTubeHandler;

    @Inject
    FlowcellMessageHandler flowcellMessageHandler;

    public EventHandlerSelector() {

    }


    public void applyEventSpecificHandling(LabEvent targetEvent, StationEventType stationEvent) {
        AbstractEventHandler validator = null;

        switch (targetEvent.getLabEventType()) {
        case DENATURE_TO_DILUTION_TRANSFER:
            validator = denatureToDilutionTubeHandler;

            break;
        case DILUTION_TO_FLOWCELL_TRANSFER:
        case STRIP_TUBE_B_TRANSFER:
        case DENATURE_TO_FLOWCELL_TRANSFER:
        case REAGENT_KIT_TO_FLOWCELL_TRANSFER:
            validator =  flowcellMessageHandler;
            break;

        }

        if (validator != null) {
            validator.handleEvent(targetEvent, stationEvent);
        }
    }

    public void setDenatureToDilutionTubeHandler(DenatureToDilutionTubeHandler denatureToDilutionTubeHandler) {
        this.denatureToDilutionTubeHandler = denatureToDilutionTubeHandler;
    }
}
