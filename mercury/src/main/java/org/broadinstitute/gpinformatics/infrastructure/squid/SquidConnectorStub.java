package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;

import javax.enterprise.inject.Alternative;

/**
 * Stub for Squid Connector
 */
@Stub
@Alternative
public class SquidConnectorStub implements SquidConnector{

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {
        return new SquidConnector.SquidResponse(500, "");
    }

}
