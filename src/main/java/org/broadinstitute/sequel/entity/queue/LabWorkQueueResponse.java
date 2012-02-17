package org.broadinstitute.sequel.entity.queue;

/**
 * When you add something to a {@link org.broadinstitute.sequel.entity.queue.FullAccessLabWorkQueue },
 * you get a response from the queue, like "ok",
 * or "your item is now third in line".
 */
public interface LabWorkQueueResponse {

    /**
     * Human readable response
     * @return
     */
    public String getText();
    
}
