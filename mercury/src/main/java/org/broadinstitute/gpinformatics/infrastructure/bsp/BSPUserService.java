package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.ClientResponse;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * BSP API for creating a user. Currently only used to create an external collaborator.
 */
@Dependent
public class BSPUserService extends BSPJerseyClient {
    private static final long serialVersionUID = 2587957568965043063L;

    private static final Log log = LogFactory.getLog(BSPUserService.class);

    private static final String CREATE_COLLABORATOR_ENDPOINT = "user/createCollaborator";

    @Inject
    BSPUserList bspUserList;

    BSPUserService() {
    }

    /**
     * Create a BSP user as a collaborator. Once the user is created, we regenerate the BspUser list and return the
     * BspUser object.
     */
    public BspUser createCollaborator(String emailAddress, BspUser creator) {
        String urlString = getBspConfig().getJaxRsWebServiceUrl(CREATE_COLLABORATOR_ENDPOINT);
        List<NameValuePair> pairs = new ArrayList<>();
        pairs.add(new BasicNameValuePair("emailAddress", emailAddress));
        pairs.add(new BasicNameValuePair("createdById", String.valueOf(creator.getUserId())));
        String parameters = URLEncodedUtils.format(pairs, CharEncoding.UTF_8);

        ClientResponse clientResponse =
                getJerseyClient().resource(urlString)
                        .type(MediaType.APPLICATION_FORM_URLENCODED_TYPE)
                        .accept(MediaType.TEXT_XML_TYPE)
                        .post(ClientResponse.class, parameters);

        BspUser bspUser = (BspUser) new XStream().fromXML(clientResponse.getEntity(String.class));
        Response.Status clientResponseStatus = Response.Status.fromStatusCode(clientResponse.getStatus());

        if (!EnumSet.of(ACCEPTED, OK).contains(clientResponseStatus)) {
            log.error("Got error code " + clientResponseStatus + " calling web service " + urlString +
                      " with parameters " + parameters);
            return null;
        }

        bspUserList.addUser(bspUser);
        return bspUser;
    }
}
