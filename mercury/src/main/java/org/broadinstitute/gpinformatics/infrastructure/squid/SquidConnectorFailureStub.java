package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;

import javax.enterprise.inject.Alternative;
import javax.ws.rs.core.Response;

/**
 * Stub for Squid Connector
 */
@Stub
@Alternative
public class SquidConnectorFailureStub implements SquidConnector{

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {
        return new SquidResponse(Response.Status.BAD_REQUEST.getStatusCode(), "");
    }

}
