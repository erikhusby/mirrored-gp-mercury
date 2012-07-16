package org.broadinstitute.sequel.infrastructure.bsp;

import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingResponse;
import org.broadinstitute.sequel.infrastructure.deployment.Stub;

import javax.inject.Inject;
import java.util.Collection;

@Stub // used in fast unit tests, non-integration.
public class BSPConnectorStub implements BSPConnector {

    @Inject
    private Log log;


    @Override
    public BSPPlatingResponse sendAliquotRequests(Collection<BSPPlatingRequest> aliquotRequests) {
        if (log != null) {
            log.info("Mock request for " + aliquotRequests.size() + " aliquots.");
        }
        return new BSPPlatingResponse("Mock response", new BSPPlatingReceipt("MockReceipt" + System.currentTimeMillis()));
    }
}
