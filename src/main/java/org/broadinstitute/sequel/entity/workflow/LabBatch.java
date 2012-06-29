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

    public static final String LCSET_PROJECT_PREFIX = "LCSET";

    private Set<Starter> starters = new HashSet<Starter>();

    private boolean isActive = true;

    private String batchName;

    private JiraTicket jiraTicket;

    /**
     * Create a new batch with the given name
     * and set of {@link Starter starting materials}
     * @param batchId
     * @param starters
     */
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
        starter.addLabBatch(this);
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

    public void setJiraTicket(JiraTicket jiraTicket) {
        this.jiraTicket = jiraTicket;
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
