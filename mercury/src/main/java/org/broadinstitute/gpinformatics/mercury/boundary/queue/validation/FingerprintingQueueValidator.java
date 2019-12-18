package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class FingerprintingQueueValidator implements AbstractQueueValidator {

    @Override
    public Map<Long, ValidationResult> validatePreEnqueue(Collection<LabVessel> labVessels, MessageCollection messageCollection) {
        return Collections.emptyMap();
    }

    @Override
    public boolean isComplete(LabVessel labVessel, MessageCollection messageCollection) {
        return true;
    }
}
