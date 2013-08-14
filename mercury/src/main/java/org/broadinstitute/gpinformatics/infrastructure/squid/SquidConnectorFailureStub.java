package org.broadinstitute.gpinformatics.infrastructure.squid;

import com.sun.jersey.api.client.UniformInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
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
public class SquidConnectorFailureStub implements SquidConnector{

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {
        return new SquidResponse(Response.Status.BAD_REQUEST.getStatusCode(), "");
    }

    @Override
    public void saveReadStructure(@Nonnull ReadStructureRequest readStructureData, @Nonnull String squidWSUrl)
            throws UniformInterfaceException {
        throw new ResourceException("Failed to call Squid", Response.Status.BAD_REQUEST);
    }
}
