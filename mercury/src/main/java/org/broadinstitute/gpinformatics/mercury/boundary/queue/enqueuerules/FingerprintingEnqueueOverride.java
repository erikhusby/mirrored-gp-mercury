package org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules;

import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import java.util.Collection;

public class FingerprintingEnqueueOverride extends AbstractEnqueueOverride {

    @Nonnull
    @Override
    public QueuePriority[] getQueuePriorityOrder() {
        return new QueuePriority[] { QueuePriority.CLIA, QueuePriority.EXOME_EXPRESS };
    }

    @Override
    protected QueuePriority checkForSpecialPriorityType(Collection<MercurySample> mercurySamples) {
        return null;
    }
}
