package org.broadinstitute.gpinformatics.infrastructure.bettalims;

import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.LibraryQuantRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.generated.QpcrRunBean;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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
        Response response = ClientBuilder.newClient().target("http://" + bettaLimsConfig.getWsHost() + ":" +
                                                           bettaLimsConfig.getWsPort() + "/bettalimsmessage")
                .request(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.xml(message), Response.class);
        return new BettaLimsResponse(response.getStatus(), response.readEntity(String.class));
    }

    @Override
    public Response createQpcrRun(QpcrRunBean qpcrRunBean) {
        Response response = ClientBuilder.newClient().target("http://" + bettaLimsConfig.getWsHost() + ":" +
                                                           bettaLimsConfig.getWsPort() + "/libraryquant/qpcrrun")
                .request(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.xml(qpcrRunBean), Response.class);
        return Response.status(response.getStatus()).build();
    }

    @Override
    public Response createLibraryQuants(LibraryQuantRunBean libraryQuantRunBean) {
        Response response = ClientBuilder.newClient().target("http://" + bettaLimsConfig.getWsHost() + ":" +
                                                           bettaLimsConfig.getWsPort() + "/libraryquant")
                .request(MediaType.APPLICATION_XML_TYPE)
                .accept(MediaType.APPLICATION_XML)
                .post(Entity.xml(libraryQuantRunBean), Response.class);
        return Response.status(response.getStatus()).build();
    }
}
