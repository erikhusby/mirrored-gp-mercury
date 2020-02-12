package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;

public class FingerprintingQueueValidator implements AbstractQueueValidator {

    @Inject
    private BSPSampleDataFetcher sampleDataFetcher;

    @Override
    public Map<Long, ValidationResult> validatePreEnqueue(Collection<LabVessel> labVessels, MessageCollection messageCollection) {
        return  DnaQuantQueueValidator.validateDna(labVessels, sampleDataFetcher);
    }

    @Override
    public boolean isComplete(LabVessel labVessel, MessageCollection messageCollection) {
        return true;
    }
}
