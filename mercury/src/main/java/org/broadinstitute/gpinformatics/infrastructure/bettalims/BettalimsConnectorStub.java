package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

/**
 * Stub for Bettalims Connector
 */
@Stub
public class BettalimsConnectorStub  implements BettalimsConnector{

    @Override
    public BettalimsResponse sendMessage(String message) {
        return new BettalimsResponse(500, "");
    }
}
