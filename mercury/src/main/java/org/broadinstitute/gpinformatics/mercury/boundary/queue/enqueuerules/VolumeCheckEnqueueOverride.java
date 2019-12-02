package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import java.util.Collection;

public class VolumeCheckEnqueueOverride extends AbstractEnqueueOverride {
    @Nonnull
    @Override
    public QueuePriority[] getQueuePriorityOrder() {
        return new QueuePriority[0];
    }

    @Override
    protected QueuePriority checkForSpecialPriorityType(Collection<MercurySample> mercurySamples) {
        return null;
    }
}
