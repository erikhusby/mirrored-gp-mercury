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

package org.broadinstitute.gpinformatics.mercury.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign_;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

/**
 * This class is the funding implementation of the token object
 *
 * @author hrafal
 */
// FIXME: update this code and its caller to use TokenInput<> instead.
@Dependent
public class ReagentDesignTokenInput extends TokenInput<ReagentDesign> {

    @Inject
    private ReagentDesignDao reagentDesignDao;

    public ReagentDesignTokenInput() {
        super(SINGLE_LINE_FORMAT);
    }

    @Override
    protected ReagentDesign getById(String reagentDesignId) {
        return reagentDesignDao.findByBusinessKey(reagentDesignId);
    }

    @SuppressWarnings("unchecked")
    public String getJsonString(String query) throws JSONException {
        List<ReagentDesign> reagentDesignList =
            reagentDesignDao.findListWithWildcard(
                ReagentDesign.class, query, true, ReagentDesign_.designName, ReagentDesign_.targetSetName);
        return createItemListString(reagentDesignList);
    }

    @Override
    protected String getTokenId(ReagentDesign reagentDesign) {
        return reagentDesign.getBusinessKey();
    }

    @Override
    protected String formatMessage(String messageString, ReagentDesign reagentDesign) {
        return MessageFormat.format(
            messageString, reagentDesign.getBusinessKey() + " (" + reagentDesign.getTargetSetName() + ")");
    }

    @Override
    protected String getTokenName(ReagentDesign reagentDesign) {
        return reagentDesign.getBusinessKey() + " (" + reagentDesign.getTargetSetName() + ")";
    }
}
