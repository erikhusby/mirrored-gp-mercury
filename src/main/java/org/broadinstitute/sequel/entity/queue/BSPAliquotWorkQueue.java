package org.broadinstitute.sequel.entity.queue;

import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularStateRange;
import org.broadinstitute.sequel.control.bsp.BSPConnector;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingResponse;

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
 * a time.  So {@link LabWorkQueue#add(org.broadinstitute.sequel.entity.vessel.LabVessel, T) add} and
 * {@link LabWorkQueue#remove(org.broadinstitute.sequel.entity.vessel.LabVessel, T) remove} are
 * used to build a list which is then sent to
 * BSP when we call some method.
 */
public class BSPAliquotWorkQueue implements LabWorkQueue<AliquotParameters>,ExternalLabWorkQueue<BSPPlatingResponse> {

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
     * add stuff.  So when you {@link LabWorkQueue#add(org.broadinstitute.sequel.entity.vessel.LabVessel, T) add stuff},
     * you're only adding it to the session.  
     * 
     * In other words, an instance of this class is
     * specific to one user session, while in instance
     * of other {@link FullAccessLabWorkQueue}s are
     * shared across instances.
     * 
     * Put another way, the PMs are building a set
     * of {@link Goop}s that will be plated together
     * from BSP, whereas for {@link FullAccessLabWorkQueue}s,
     * they're dribbling {@link org.broadinstitute.sequel.entity.vessel.LabTangible (tubes/plates)}
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
    public LabWorkQueueResponse add(LabVessel vessel, AliquotParameters aliquotParameters) {
        if (vessel == null) {
             throw new IllegalArgumentException("labTangible must be non-null in BSPAliquotWorkQueue.add");
        }
        if (aliquotParameters == null) {
             throw new IllegalArgumentException("bucket must be non-null in BSPAliquotWorkQueue.add");
        }
        aliquotRequests.add(new BSPPlatingRequest(vessel.getLabCentricName(),aliquotParameters));

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
    public LabWorkQueueResponse remove(LabVessel vessel, AliquotParameters bucket) {
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

}