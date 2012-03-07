package org.broadinstitute.sequel.control.workflow;

import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueParameters;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.HashSet;

public class WorkflowEngine {
    
    private Collection<Workflow> workflows = new HashSet<Workflow>();
    
    public void addWorkflow(Workflow workflow) {
        if (workflow == null) {
             throw new NullPointerException("workflow cannot be null."); 
        }
        workflows.add(workflow);
    }

    public Collection<Workflow> getActiveWorkflows(LabVessel startingVessel) {
        final Collection<Workflow> workflowsForVessel = new HashSet<Workflow>();
        for (Workflow workflow : workflows) {
            if ((workflow.getAllVessels().contains(startingVessel))) {
                workflowsForVessel.add(workflow);
            }
        }
        return workflows;
    }
}
