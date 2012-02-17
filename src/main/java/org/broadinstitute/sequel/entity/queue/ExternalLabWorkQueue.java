package org.broadinstitute.sequel.entity.queue;

/**
 * a {@link LabWorkQueue} to which we can submit
 * a batch of {@link org.broadinstitute.sequel.entity.vessel.LabTangible}, but whose
 * fulfilling lab is out of our control.
 *
 * PMs will make a batch of samples to send
 * over to BSP.  When they're happy with the
 * contents of the batch, we send them
 * over to BSP as a single transaction
 * by calling {@link #sendBatch()}
 */
public interface ExternalLabWorkQueue<T extends LabWorkQueueResponse> {

    /**
     * Send the batch to the receiving
     * service.  This is a kind of
     * commit signal to the external lab
     */
    public T sendBatch();
}
