package org.broadinstitute.gpinformatics.mercury.boundary.queue.dequeueRules;

import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;

import java.util.List;

public abstract class AbstractPostDequeueHandler {
    public abstract void process(List<QueueEntity> completedQueueEntities);
}
