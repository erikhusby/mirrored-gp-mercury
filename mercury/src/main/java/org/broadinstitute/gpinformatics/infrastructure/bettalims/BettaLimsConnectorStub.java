package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;

/**
 * Stub for BettaLims Connector
 */
@Stub
@Alternative
public class BettaLimsConnectorStub implements BettaLimsConnector {

    @Override
    public BettaLimsResponse sendMessage(String message) {
        return new BettaLimsResponse(500, "");
    }
}
