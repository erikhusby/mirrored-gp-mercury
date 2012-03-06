package org.broadinstitute.sequel.entity.queue;

import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.project.SequencingPlanDetail;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;
import org.broadinstitute.sequel.entity.workflow.WorkflowDescription;

import java.util.Collection;

public interface LabWorkQueue<T extends LabWorkQueueParameters> {

    public LabWorkQueueName getQueueName();

    /**
     * Adds a sample to this queue.  Adding a sample
     * doesn't necessarily mean it will be immediately
     * visible to lab staff.
     *
     *
     * @param vessel
     * @param workflowParameters the parameters, also considered
     *                   the "bucket" for the queue.
     * @return a response object, which may embody error information
     * like "Hey, I can't put this into the queue because the project
     * linked to this sample doesn't have the read length set".
     */
    public LabWorkQueueResponse add(LabVessel vessel,
                                    T workflowParameters,
                                    SequencingPlanDetail sequencingPlan);

    /**
     * What's the MolecularStateRange required for
     * entry into this queue?
     * @return
     */
    public Collection<MolecularStateRange> getMolecularStateRequirements();

    /**
     * Remove the tangible from the given bucket.
     *
     * @param vessel
     * @param workflowParameters
     * @return
     */
    public LabWorkQueueResponse startWork(LabVessel vessel,
                                          T workflowParameters,
                                          WorkflowDescription workflow,
                                          Person user);
}
