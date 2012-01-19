package org.broadinstitute.sequel;

import java.util.Collection;

public interface LabWorkQueue<T extends LabWorkQueueParameters> {

    public LabWorkQueueName getQueueName();

    /**
     * Adds a sample to this queue.  Adding a sample
     * doesn't necessarily mean it will be immediately
     * visible to lab staff.
     *
     * @param labTangible the thing to enqueue
     * @param bucket the parameters, also considered
     *                   the "bucket" for the queue.
     * @return a response object, which may embody error information
     * like "Hey, I can't put this into the queue because the project
     * linked to this sample doesn't have the read length set".
     */
    public LabWorkQueueResponse add(LabTangible labTangible,
                                    T bucket);

    /**
     * What's the MolecularStateRange required for
     * entry into this queue?
     * @return
     */
    public Collection<MolecularStateRange> getMolecularStateRequirements();

    /**
     * Remove the tangible from the given bucket.
     * @param labTangible
     * @param bucket
     * @return
     */
    public LabWorkQueueResponse remove(LabTangible labTangible,T bucket);
}
