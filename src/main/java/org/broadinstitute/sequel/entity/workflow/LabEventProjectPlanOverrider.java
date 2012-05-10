package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import javax.inject.Inject;
import java.util.Collection;

/**
 * When someone puts a {@link LabVessel} into a
 * {@link LabBatch}, there is an opportunity to set
 * an "override" for the {@link ProjectPlan}, so that
 * a user can say "Do this {@link org.broadinstitute.sequel.entity.project.WorkflowDescription workflow},
 * but do it on behalf of a different {@link ProjectPlan}.
 */
public class LabEventProjectPlanOverrider {

    private LabBatchDAO labBatchDAO;

    public LabEventProjectPlanOverrider() {}

    public LabEventProjectPlanOverrider(LabBatchDAO labBatchDAO) {
        this.labBatchDAO = labBatchDAO;
    }

    /**
     * Figures out what (if any) {@link ProjectPlan} overrides
     * should be and sets them, per {@link LabVessel} in the
     * {@link LabEvent}
     * @param labEvent
     */
    public void setProjectPlanOverrides(LabEvent labEvent) {
        Collection<LabVessel> vesselsForBatch = null;
        if (!labEvent.getSourceLabVessels().isEmpty()) {
            vesselsForBatch = labEvent.getSourceLabVessels();
        }
        else {
            vesselsForBatch = labEvent.getTargetLabVessels();
        }
        Collection<LabBatch> possibleBatches = labBatchDAO.guessActiveBatchesForVessels(vesselsForBatch);

        if (possibleBatches.size() > 1) {
            // todo make this an alert message back to the jira ticket?
            throw new RuntimeException("Cannot resolve which of " + possibleBatches.size() + " batches to use");
        }
        else if (possibleBatches.size() == 1) {
            LabBatch batch = possibleBatches.iterator().next();
            batch.getJiraTicket().addComment(labEvent.getEventOperator().getLogin() + "is processing " + labEvent.getEventName().name() + " at " + labEvent.getEventLocation());
            for (LabVessel vessel : vesselsForBatch) {
                ProjectPlan projectPlanOverride = batch.getProjectPlanOverride(vessel);
                if (projectPlanOverride != null) {
                    //labEvent.setProjectPlanOverride(vessel,projectPlanOverride);
                }
            }
        }
        // else no batches
}
}
