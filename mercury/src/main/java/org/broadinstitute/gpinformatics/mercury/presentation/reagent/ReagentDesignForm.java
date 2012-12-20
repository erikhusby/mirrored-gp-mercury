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
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.Arrays;
import java.util.List;

/**
 * @author dryan
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
    private ReagentsBean reagentsBean;

    @Inject
    Conversation conversation;
    final String LIST_PAGE = "list_reagent_designs";


    public void initForm() {
        reagentsBean.getReagentDesign();
    }

    public void initView() {
        if (!facesContext.isPostback()) {
            reagentsBean.setReagentDesignTableData(reagentDesignDao.findAll());
            if (conversation.isTransient()) {
                conversation.begin();
            }
        }
    }


    public String edit() {
        String updatedOrCreated = isCreating() ? "created" : "updated";
        try {
            reagentDesignDao.persist(reagentsBean.getReagentDesign());
        } catch (Exception e) {
            log.error(e);
            addErrorMessage(e.getMessage());
            return null;
        }
        final String infoMessage =
                String.format("The Research Design \"%s\" has been %s.",
                        reagentsBean.getReagentDesign().getDesignName(),
                        updatedOrCreated);
        addInfoMessage(infoMessage);
        return redirect(LIST_PAGE);
    }

    public FacesContext getFacesContext() {
        return facesContext;
    }

    public boolean isCreating() {
        return (reagentsBean.getReagentDesign() == null || reagentsBean.getReagentDesign().getDesignName() == null);
    }

    public List<ReagentDesign.ReagentType> getReagentTypes() {
        return Arrays.asList(ReagentDesign.ReagentType.values());
    }
}
