package org.broadinstitute.sequel;

/**
 * When you add something to a {@link FullAccessLabWorkQueue },
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
