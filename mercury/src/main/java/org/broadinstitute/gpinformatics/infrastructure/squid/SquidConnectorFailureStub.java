package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;

import javax.annotation.Nonnull;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

/**
 * Stub for Squid Connector testing (non-CDI managed)
 */
public class SquidConnectorFailureStub implements SquidConnector {

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {
        return new SquidResponse(Response.Status.BAD_REQUEST.getStatusCode(), "");
    }

    @Override
    public SquidResponse saveReadStructure(@Nonnull ReadStructureRequest readStructureData, @Nonnull String squidWSUrl)
            throws WebApplicationException {
        return new SquidResponse(Response.Status.BAD_REQUEST.getStatusCode(), "");
    }

}
