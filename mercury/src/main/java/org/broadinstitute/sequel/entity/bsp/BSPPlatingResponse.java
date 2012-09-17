package org.broadinstitute.sequel.entity.bsp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.queue.LabWorkQueueResponse;

import javax.inject.Inject;

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
