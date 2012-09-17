package org.broadinstitute.sequel.entity.workflow;

import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
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

    public Collection<Workflow> getActiveWorkflows(LabVessel labVessel,
                                                   WorkflowDescription workflowDescription) {
        if (labVessel == null) {
             throw new NullPointerException("labVessel cannot be null."); 
        }
        final Collection<Workflow> workflowsForVessel = new HashSet<Workflow>();
        for (Workflow workflow : workflows) {
            if ((workflow.getAllVessels().contains(labVessel))) {
                if (workflowDescription != null) {
                    if (workflowDescription.equals(workflow.getProjectPlan().getWorkflowDescription())) {
                        workflowsForVessel.add(workflow);
                    }
                }
                else {
                    workflowsForVessel.add(workflow);
                }
            }
        }
        return workflowsForVessel;
    }
    
    public Collection<BasicProjectPlan> getActiveProjectPlans(SampleInstance sampleInstance) {
        throw new RuntimeException("not implemented");
    }
}
