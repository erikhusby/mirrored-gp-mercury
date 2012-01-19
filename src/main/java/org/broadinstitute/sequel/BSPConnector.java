package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * Wrapper around BSP's client API
 */
public interface BSPConnector {
    
    public BSPPlatingResponse sendAliquotRequests(Collection<BSPPlatingRequest> aliquotRequests);

}
