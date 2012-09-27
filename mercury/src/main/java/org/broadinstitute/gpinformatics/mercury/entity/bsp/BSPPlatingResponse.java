package org.broadinstitute.gpinformatics.mercury.entity.bsp;

import org.broadinstitute.gpinformatics.mercury.entity.queue.LabWorkQueueResponse;

public class BSPPlatingResponse implements LabWorkQueueResponse {

    //@Inject
    //private Log log;

    private boolean wasRejected;
    
    private BSPPlatingReceipt receipt;
    
    private final String text;
    
    public BSPPlatingResponse(String text,BSPPlatingReceipt receipt) {
        if (text == null) {
             throw new IllegalArgumentException("text must be non-null in BSPPlatingResponse.BSPPlatingResponse");
        }
        this.text = text;
        this.wasRejected = false;
        this.receipt = receipt;
    }
    
    public void setReceipt(BSPPlatingReceipt receipt) {
        this.receipt = receipt;
    }
    
    
    public BSPPlatingReceipt getReceipt() {
        return receipt;
    }

    public boolean wasRejected() {
        return wasRejected;
    }

    @Override
    public String getText() {
        return text;
    }
}
