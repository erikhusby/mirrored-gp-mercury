package org.broadinstitute.gpinformatics.infrastructure.collaborate;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.CollaborationData;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;

/**
 *
 * Service to talk to the CollaborationData Portal.
 */
@Default
public class CollaborationPortalService extends AbstractJerseyClientService {

    private static final long serialVersionUID = 5340477906783139812L;

    enum Endpoint {
        BEGIN_COLLABORATION("/rest/collaborate/create"),
        GET_COLLABORATION_DETAILS("/rest/collaborate/get/");

        String suffixUrl;

        Endpoint(String suffixUrl) {
            this.suffixUrl = suffixUrl;
        }

        public String getSuffixUrl() {
            return suffixUrl;
        }
    }

    private final CollaborateConfig config;

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param config The configuration for connecting with the collaboration portal.
     */
    @Inject
    public CollaborationPortalService(CollaborateConfig config) {
        this.config = config;
    }

    @Inject
    private BSPUserList bspUserList;

    @Override
    protected void customizeClient(Client client) {
//        specifyHttpAuthCredentials(client, portalConfig);
    }

    /**
     * Tell the collaboration portal that the research project should be shared with the user. Depending on the
     * state of the user in BSP and the Portal, te account will be created/added to the appropriate application. If
     * the user does not have a portal account, an invitation will be sent for them to fill in all needed information.
     *
     * @param researchProject The research project.
     * @param selectedCollaborator The actual existing collaborator identifier.
     * @param specifiedCollaborator The email specified for an existing user or a new user.
     * @param collaborationMessage The optional message from the PM to the collaborator.
     *
     * @return The collaboration id
     */
    public String beginCollaboration(
            @Nonnull ResearchProject researchProject, @Nullable Long selectedCollaborator, @Nullable String specifiedCollaborator,
            @Nullable String collaborationMessage)
            throws CollaborationNotFoundException, CollaborationPortalException {

        if (researchProject.getProjectManagers().length < 1) {
            throw new IllegalArgumentException(
                    "Cannot start a collaboration on a research project with no Project Manager");
        }
        BspUser projectManager = bspUserList.getById(researchProject.getProjectManagers()[0]);
        if (projectManager == null) {
            throw new IllegalArgumentException("Not a valid BSP Project Manager");
        }

        String url = config.getUrlBase() + Endpoint.BEGIN_COLLABORATION.getSuffixUrl();
        WebResource resource = getJerseyClient().resource(url);

        CollaborationData collaboration =
                new CollaborationData(researchProject.getName(), researchProject.getSynopsis(), specifiedCollaborator,
                        selectedCollaborator, projectManager.getDomainUserId(), collaborationMessage);

        resource.type(MediaType.APPLICATION_XML).post(ClientResponse.class, collaboration);

        try {
            return resource.accept(MediaType.TEXT_PLAIN).get(String.class);
        } catch (UniformInterfaceException e) {
            throw new CollaborationNotFoundException("Could not communicate with collaboration portal at " + url);
        } catch (ClientHandlerException e) {
            throw new CollaborationPortalException("Could not communicate with collaboration portal at " + url, e);
        }
    }

    public CollaborationData getCollaboration(String collaborationId)
            throws CollaborationNotFoundException, CollaborationPortalException {

        String url = config.getUrlBase() + Endpoint.GET_COLLABORATION_DETAILS.getSuffixUrl() + collaborationId;
        WebResource resource = getJerseyClient().resource(url);

        try {
            return resource.accept(MediaType.APPLICATION_XML).get(CollaborationData.class);
        } catch (UniformInterfaceException e) {
            throw new CollaborationNotFoundException("Could not communicate with collaboration portal at " + url);
        } catch (ClientHandlerException e) {
            throw new CollaborationPortalException("Could not communicate with collaboration portal at " + url, e);
        }
    }
}
