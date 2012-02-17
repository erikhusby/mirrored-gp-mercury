package org.broadinstitute.sequel.control.bsp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingResponse;

import javax.enterprise.inject.Default;
import java.util.Collection;

@Default // used in fast unit tests, non-integration.
public class MockBSPConnector implements BSPConnector {

    private static Log gLog = LogFactory.getLog(MockBSPConnector.class);


    @Override
    public BSPPlatingResponse sendAliquotRequests(Collection<BSPPlatingRequest> aliquotRequests) {
        gLog.info("Mock request for " + aliquotRequests.size() + " aliquots.");
        return new BSPPlatingResponse("Mock response", new BSPPlatingReceipt("MockReceipt" + System.currentTimeMillis()));
    }
}
