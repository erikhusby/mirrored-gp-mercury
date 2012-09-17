package org.broadinstitute.sequel.control.dao.workflow;

import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.queue.LabWorkQueue;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Set;

public class WorkQueueDAOImpl implements WorkQueueDAO {

    @Override
    public Set<LabWorkQueue> getPendingQueues(LabVessel vessel, WorkflowDescription workflow) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
