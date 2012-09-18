package org.broadinstitute.gpinformatics.mercury.infrastructure.bsp;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingResponse;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Stub;

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
