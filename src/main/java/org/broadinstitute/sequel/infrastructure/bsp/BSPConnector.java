package org.broadinstitute.sequel.infrastructure.bsp;

import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingResponse;

import java.util.Collection;

/**
 * Wrapper around BSP's client API
 */
public interface BSPConnector {
    
    public BSPPlatingResponse sendAliquotRequests(Collection<BSPPlatingRequest> aliquotRequests);

}
