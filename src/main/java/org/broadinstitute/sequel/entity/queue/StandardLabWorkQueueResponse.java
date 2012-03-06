package org.broadinstitute.sequel.entity.queue;

public class StandardLabWorkQueueResponse implements LabWorkQueueResponse {

    private final String message;
    
    public StandardLabWorkQueueResponse(String message) {
        if (message == null) {
             throw new NullPointerException("message cannot be null.");
        }
        this.message = message;
    }
    
    @Override
    public String getText() {
        return message;
    }
}
