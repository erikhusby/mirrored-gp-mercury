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
import org.broadinstitute.gpinformatics.infrastructure.jsf.TableData;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.ConversationScoped;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;

/**
 * @author breilly
 */
@Named
@RequestScoped
public class ReagentDesignForm extends AbstractJsfBean {
    @Inject
    private Log log;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private FacesContext facesContext;

    @Inject
    Conversation conversation;

    private ReagentDesign reagentDesign;

    public ReagentDesign getReagentDesign() {
        return reagentDesign;
    }

    public void setReagentDesign(ReagentDesign reagentDesign) {
        this.reagentDesign = reagentDesign;
    }

    public List<ReagentDesign> getAllReagentDesigns() {
        return reagentDesignTableData.getValues();
    }

    @ConversationScoped
    public static class ReagentDesignTableData extends TableData<ReagentDesign> {
    }

    @Inject
    ReagentDesignForm.ReagentDesignTableData reagentDesignTableData;

    public void initView() {
        if (!facesContext.isPostback()) {
            reagentDesignTableData.setValues(reagentDesignDao.findAll());
            if (conversation.isTransient()) {
                conversation.begin();
            }
        }
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

    public Conversation getConversation() {
        return conversation;
    }

    public FacesContext getFacesContext() {
        return facesContext;
    }

    public String edit() {
        String updatedOrCreated = "updated";
        if (reagentDesign.getReagentDesignId() == null) {
            updatedOrCreated = "created";
        }
        try {
            reagentDesignDao.persist(reagentDesign);
        } catch (Exception e) {
            log.error(e);
            addErrorMessage(e.getMessage());
            return null;
        }
        final String infoMessage = String.format("The Research Design \"%s\" has been %s.",
                reagentDesign.getDesignName(), updatedOrCreated);
        addInfoMessage(infoMessage);
        return redirect("list");
    }

    public ReagentDesignTableData getReagentDesignTableData() {
        return reagentDesignTableData;
    }
}
