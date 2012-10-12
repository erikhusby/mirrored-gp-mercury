package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
//import org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan;
import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularStateRange;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Collection;

@Entity
@Audited
@Table(schema = "mercury")
public abstract class LabWorkQueue<T extends LabWorkQueueParameters> {

    @Id
    @SequenceGenerator(name = "SEQ_LAB_WORK_QUEUE", schema = "mercury", sequenceName = "SEQ_LAB_WORK_QUEUE")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LAB_WORK_QUEUE")
    private Long labWorkQueueId;

    public abstract LabWorkQueueName getQueueName();

    /**
     * Adds a sample to this queue.  Adding a sample
     * doesn't necessarily mean it will be immediately
     * visible to lab staff.
     *
     *
     * @param vessel The vessel, complete with a functioning
     *               {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel#getSampleInstances()}
     *               that detail the {@link org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan}
     *               relationships via {@link org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance#getAllProjectPlans()}
     *               and {@link org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance#getSingleProjectPlan()}
     * @param workflowParameters the parameters, also considered
     *                   the "bucket" for the queue.
     * @return a response object, which may embody error information
     * like "Hey, I can't put this into the queue because the project
     * linked to this sample doesn't have the read length set".
     */
    public abstract LabWorkQueueResponse add(LabVessel vessel,
                                    T workflowParameters,
                                    WorkflowDescription workflowDescription/*,
                                    BasicProjectPlan projectPlanOverride*/);

    /**
     * What's the MolecularStateRange required for
     * entry into this queue?
     * @return
     */
    public abstract Collection<MolecularStateRange> getMolecularStateRequirements();

    /**
     * Remove the tangible from the given bucket.
     *
     * @param vessels
     * @param workflowParameters
     * @return
     */
    public abstract LabWorkQueueResponse startWork(Collection<LabVessel> vessels,
                                          T workflowParameters,
                                          WorkflowDescription workflow,
                                          Person user);

    public abstract boolean isEmpty();

    public abstract void remove(WorkQueueEntry workQueueEntry);

    /**
     * Returns any entires this queue has for the given
     * workflow and vessel.
     * @param workflow
     * @param vessel
     * @return
     */
    public abstract Collection<WorkQueueEntry<T>> getEntriesForWorkflow(WorkflowDescription workflow,
                                                            LabVessel vessel);

}
