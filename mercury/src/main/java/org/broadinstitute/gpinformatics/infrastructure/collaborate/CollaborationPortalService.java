package org.broadinstitute.gpinformatics.infrastructure.collaborate;


import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.projects.SampleKitRecipient;
import org.broadinstitute.gpinformatics.athena.entity.project.CollaborationData;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * Service to talk to the CollaborationData Portal.
 */
@Default
@Dependent
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
     *
     * @param researchProject The research project.
     * @param collaborator The collaborator.
     * @param sampleKitRecipient The person to send kits to for fulfilling the order.
     * @param collaborationMessage The optional message from the PM to the collaborator.
     *  @return The collaboration id
     */
    public String beginCollaboration(@Nonnull ResearchProject researchProject, @Nonnull BspUser collaborator,
                                     @Nonnull String quoteId, @Nonnull SampleKitRecipient sampleKitRecipient,
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
        WebTarget resource = getJerseyClient().target(url);

        CollaborationData collaboration =
                new CollaborationData(researchProject.getName(), researchProject.getSynopsis(),
                        researchProject.getBusinessKey(), collaborator.getUserId(), projectManager.getUserId(), quoteId,
                        sampleKitRecipient, collaborationMessage);

        try {
            return resource.request(MediaType.APPLICATION_XML).post(Entity.xml(collaboration), String.class);
        } catch (WebApplicationException e) {
            rethrowIfCollaborationError(e);
            throw new CollaborationNotFoundException("Could not communicate with collaboration portal at " + url, e);
        }
    }

    public CollaborationData getCollaboration(@Nonnull String researchProjectKey)
            throws CollaborationNotFoundException, CollaborationPortalException {

        String url = config.getUrlBase() + Endpoint.GET_COLLABORATION_DETAILS.getSuffixUrl() + researchProjectKey;
        WebTarget resource = getJerseyClient().target(url);

        try {
            return resource.request(MediaType.APPLICATION_XML).get(CollaborationData.class);
        } catch (WebApplicationException e) {
            throw new CollaborationPortalException("Could not communicate with collaboration portal at " + url, e);
        }
    }


    public String resendInvitation(@Nonnull String researchProjectKey) throws CollaborationPortalException {

        String url = config.getUrlBase() + Endpoint.RESEND_INVITATION.getSuffixUrl() + researchProjectKey;
        WebTarget resource = getJerseyClient().target(url);

        try {
            return resource.request().post(null, String.class);
        } catch (WebApplicationException e) {
            rethrowIfCollaborationError(e);
            throw new CollaborationPortalException("Could not communicate with collaboration portal at " + url, e);
        }
    }

    private static void rethrowIfCollaborationError(WebApplicationException e) throws CollaborationPortalException {
        if (e.getResponse().getStatus() == Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()) {
            throw new CollaborationPortalException(e.getResponse().readEntity(String.class), e);
        }
    }
}
