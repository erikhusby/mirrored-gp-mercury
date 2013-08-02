package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;

/**
 * Stub for Bettalims Connector
 */
@Stub
@Alternative
public class BettaLimsConnectorStub implements BettaLimsConnector {

    @Override
    public BettalimsResponse sendMessage(String message) {
        return new BettalimsResponse(500, "");
    }
}
