package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.mercury.BSPJaxRsClient;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.util.EnumSet;

import static javax.ws.rs.core.Response.Status.ACCEPTED;
import static javax.ws.rs.core.Response.Status.OK;

/**
 * BSP API for creating a user. Currently only used to create an external collaborator.
 */
@Dependent
public class BSPUserService extends BSPJaxRsClient {
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

        MultivaluedHashMap<String, String> formData = new MultivaluedHashMap<>();
        formData.add("emailAddress", emailAddress);
        formData.add("createdById", String.valueOf(creator.getUserId()));
        Response clientResponse =
                getJerseyClient().target(urlString)
                        .request(MediaType.TEXT_XML_TYPE)
                        .post(Entity.form(formData));

        BspUser bspUser = (BspUser) new XStream().fromXML(clientResponse.readEntity(String.class));
        Response.Status clientResponseStatus = Response.Status.fromStatusCode(clientResponse.getStatus());
        clientResponse.close();

        if (!EnumSet.of(ACCEPTED, OK).contains(clientResponseStatus)) {
            log.error("Got error code " + clientResponseStatus + " calling web service " + urlString +
                      " with parameters " + formData);
            return null;
        }

        bspUserList.addUser(bspUser);
        return bspUser;
    }
}
