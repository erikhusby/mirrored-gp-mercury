package org.broadinstitute.sequel.control.dao.workflow;

import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.queue.LabWorkQueue;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Set;

public interface WorkQueueDAO {

    /**
     * Find the {@link LabWorkQueue}s that currently contain
     * vessel for the given workflow.  We expect {@link org.broadinstitute.sequel.control.labevent.LabEventHandler}
     * to use this method to figure out whether to set the
     * {@link org.broadinstitute.sequel.entity.project.BasicProjectPlan} override
     * via {@link org.broadinstitute.sequel.entity.labevent.LabEvent#getProjectPlanOverride()}.
     * @param vessel
     * @return
     */
    public Set<LabWorkQueue> getPendingQueues(LabVessel vessel,WorkflowDescription workflow);
}
