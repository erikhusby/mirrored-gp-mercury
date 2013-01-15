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

import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign_;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import java.util.List;

/**
 * This class is the funding implementation of the token object
 *
 * @author hrafal
 */
public class ReagentDesignTokenInput extends TokenInput<ReagentDesign> {
    @Inject
    private ReagentDesignDao reagentDesignDao;

    public ReagentDesignTokenInput() {
        super();
    }

    @Override
    protected ReagentDesign getById(String reagentId) {
        return reagentDesignDao.findByBusinessKey(reagentId);
    }

    @SuppressWarnings("unchecked")
    public static String getJsonString(ReagentDesignDao reagentDesignDao, String query) throws JSONException {
        List<ReagentDesign> reagentDesignList = reagentDesignDao.findListWithWildcard(ReagentDesign.class, query, true,
                ReagentDesign_.designName, ReagentDesign_.targetSetName);

        JSONArray itemList = new JSONArray();
        for (ReagentDesign reagentDesign : reagentDesignList) {
            itemList.put(new AutoCompleteToken(String.valueOf(reagentDesign.getBusinessKey()),
                    reagentDesign.getBusinessKey() + " (" + reagentDesign.getTargetSetName() + ")", false)
                    .getJSONObject());
        }

        return itemList.toString();
    }

    public static String getReagentDesignCompleteData(ReagentDesignDao reagentDesignDao, String reagentDesignId)
            throws JSONException {

        JSONArray itemList = new JSONArray();

        ReagentDesign reagentDesign = reagentDesignDao.findByBusinessKey(reagentDesignId);
        if (reagentDesign != null) {
            itemList.put(new AutoCompleteToken(reagentDesignId, reagentDesign.getBusinessKey(), false).getJSONObject());
        }

        return itemList.toString();
    }
}
