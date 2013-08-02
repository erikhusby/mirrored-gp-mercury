package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

/**
 * Implementation of connector to BettaLIMS
 */
@Impl
public class BettalimsConnectorImpl implements BettalimsConnector {

    @Inject
    private BettaLimsConfig bettaLimsConfig;

    /**
     * for CDI
     */
    public BettalimsConnectorImpl() {
    }

    public BettalimsConnectorImpl(BettaLimsConfig bettaLimsConfig) {
        this.bettaLimsConfig = bettaLimsConfig;
    }

    /**
     * Call JAX-RS web service
     *
     * @param message from liquid handling deck
     *
     * @return code and message
     */
    @Override
    public BettalimsResponse sendMessage(String message) {
        ClientResponse response = Client.create().resource("http://" + bettaLimsConfig.getWsHost() + ":" +
                                                           bettaLimsConfig.getWsPort() + "/bettalimsmessage")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(message)
                .post(ClientResponse.class);
        return new BettalimsResponse(response.getStatus(), response.getEntity(String.class));
    }
}
