package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.Set;

/**
 * The batch of work, as tracked by a person
 * in the lab.  A batch is basically an lc set.
 */
public interface LabBatch {
    
    public boolean getActive();
    
    public void setActive(boolean isActive);
    
    public String getBatchId();

    public Set<LabVessel> getStartingVessels();

    public WorkflowDescription getWorkflowForVessel(LabVessel vessel);

    public void setProjectPlanOverride(LabVessel vessel,ProjectPlan planOverride);

    public ProjectPlan getProjectPlanOverride(LabVessel labVessel);

    public JiraTicket getJiraTicket();

}
