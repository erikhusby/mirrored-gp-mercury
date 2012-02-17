package org.broadinstitute.sequel.control.bsp;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingResponse;

import javax.enterprise.inject.Alternative;
import java.util.Collection;

@Alternative // used to connect to the live dev BSP server
public class DevBSPConnector implements BSPConnector {

    private static Log gLog = LogFactory.getLog(DevBSPConnector.class);

    @Override
    public BSPPlatingResponse sendAliquotRequests(Collection<BSPPlatingRequest> aliquotRequests) {
        throw new RuntimeException("dev aliquotter.");
    }
}
