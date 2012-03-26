package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueParameters;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;

public class Workflow {

    private ProjectPlan projectPlan;
    
    private Collection<LabVessel> vessels;
    
    private LabWorkQueueParameters labQueueParameters;

    private WorkflowState state;
    
    public Workflow(ProjectPlan projectPlan,
                    Collection<LabVessel> labVessels,
                    LabWorkQueueParameters labQueueParameters) {
        if (projectPlan == null) {
             throw new NullPointerException("projectPlan cannot be null."); 
        }
        if (labVessels == null) {
             throw new NullPointerException("labVessels cannot be null."); 
        }
        this.projectPlan = projectPlan;
        this.vessels = labVessels;
        this.labQueueParameters = labQueueParameters;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public WorkflowState getState() {
        return state;
    }

    public ProjectPlan getProjectPlan() {
        return projectPlan;
    }

    public Collection<LabVessel> getAllVessels() {
        return vessels;
    }

    public boolean isBillable(LabEventName eventName) {
        throw new RuntimeException("not implemented");
    }

}
