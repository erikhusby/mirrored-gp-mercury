package org.broadinstitute.sequel.entity.queue;

import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;
import org.broadinstitute.sequel.entity.workflow.WorkflowEngine;

import java.util.Collection;
import java.util.Set;

public interface LabWorkQueue<T extends LabWorkQueueParameters> {

    public LabWorkQueueName getQueueName();

    /**
     * Adds a sample to this queue.  Adding a sample
     * doesn't necessarily mean it will be immediately
     * visible to lab staff.
     *
     *
     * @param vessel The vessel, complete with a functioning
     *               {@link org.broadinstitute.sequel.entity.vessel.LabVessel#getSampleInstances()}
     *               that detail the {@link org.broadinstitute.sequel.entity.project.ProjectPlan}
     *               relationships via {@link org.broadinstitute.sequel.entity.sample.SampleInstance#getAllProjectPlans()}
     *               and {@link org.broadinstitute.sequel.entity.sample.SampleInstance#getSingleProjectPlan()}
     * @param workflowParameters the parameters, also considered
     *                   the "bucket" for the queue.
     * @return a response object, which may embody error information
     * like "Hey, I can't put this into the queue because the project
     * linked to this sample doesn't have the read length set".
     */
    public LabWorkQueueResponse add(LabVessel vessel,
                                    T workflowParameters,
                                    WorkflowDescription workflowDescription,
                                    ProjectPlan projectPlanOverride);

    /**
     * What's the MolecularStateRange required for
     * entry into this queue?
     * @return
     */
    public Collection<MolecularStateRange> getMolecularStateRequirements();

    /**
     * Remove the tangible from the given bucket.
     *
     * @param vessels
     * @param workflowParameters
     * @return
     */
    public LabWorkQueueResponse startWork(Collection<LabVessel> vessels,
                                          T workflowParameters,
                                          WorkflowDescription workflow,
                                          Person user);

    public boolean isEmpty();

    public void remove(WorkQueueEntry workQueueEntry);

    public Collection<WorkQueueEntry<T>> getEntriesForWorkflow(WorkflowDescription workflow,
                                                            LabVessel vessel);

}
