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
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.CollaborationData;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationPortalException;
import org.broadinstitute.gpinformatics.infrastructure.collaborate.CollaborationPortalService;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

/**
 * This class is responsible for managing a Portal collaboration for the project manager.
 */
@Stateful
@RequestScoped
public class CollaborationEjb {

    private final ResearchProjectDao researchProjectDao;

    private final BSPUserList userList;

    private final CollaborationPortalService collaborationPortalService;

    private final BSPUserService bspUserService;

    private final UserBean userBean;

    // EJBs require a no arg constructor.
    @SuppressWarnings("unused")
    public CollaborationEjb() {
        this(null, null, null, null, null);
    }

    @Inject
    public CollaborationEjb(ResearchProjectDao researchProjectDao, CollaborationPortalService collaborationPortalService,
                            BSPUserList userList, BSPUserService bspUserService, UserBean userBean) {
        this.researchProjectDao = researchProjectDao;
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
     *     <dt><In Portal/dt>
     *         <dd>A call will be made to create the collaboration. The portal will then invite the user if there
     *         is no account or associate the user and notify them of the new collaboration</dd>
     *         <dd>Will request user account information</dd>
     *     <dt>In BSP</dt>
     *         <dd>Will request user account information</dd>
     * </dl>
     *
     * @param researchProjectKey The research project business key so it can be fetched as part of the transaction here.
     * @param selectedCollaborator The bsp user id for an external collaborator that was specified
     * @param collaboratorEmail The email of a collaborator that was specifically specified
     * @param collaborationMessage The optional extra message from the project manager
     */
    public void beginCollaboration(@Nonnull String researchProjectKey, @Nullable Long selectedCollaborator,
                                   @Nullable String collaboratorEmail, @Nullable String collaborationMessage)
            throws CollaborationNotFoundException, CollaborationPortalException {

        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);

        // If both the selected collaborator and the specified collaborators are null, then throw an exception.
        if ((selectedCollaborator == null) && (collaboratorEmail == null)) {
            throw new IllegalArgumentException("must specify a collaborator domain user id or an email address");
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
            throw new RuntimeException("Couldn't create a BSP user with the email address " + collaboratorEmail);
        }

        // Ensure that the user is an external collaborator.
        // Add the user as an external collaborator in case they are not there already.
        researchProject.addPerson(RoleType.EXTERNAL, bspUser.getUserId());

        // Now tell the portal to create this collaborator.
        String collaborationId = collaborationPortalService.beginCollaboration(researchProject,
                bspUser, collaborationMessage);
        if (StringUtils.isBlank(collaborationId)) {
            throw new RuntimeException("Could not create a collaboration");
        }
    }

    public CollaborationData getCollaboration(@Nonnull String collaborationId)
            throws CollaborationNotFoundException, CollaborationPortalException {
        return collaborationPortalService.getCollaboration(collaborationId);
    }
}
