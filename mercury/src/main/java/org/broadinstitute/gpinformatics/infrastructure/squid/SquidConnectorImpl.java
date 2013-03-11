package org.broadinstitute.gpinformatics.infrastructure.squid;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

/**
 * @author Scott Matthews
 *         Date: 3/11/13
 *         Time: 1:46 PM
 */
@Impl
public class SquidConnectorImpl implements SquidConnector {

    SquidConfig squidConfig;

    @Inject
    public SquidConnectorImpl(SquidConfig squidConfig) {
        this.squidConfig = squidConfig;
    }

    @Override
    public SquidResponse createRun(SolexaRunBean runInformation) {

        ClientResponse response = Client.create().resource(squidConfig.getUrl() + "/rest/solexarun")
                                .type(MediaType.APPLICATION_XML_TYPE)
                                .accept(MediaType.APPLICATION_XML)
                                .entity(runInformation).post(ClientResponse.class);

        return new SquidResponse(response.getStatus(), response.getEntity(String.class));

    }
}
