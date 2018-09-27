package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

public abstract class AbstractQueueValidator {

    public abstract void validatePreEnqueue(LabVessel labVessel, MessageCollection messageCollection);

    public abstract boolean isComplete(LabVessel labVessel, MessageCollection messageCollection);

}
