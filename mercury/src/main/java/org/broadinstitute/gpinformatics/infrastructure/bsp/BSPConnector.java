package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingResponse;

import java.util.Collection;

/**
 * Wrapper around BSP's client API
 */
public interface BSPConnector {
    
    public BSPPlatingResponse sendAliquotRequests(Collection<BSPPlatingRequest> aliquotRequests);

}
