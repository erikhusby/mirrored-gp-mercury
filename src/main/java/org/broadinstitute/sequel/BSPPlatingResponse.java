package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class BSPPlatingResponse implements LabWorkQueueResponse {

    private static Log gLog = LogFactory.getLog(BSPPlatingResponse.class);

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
