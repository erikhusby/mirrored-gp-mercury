package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.Map;

public interface AbstractQueueValidator {

    Map<Long, ValidationResult> validatePreEnqueue(Collection<LabVessel> labVessel, MessageCollection messageCollection);

    boolean isComplete(LabVessel labVessel, MessageCollection messageCollection);
}
