package org.broadinstitute.gpinformatics.mercury.entity.bsp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Collection;
import java.util.HashSet;

public class BSPPlatingReceipt {

    private static Log gLog = LogFactory.getLog(BSPPlatingReceipt.class);

    private final String receiptId;
    
    private Collection<BSPPlatingRequest> requests = new HashSet<>();
    
    public BSPPlatingReceipt(String receiptId) {
        this.receiptId = receiptId;        
    }
    
    public Collection<BSPPlatingRequest> getPlatingRequests() {
        return requests;
    }

    public void addPlatingRequest(BSPPlatingRequest platingRequest) {
        requests.add(platingRequest);
    }
    
}
