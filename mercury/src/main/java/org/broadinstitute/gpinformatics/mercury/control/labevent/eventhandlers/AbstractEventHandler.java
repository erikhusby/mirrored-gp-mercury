package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

/**
 * BettaLims messages that are processed in mercury are, for the most part, generic.  There is no specific validation
 * or processing per message that needs to happen.  The purpose of this method is to provide a structure for those
 * special cases where specific validation or handling needs to happen.
 *
 * By triggering off of the {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType}, we can
 * target each message at it's own message validator.
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
