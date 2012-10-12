package org.broadinstitute.gpinformatics.mercury.entity.workflow;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventName;
//import org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan;
import org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueueParameters;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Collection;

public class Workflow {

//    private BasicProjectPlan projectPlan;
    
    private Collection<LabVessel> vessels;
    
    private LabWorkQueueParameters labQueueParameters;

    private WorkflowState state;
    
    public Workflow(//BasicProjectPlan projectPlan,
                    Collection<LabVessel> labVessels,
                    LabWorkQueueParameters labQueueParameters) {
//        if (projectPlan == null) {
//             throw new NullPointerException("projectPlan cannot be null.");
//        }
        if (labVessels == null) {
             throw new NullPointerException("labVessels cannot be null."); 
        }
//        this.projectPlan = projectPlan;
        this.vessels = labVessels;
        this.labQueueParameters = labQueueParameters;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public WorkflowState getState() {
        return state;
    }

//    public BasicProjectPlan getProjectPlan() {
//        return projectPlan;
//    }

    public Collection<LabVessel> getAllVessels() {
        return vessels;
    }

    public boolean isBillable(LabEventName eventName) {
        throw new RuntimeException("not implemented");
    }

}
