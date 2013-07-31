package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import javax.inject.Inject;

/**
 * TODO scottmat fill in javadoc!!!
 */
public class EventHandlerSelector {

    DenatureToDilutionTubeHandler denatureToDilutionTubeHandler;

    FlowcellMessageHandler flowcellMessageHandler;

    @Inject
    public EventHandlerSelector(
            DenatureToDilutionTubeHandler denatureToDilutionTubeHandler,
            FlowcellMessageHandler flowcellMessageHandler) {
        this.denatureToDilutionTubeHandler = denatureToDilutionTubeHandler;
        this.flowcellMessageHandler = flowcellMessageHandler;
    }

    public void applyEventSpecificHandling(LabEvent targetEvent, StationEventType stationEvent) {
        AbstractEventHandler validator = null;

        switch (targetEvent.getLabEventType()) {
        case DENATURE_TO_DILUTION_TRANSFER:
            validator = denatureToDilutionTubeHandler;

            break;
        case DILUTION_TO_FLOWCELL_TRANSFER:
        case FLOWCELL_TRANSFER:
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

    public FlowcellMessageHandler getFlowcellMessageHandler() {
        return flowcellMessageHandler;
    }
}
