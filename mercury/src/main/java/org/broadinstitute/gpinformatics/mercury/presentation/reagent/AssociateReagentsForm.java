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

package org.broadinstitute.gpinformatics.mercury.presentation.reagent;

import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.DesignedReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.Conversation;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class AssociateReagentsForm extends AbstractJsfBean {
    @Inject
    private Log log;

    @Inject
    LabEventFactory labEventFactory;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private FacesContext facesContext;

    @Inject
    Conversation conversation;

    @Inject
    ReagentsBean reagentsBean;
    final String LIST_PAGE = "list_reagent_designs";

    public void initView() {
        if (!facesContext.isPostback()) {
            if (conversation.isTransient()) {
                conversation.begin();
            }
        }
    }

    public void initForm() {
        reagentsBean.getReagentDesign();
    }

    @SuppressWarnings({"unchecked"})
    public List<ReagentDesign> findReagent(String searchString) {
        return reagentDesignDao.findListWithWildcard(ReagentDesign.class, searchString, true,
                ReagentDesign_.designName,
                ReagentDesign_.targetSetName,
                ReagentDesign_.manufacturersName
        );
    }

    public String associateReagent() {
        if (reagentsBean.getBarcodeMap().isEmpty()) {
            addErrorMessage("Can not associate empty barcode to reagent.");
        }

        List<String> businessKeys = new ArrayList<String>();
        for (String reagentName : reagentsBean.getBarcodeMap().keySet()) {
            if (!reagentsBean.getBarcodeMap().get(reagentName).isEmpty()) {
                businessKeys.add(reagentName);
            }
        }
        List<ReagentDesign> reagentDesigns = reagentDesignDao.findByBusinessKey(businessKeys);

        List<TwoDBarcodedTube> twoDBarcodedTubeList = new ArrayList<TwoDBarcodedTube>();
        for (ReagentDesign reagentDesign : reagentDesigns) {
            String barcodeList[] = reagentsBean.getBarcodeMap().get(reagentDesign.getDesignName()).split("\\W");
            for (String barcodeItem : barcodeList) {
                DesignedReagent reagent = new DesignedReagent(reagentDesign);
                TwoDBarcodedTube twoDee = new TwoDBarcodedTube(barcodeItem);
                twoDee.addReagent(reagent);
                twoDBarcodedTubeList.add(twoDee);
            }
        }

        twoDBarcodedTubeDAO.persistAll(twoDBarcodedTubeList);
        addInfoMessage(String.format("%s tubes initialized with reagents.", twoDBarcodedTubeList.size()));

        return redirect(LIST_PAGE);
    }

    public ReagentsBean getReagentsBean() {
        return reagentsBean;
    }

    public void setReagentsBean(ReagentsBean reagentsBean) {
        this.reagentsBean = reagentsBean;
    }
}
