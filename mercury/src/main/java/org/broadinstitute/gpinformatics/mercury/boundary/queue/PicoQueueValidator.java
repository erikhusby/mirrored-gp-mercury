package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

public class PicoQueueValidator extends AbstractQueueValidator {

    @Override
    public void validatePreEnqueue(LabVessel labVessel, MessageCollection messageCollection) {

    }

    @Override
    public boolean isComplete(LabVessel labVessel) {
        return true;
    }
}
