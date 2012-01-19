package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

public class BSPPlatingRequest {

    private static Log gLog = LogFactory.getLog(BSPPlatingRequest.class);

    private boolean isFulfilled;
    
    private final String sampleName;

    private BSPPlatingReceipt receipt;

    private final AliquotParameters aliquotParameters;

    public BSPPlatingRequest(String sampleName,
                             AliquotParameters aliquotParameters) {
        if (sampleName == null) {
             throw new IllegalArgumentException("sampleName must be non-null in BSPPlatingRequest.BSPPlatingRequest");
        }
        if (aliquotParameters == null) {
             throw new IllegalArgumentException("aliquotParameters must be non-null in BSPPlatingRequest.BSPPlatingRequest");
        }

        this.sampleName = sampleName;
        isFulfilled = false;
        this.aliquotParameters = aliquotParameters;
        aliquotParameters.getProject().addPlatingRequest(this);
    }


    public void setReceipt(BSPPlatingReceipt receipt) {
        this.receipt = receipt;
        receipt.addPlatingRequest(this);
    }
    
    public String getSampleName() {
        return sampleName;
    }
    
    public BSPPlatingReceipt getReceipt() {
        return receipt;
    }

    public boolean isFulfilled() {
        return isFulfilled;    
    }

    public void setFulfilled(boolean isFulfilled) {
        this.isFulfilled = isFulfilled;
    }

    public AliquotParameters getAliquotParameters() {
        return aliquotParameters;
    }

}
