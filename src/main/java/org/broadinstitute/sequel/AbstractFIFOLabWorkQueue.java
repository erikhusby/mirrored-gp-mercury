package org.broadinstitute.sequel;


import java.util.*;

/**
 * Add things in and they come out in fifo
 * order.  Probably most queues in the lab
 * will work this way initially.
 */
public abstract class AbstractFIFOLabWorkQueue<T extends LabWorkQueueParameters> implements FullAccessLabWorkQueue<T> {

    // list choice is important here--fifo.
    private Map<T,LinkedList<LabTangible>> items = new HashMap<T, LinkedList<LabTangible>>();

    @Override
    public Collection<LabTangible> suggestNextBatch(int batchSize, T bucket) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public LabWorkQueueResponse remove(final LabTangible labTangible,T bucket) {
        final Collection<T> containingBuckets = getContainingBuckets(labTangible);
        if (containingBuckets.isEmpty()) {
            throw new RuntimeException("Cannot find " + labTangible.getLabCentricName() + " in " + getQueueName());
        }
        else {
            for (T bucketKey: containingBuckets) {
                if (!items.get(bucketKey).remove(labTangible)) {
                    throw new RuntimeException("Failed to remove " + labTangible.getLabCentricName() + " from " + getQueueName());
                }
            }
        }
        return new LabWorkQueueResponse() {
            @Override
            public String getText() {
                return "Removed " + labTangible.getLabCentricName() + " from " + containingBuckets.size() + " buckets in " + getQueueName();
            }
        };
    }

    /**
     * Returns keys for the buckets in which this
     * labTangible resides.
     * @param tangible
     * @return
     */
    public Collection<T> getContainingBuckets(LabTangible tangible) {
        Set<T> bucketsThatContainIt = new HashSet<T>();
        for (Map.Entry<T,LinkedList<LabTangible>> entry: items.entrySet()) {
            if (entry.getValue().contains(tangible)) {
                bucketsThatContainIt.add(entry.getKey());
            }
        }
        return bucketsThatContainIt;
    }

    @Override
    public void moveToTop(LabTangible labTangible,T bucket) {

    }

    @Override
    public void startWork(LabTangible labTangible, T bucket) {
        // I'm commenting this part out because I think the lab is going
        // to work FIFO internally.  How does the tech know what
        // they should work on?  Go look in the freezer.  Whatever's there
        // is what you take.  In this model, nobody is going to bother
        // to add something to a queue.  they're just going to grab
        // what's in the fridge and run.

        // There will be exceptions for queues that the PMs manage.
        // those subclasses may barf if someone attempts to start
        // work on something that isn't in the queue.

        /*
        if (!peek(bucket).contains(labTangible)) {
            throw new RuntimeException("Can't start work on " + labTangible.getLabCentricName() + " in the " + bucket.toText() + " bucket in " + getQueueName());
        }
        */
        SampleSheetAlertUtil.doAlert("Starting " + getQueueName() + " work for these samples.",labTangible.getSampleSheets(),false);
        remove(labTangible,bucket);
    }



    @Override
    public LabWorkQueueResponse add(LabTangible labTangible, T parameters) {

        for (StateChange stateChange : labTangible.getStateChanges()) {

        }

        for (SampleInstance sampleInstance: labTangible.getSampleInstances()) {
            for (MolecularStateRange molStateRange: getMolecularStateRequirements()) {
                if (!molStateRange.isInRange(sampleInstance.getMolecularState())) {
                    throw new RuntimeException("Can't add " + labTangible.getLabCentricName() + " into " + getQueueName() + " because sample " + sampleInstance.getStartingSample().getSampleName() + " is in an invalid molecular state.");
                }
            }
            // check that the molecular state of the sample
            // matches what this queue can handle.
        }


        // todo let's assume that each of these lab queues
        // will result in a labops jira ticket.  so we'll
        // create one on-the-fly, and link it back to the
        // PM project jira

        throw new RuntimeException("Method not yet implemented.");
    }

    @Override
    public Collection<LabTangible> peek(T parameters) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabTangible> peekAll() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void markComplete(LabTangible labTangible,T bucket) {
        SampleSheetAlertUtil.doAlert("Completed " + getQueueName() + " work for these samples.",labTangible.getSampleSheets(),false);
    }

    @Override
    public void printWorkSheet(Collection<LabTangible> labTangibles, T bucket) {
        throw new RuntimeException("I haven't been written yet.");
    }
}
