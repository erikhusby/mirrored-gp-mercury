package org.broadinstitute.gpinformatics.infrastructure.squid;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;

import javax.annotation.Nonnull;
import javax.enterprise.inject.Alternative;
import javax.ws.rs.core.Response;

/**
 * Stub for Squid Connector
 */
@Stub
@Alternative
public class SquidConnectorStub implements SquidConnector{

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {
        return new SquidConnector.SquidResponse(Response.Status.CREATED.getStatusCode(), "");
    }

    @Override
    public SquidResponse saveReadStructure(@Nonnull ReadStructureRequest readStructureData, @Nonnull String squidWSUrl)
            throws UniformInterfaceException {
        return new SquidConnector.SquidResponse(Response.Status.CREATED.getStatusCode(), "");
    }
}
