package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.QpcrRunBean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Implementation of connector to BettaLIMS
 */
@Dependent
@Default
public class BettaLimsConnectorImpl implements BettaLimsConnector {

    @Inject
    private BettaLimsConfig bettaLimsConfig;

    /**
     * for CDI
     */
    public BettaLimsConnectorImpl() {
    }

    public BettaLimsConnectorImpl(BettaLimsConfig bettaLimsConfig) {
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
    public BettaLimsResponse sendMessage(String message) {
        ClientResponse response = Client.create().resource("http://" + bettaLimsConfig.getWsHost() + ":" +
                                                           bettaLimsConfig.getWsPort() + "/bettalimsmessage")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(message)
                .post(ClientResponse.class);
        return new BettaLimsResponse(response.getStatus(), response.getEntity(String.class));
    }

    @Override
    public Response createQpcrRun(QpcrRunBean qpcrRunBean) {
        ClientResponse response = Client.create().resource("http://" + bettaLimsConfig.getWsHost() + ":" +
                                                           bettaLimsConfig.getWsPort() + "/libraryquant/qpcrrun")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(qpcrRunBean)
                .post(ClientResponse.class);
        return Response.status(response.getStatus()).build();
    }

    @Override
    public Response createLibraryQuants(LibraryQuantRunBean libraryQuantRunBean) {
        ClientResponse response = Client.create().resource("http://" + bettaLimsConfig.getWsHost() + ":" +
                                                           bettaLimsConfig.getWsPort() + "/libraryquant")
                .type(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .entity(libraryQuantRunBean)
                .post(ClientResponse.class);
        return Response.status(response.getStatus()).build();
    }
}
