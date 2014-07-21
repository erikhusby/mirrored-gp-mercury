package org.broadinstitute.gpinformatics.infrastructure.collaborate;


import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
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
import javax.ws.rs.core.Response;

/**
 *
 * Service to talk to the CollaborationData Portal.
 */
@Default
public class CollaborationPortalService extends AbstractJerseyClientService {

    private static final long serialVersionUID = 5340477906783139812L;

    enum Endpoint {
        BEGIN_COLLABORATION("/rest/collaborate/create"),
        GET_COLLABORATION_DETAILS("/rest/collaborate/get/"),
        RESEND_INVITATION("/rest/collaborate/resendInvitation/");

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
     * the user has never logged into the portal, an invitation will be sent for them to fill in all needed information.
     *
     * @param researchProject The research project.
     * @param collaborator The collaborator.
     * @param collaborationMessage The optional message from the PM to the collaborator.
     *
     * @return The collaboration id
     */
    public String beginCollaboration(@Nonnull ResearchProject researchProject, @Nonnull BspUser collaborator,
                                     @Nonnull String quoteId,
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
                new CollaborationData(researchProject.getName(), researchProject.getSynopsis(),
                        researchProject.getBusinessKey(),
                        collaborator.getUserId(), projectManager.getUserId(), quoteId, collaborationMessage);

        try {
            return resource.type(MediaType.APPLICATION_XML).post(String.class, collaboration);
        } catch (UniformInterfaceException e) {
            rethrowIfCollaborationError(e);
            throw new CollaborationNotFoundException("Could not communicate with collaboration portal at " + url, e);
        } catch (ClientHandlerException e) {
            throw new CollaborationPortalException("Could not communicate with collaboration portal at " + url, e);
        }
    }

    public CollaborationData getCollaboration(@Nonnull String researchProjectKey)
            throws CollaborationNotFoundException, CollaborationPortalException {

        String url = config.getUrlBase() + Endpoint.GET_COLLABORATION_DETAILS.getSuffixUrl() + researchProjectKey;
        WebResource resource = getJerseyClient().resource(url);

        try {
            return resource.accept(MediaType.APPLICATION_XML).get(CollaborationData.class);
        } catch (UniformInterfaceException e) {
            // No collaboration yet.
            return null;
        } catch (ClientHandlerException e) {
            throw new CollaborationPortalException("Could not communicate with collaboration portal at " + url, e);
        }
    }


    public String resendInvitation(@Nonnull String researchProjectKey) throws CollaborationPortalException {

        String url = config.getUrlBase() + Endpoint.RESEND_INVITATION.getSuffixUrl() + researchProjectKey;
        WebResource resource = getJerseyClient().resource(url);

        try {
            return resource.post(String.class);
        } catch (UniformInterfaceException e) {
            rethrowIfCollaborationError(e);
            throw new CollaborationPortalException("Could not communicate with collaboration portal at " + url, e);
        }
    }

    private static void rethrowIfCollaborationError(UniformInterfaceException e) throws CollaborationPortalException {
        if (e.getResponse().getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new CollaborationPortalException(e.getResponse().getEntity(String.class), e);
        }
    }
}
