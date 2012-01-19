package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

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
