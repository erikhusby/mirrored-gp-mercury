package org.broadinstitute.gpinformatics.mercury.control.dao.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueue;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Set;

public interface WorkQueueDAO {

    /**
     * Find the {@link LabWorkQueue}s that currently contain
     * vessel for the given workflow.  We expect {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler}
     * to use this method to figure out whether to set the
     * {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan} override
     * via {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent#getProjectPlanOverride()}.
     * @param vessel
     * @return
     */
    public Set<LabWorkQueue> getPendingQueues(LabVessel vessel,WorkflowDescription workflow);
}
