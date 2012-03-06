package org.broadinstitute.sequel.entity.queue;


import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.broadinstitute.sequel.entity.vessel.LabTangible;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheetAlertUtil;
import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;

import java.util.*;

/**
 * Add things in and they come out in fifo
 * order.  Probably most queues in the lab
 * will work this way initially.
 */
public abstract class AbstractFIFOLabWorkQueue<T extends LabWorkQueueParameters> implements FullAccessLabWorkQueue<T> {

    private Collection<WorkQueueEntry> entries = new HashSet<WorkQueueEntry>();
    
    @Override
    public LabWorkQueueResponse startWork(LabVessel vessel, T workflowParameters, WorkflowDescription workflow) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public LabWorkQueueResponse add(LabVessel vessel, T workflowParameters, ProjectPlan projectPlan) {
        if (vessel == null) {
             throw new NullPointerException("vessel cannot be null.");
        }
        if (projectPlan == null) {
             throw new NullPointerException("projectPlan cannot be null.");
        }
        boolean isNew = entries.add(new WorkQueueEntry(vessel,workflowParameters,projectPlan));


    }


    @Override
    public void moveToTop(LabVessel vessel,T bucket) {

    }


}
