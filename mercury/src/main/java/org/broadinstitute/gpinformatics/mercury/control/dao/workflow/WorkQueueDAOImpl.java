package org.broadinstitute.gpinformatics.mercury.control.dao.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Set;

public class WorkQueueDAOImpl implements WorkQueueDAO {

    @Override
    public Set<LabWorkQueue> getPendingQueues(LabVessel vessel, WorkflowDescription workflow) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
