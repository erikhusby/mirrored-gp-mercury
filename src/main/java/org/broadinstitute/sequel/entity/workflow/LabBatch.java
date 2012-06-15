package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Map;

/**
 * The batch of work, as tracked by a person
 * in the lab.  A batch is basically an lc set.
 */
public interface LabBatch {
    
    public boolean getActive();
    
    public void setActive(boolean isActive);
    
    public String getBatchId();

    /**
     * Key is the actual {@link Starter}, and value
     * is the {@link LabVessel} that is the concrete
     * vessel used.  We need this flexibility so we can
     * start jira tickets with either samples, kits, or
     * {@link LabVessel}s
     * @return
     */
    public Map<Starter,LabVessel> getStarters();

    public WorkflowDescription getWorkflowForVessel(LabVessel vessel);

    public void setProjectPlanOverride(LabVessel vessel,BasicProjectPlan planOverride);

    public BasicProjectPlan getProjectPlanOverride(LabVessel labVessel);

    public JiraTicket getJiraTicket();

}
