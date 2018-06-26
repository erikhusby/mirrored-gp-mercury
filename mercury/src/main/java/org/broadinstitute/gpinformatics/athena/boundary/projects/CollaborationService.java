/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the 
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support 
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its 
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.projects;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.CollaborationData;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserService;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationPortalException;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationPortalService;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collections;

/**
 * This class is responsible for managing a Portal collaboration for the project manager.
 */
@Dependent
public class CollaborationService {

    private final ResearchProjectEjb researchProjectEjb;

    private final BSPUserList userList;

    private final CollaborationPortalService collaborationPortalService;

    private final BSPUserService bspUserService;

    private final UserBean userBean;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public CollaborationService() {
        this(null, null, null, null, null);
    }

    @Inject
    public CollaborationService(ResearchProjectEjb researchProjectEjb,
                                CollaborationPortalService collaborationPortalService, BSPUserList userList,
                                BSPUserService bspUserService, UserBean userBean) {
        this.researchProjectEjb = researchProjectEjb;
        this.collaborationPortalService = collaborationPortalService;
        this.userList = userList;
        this.bspUserService = bspUserService;
        this.userBean = userBean;
    }

    /**
     * Do everything to start a new portal collaboration. This includes:
     *
     * <dl>
     *     <dt>In Mercury</dt>
     *       <dd>Add the PRIMARY_EXTERNAL collaborator to the research project</dd>
     *       <dd>If the email was used and the collaborator exists in BSP add as external collaborator and
     *           use as if selected</dd>
     *       <dd>If the email was used and the collaborator does not exist in BSP, it will be sent to the portal for
     *           invitation.
     *       </dd>
     *       <dd>The research project will be updated with appropriate information</dd>
     *     <dt>In Portal</dt>
     *         <dd>A call will be made to create the collaboration. The portal will then invite the user if there
     *         is no account or associate the user and notify them of the new collaboration</dd>
     *         <dd>Will request user account information</dd>
     *     <dt>In BSP</dt>
     *         <dd>Will request user account information</dd>
     * </dl>
     *
     * @param researchProject The research project
     * @param selectedCollaborator The bsp user id for an external collaborator that was specified
     * @param collaboratorEmail The email of a collaborator that was specifically specified
     * @param quoteId the collaboration's quote ID
     * @param sampleKitRecipient The person to send kits to for fulfilling the order.
     * @param collaborationMessage The optional extra message from the project manager
     */
    public void beginCollaboration(@Nonnull ResearchProject researchProject, @Nullable Long selectedCollaborator,
                                   @Nullable String collaboratorEmail, @Nonnull String quoteId,
                                   SampleKitRecipient sampleKitRecipient,
                                   @Nullable String collaborationMessage)
            throws CollaborationNotFoundException, CollaborationPortalException {

        // If both the selected collaborator and the specified collaborators are null, then throw an exception.
        if ((selectedCollaborator == null) && (collaboratorEmail == null)) {
            throw new IllegalArgumentException("Must specify a Collaborator Domain User ID or an email address");
        }

        if (researchProject.getCohortIds().length != 1) {
            throw new IllegalArgumentException("A collaboration requires one and only one cohort to be defined " +
                                               "on the research project");
        }

        // Look up the selected id.
        BspUser bspUser = null;
        if (selectedCollaborator != null) {
            bspUser = userList.getById(selectedCollaborator);
        }

        // If there is no bsp user, look up by email.
        if (bspUser == null) {
            bspUser = userList.getByEmail(collaboratorEmail);
        }

        // Still no BSP user, so ask BSP to create one for us.
        if (bspUser == null) {
            bspUser = bspUserService.createCollaborator(collaboratorEmail, userBean.getBspUser());
        }

        if (bspUser == null) {
            throw new RuntimeException("Could not create a BSP user with the email address " + collaboratorEmail);
        }

        // Ensure that the user is an external collaborator.
        // Add the user as an external collaborator in case they are not there already.
        researchProjectEjb.addPeople(researchProject.getBusinessKey(), RoleType.EXTERNAL,
                Collections.singletonList(bspUser));

        // Now tell the portal to create this collaborator.
        String collaborationId =
                collaborationPortalService.beginCollaboration(researchProject, bspUser, quoteId,
                        sampleKitRecipient, collaborationMessage);
        if (StringUtils.isBlank(collaborationId)) {
            throw new RuntimeException("Could not create a Collaboration");
        }
    }

    public CollaborationData getCollaboration(@Nonnull String researchProjectKey)
            throws CollaborationNotFoundException, CollaborationPortalException {
        return collaborationPortalService.getCollaboration(researchProjectKey);
    }

    public String resendInvitation(@Nonnull String researchProjectKey)
            throws CollaborationNotFoundException, CollaborationPortalException {
        return collaborationPortalService.resendInvitation(researchProjectKey);
    }
}
