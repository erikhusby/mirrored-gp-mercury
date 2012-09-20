package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.mercury.entity.project.BasicProjectPlan;
import org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularStateRange;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConnector;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingResponse;

import java.util.Collection;
import java.util.HashSet;

/**
 * Coding this up makes me think that
 * we should make a ReadableLabWorkQueue
 * and WriteableLabWorkQueue.  BSP is only
 * readable for us, and the current
 * interfaces lead us to have lots
 * of empty menthods here that do not
 * make sense.
 * 
 * Unlike other queues, which we treat as
 * static singletons, the the BSP queue
 * is used to build a single plate at
 * a time.  So {@link LabWorkQueue#add(org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel, LabWorkQueueParameters, org.broadinstitute.gpinformatics.mercury.entity.project.SequencingPlanDetail)} and
 * {@link LabWorkQueue#startWork(org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel, LabWorkQueueParameters, org.broadinstitute.gpinformatics.mercury.entity.project.WorkflowDescription, org.broadinstitute.gpinformatics.mercury.entity.person.Person)} are
 * used to build a list which is then sent to
 * BSP when we call some method.
 */
public class BSPAliquotWorkQueue extends LabWorkQueue<AliquotParameters> implements ExternalLabWorkQueue<BSPPlatingResponse> {

    // Can we first make db-free unit tests and then play some
    // CDI configuration to turn them into various flavors
    // of integration tests for free?  Just change the beans.crap.xml
    // file you're using to run the same tests with or without
    // BSP connectivity.  Could you do the same with db access?
    private BSPConnector bspConnector;
    
    private final Collection<BSPPlatingRequest> aliquotRequests = new HashSet<BSPPlatingRequest>();
    
    /**
     * When you new up one of these, the list
     * of samples is session-specific.  {@link LabWorkQueue}s
     * that are specific to GSP are used by PMs (to add
     * stuff) and by GSP lab staff (to process samples),
     * but the BSP queue is only used in GSP to
     * add stuff.  So when you {@link LabWorkQueue#add(org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel, LabWorkQueueParameters, org.broadinstitute.gpinformatics.mercury.entity.project.SequencingPlanDetail)} ) add stuff},
     * you're only adding it to the session.  
     * 
     * In other words, an instance of this class is
     * specific to one user session, while in instance
     * of other {@link FullAccessLabWorkQueue}s are
     * shared across instances.
     * 
     * Put another way, the PMs are building a set
     * of {@link org.broadinstitute.gpinformatics.mercury.entity.sample.StartingSample}s that will be plated together
     * from BSP, whereas for {@link FullAccessLabWorkQueue}s,
     * they're dribbling {@link org.broadinstitute.gpinformatics.mercury.entity.vessel.LabTangible (tubes/plates)}
     * one at a time, and letting the lab pull in whatever
     * batch is "best".
     */
    public BSPAliquotWorkQueue(BSPConnector bspConnector) {
        this.bspConnector = bspConnector;
    }
    
    @Override
    public LabWorkQueueName getQueueName() {
        return LabWorkQueueName.BSP_ALIQUOT;
    }


    @Override
    public LabWorkQueueResponse startWork(Collection<LabVessel> vessels, AliquotParameters workflowParameters, WorkflowDescription workflow, Person user) {
        throw new RuntimeException("I haven't been written yet.");
    }


    @Override
    public LabWorkQueueResponse add(LabVessel vessel, AliquotParameters workflowParameters, WorkflowDescription workflowDescription, BasicProjectPlan projectPlanOverride) {
        if (vessel == null) {
            throw new IllegalArgumentException("labTangible must be non-null in BSPAliquotWorkQueue.add");
        }
        if (workflowParameters == null) {
            throw new IllegalArgumentException("bucket must be non-null in BSPAliquotWorkQueue.add");
        }
        aliquotRequests.add(new BSPPlatingRequest(vessel.getLabCentricName(),workflowParameters));

        // todo some service call to BSP to say "does this look right?"
        // and then respond accordingly

        return new LabWorkQueueResponse() {
            @Override
            public String getText() {
                return "Okay";
            }
        };
    }


    @Override
    public Collection<MolecularStateRange> getMolecularStateRequirements() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public BSPPlatingResponse sendBatch() {
        BSPPlatingResponse platingResponse =  bspConnector.sendAliquotRequests(aliquotRequests);

        if (!platingResponse.wasRejected()) {
            for (BSPPlatingRequest platingRequest : aliquotRequests) {
                // ideally we could get per-sample receipts from BSP, instead
                // of a receipt per batch.  we might overload BSP's "project"
                // string and shove a concatention of all our request
                // ids in there so that when the aliquots are received,
                // we know how to resolve them to aliquot requests.
                platingRequest.setReceipt(platingResponse.getReceipt());
            }
        }
        else {
            //else do what I'm not sure
        }
        return platingResponse;
    }

    @Override
    public boolean isEmpty() {
        return aliquotRequests.isEmpty();
    }

    @Override
    public void remove(WorkQueueEntry workQueueEntry) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<WorkQueueEntry<AliquotParameters>> getEntriesForWorkflow(WorkflowDescription workflow, LabVessel vessel) {
        throw new RuntimeException("I haven't been written yet.");
    }
}