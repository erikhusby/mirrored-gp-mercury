package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

/**
 * This abstract class defines the makeup of each event handler.
 */
public abstract class AbstractEventHandler {

    /**
     * The handleEvent method is where each event type handler will define the logic to add extra validation for a
     * specific lab event type, or specific handling of the message, or both
     * @param targetEvent Lab event created with the processed bettalims message.  From here, we can accessed the
     *                    vessels that were referenced/created for the incoming message.
     * @param stationEvent Event Type representation of the bettalims message.  From here, we can access any extra data
     *                     that is not captured in the Lab Event (e.g. ReceptacleType Metadata)
     */
    public abstract void handleEvent(LabEvent targetEvent, StationEventType stationEvent) ;

}
