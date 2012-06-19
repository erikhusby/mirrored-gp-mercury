package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.HashSet;
import java.util.Set;

/**
 * The batch of work, as tracked by a person
 * in the lab.  A batch is basically an lc set.
 */
public class LabBatch {

    private Set<Starter> starters = new HashSet<Starter>();

    private boolean isActive = true;

    private String batchName;

    private JiraTicket jiraTicket;

    public LabBatch(String batchId,
                    Set<Starter> starters) {
        if (batchId == null) {
            throw new NullPointerException("BatchId cannot be null");
        }
        if (starters == null) {
            throw new NullPointerException("starters cannot be null");
        }
        this.batchName = batchId;
        for (Starter starter : starters) {
            addStarter(starter);
        }
    }

    public void addStarter(Starter starter) {
        starters.add(starter);
    }

    public boolean getActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    public String getBatchName() {
        return batchName;
    }

    public Set<Starter> getStarters() {
        return starters;
    }

    public JiraTicket getJiraTicket() {
        return jiraTicket;
    }

    public void setProjectPlanOverride(LabVessel vessel,ProjectPlan planOverride) {
        throw new RuntimeException("I haven't been written yet.");
    }

    public ProjectPlan getProjectPlanOverride(LabVessel labVessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

}
