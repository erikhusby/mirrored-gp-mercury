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

    final String ASSOCIATE_REAGENTS_PAGE = "associate_reagents";

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
        return reagentDesignDao.findListWithWildcard(ReagentDesign.class, searchString,
                ReagentDesign_.reagentDesign,
                ReagentDesign_.targetSetName,
                ReagentDesign_.manufacturersName
        );
    }

    public String associateReagent() {
        if (reagentsBean.getBarcode().isEmpty()) {
            addErrorMessage("Can not associate empty barcode to reagent.");
        }
        String barcodeList[] = reagentsBean.getBarcode().split("\\W");
        List<TwoDBarcodedTube> twoDBarcodedTubeList = new ArrayList<TwoDBarcodedTube>(barcodeList.length);
        for (String barcodeItem : barcodeList) {
            DesignedReagent reagent = new DesignedReagent(reagentsBean.getReagentDesign());
            TwoDBarcodedTube twoDee = new TwoDBarcodedTube(barcodeItem);
            twoDee.addReagent(reagent);
            twoDBarcodedTubeList.add(twoDee);
        }
        twoDBarcodedTubeDAO.persistAll(twoDBarcodedTubeList);
        addInfoMessage(String.format("%s tubes initialized with reagent %s.", twoDBarcodedTubeList.size(),
                reagentsBean.getReagentDesign()));

        return redirect(ASSOCIATE_REAGENTS_PAGE);
    }

    public ReagentsBean getReagentsBean() {
        return reagentsBean;
    }

    public void setReagentsBean(ReagentsBean reagentsBean) {
        this.reagentsBean = reagentsBean;
    }
}
