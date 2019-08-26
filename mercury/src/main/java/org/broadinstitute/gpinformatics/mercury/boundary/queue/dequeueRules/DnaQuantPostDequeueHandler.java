package org.broadinstitute.gpinformatics.mercury.boundary.queue.dequeueRules;

import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;

import java.util.List;

public class DnaQuantPostDequeueHandler extends AbstractPostDequeueHandler {

    @Override
    public void process(List<QueueEntity> completedQueueEntities) {
        // todo jmt add to plating queue?
    }
}
