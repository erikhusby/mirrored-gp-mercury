/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2012 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.entity.project.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author breilly
 */
@Named
@RequestScoped
public class ReagentDesignForm extends AbstractJsfBean {

    @Inject
    private Log log;

    @Inject
    private ReagentDesign reagent;

    @Inject
    private FacesContext facesContext;

    @Inject
    private UserBean userBean;

    public void initForm() {
        if (userBean.ensureUserValid()) {
            // Only initialize the form if not a postback. Otherwise, we'll leave the form as the user submitted it.
            if (!facesContext.isPostback()) {

                // Add current user as a PM only if this is a new research project being created
//                if (isCreating()) {
//                    projectManagers = new ArrayList<BspUser>();
//                    projectManagers.add(userBean.getBspUser());
//                } else {
//                    projectManagers = makeBspUserList(detail.getProject().getProjectManagers());
//                    broadPIs = makeBspUserList(detail.getProject().getBroadPIs());
//                    scientists = makeBspUserList(detail.getProject().getScientists());
//                    externalCollaborators = makeBspUserList(detail.getProject().getExternalCollaborators());
//
//                    fundingSources = makeFundingSources(detail.getProject().getFundingIds());
//                    sampleCohorts = makeCohortList(detail.getProject().getCohortIds());
//                    irbs = makeIrbs(detail.getProject().getIrbNumbers());
//                }
            }
        } else {
//            addErrorMessage(MessageFormat.format(UserBean.LOGIN_WARNING,
//                    (isCreating() ? "create" : "edit") + " a research project"));
        }
    }

    public String save() {
//        if (reagent.get()) {
//            return create();
//        } else {
            return edit();
//        }
    }

    public String create() {

        try {
//            researchProjectManager.createResearchProject(project);
        } catch (Exception e) {
            addErrorMessage(e.getMessage());
            return null;
        }

//        addInfoMessage("The Research Project \"" + project.getTitle() + "\" has been created.");
        return redirect("view");
    }

    public String edit() {
//        ResearchProject project = detail.getProject();
//        addCollections(project);

//        project.recordModification(userBean.getBspUser().getUserId());

        try {
  //          researchProjectManager.updateResearchProject(project);
        } catch (Exception e) {
            addErrorMessage(e.getMessage());
            return null;
        }

//        addInfoMessage("The Research Project \"" + project.getTitle() + "\" has been updated.");
        return redirect("view");
    }
}
