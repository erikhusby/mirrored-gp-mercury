package org.broadinstitute.gpinformatics.infrastructure.portal;


import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.enterprise.inject.Default;
import javax.inject.Inject;

/**
 *
 * Service to talk to the Collaboration Portal.
 */
@Default
public class CollaborationPortalService extends AbstractJerseyClientService {

    private static final long serialVersionUID = 5340477906783139812L;

    @Inject
    private PortalConfig portalConfig;

    /**
     * Tell the collaboration portal that the research project should be shared with the user. Depending on the
     * state of the user in BSP and the Portal, te account will be created/added to the appropriate application. If
     * the user does not have a portal account, an invitation will be sent for them to fill in all needed information.
     *
     * @param researchProjectKey The business key of the research project.
     * @param specifiedCollaborator The BSP user who is already on the project as an external collaborator
     * @param collaboratorEmail The email of the collaborator to add
     * @param collaborationMessage The optional message from the PM to the collaborator
     *
     * @return The collaboration id
     */
    public String beginCollaboration(
            String researchProjectKey, String specifiedCollaborator, String collaborationMessage) {
        return "COLLAB-1";
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, portalConfig);
    }
}
