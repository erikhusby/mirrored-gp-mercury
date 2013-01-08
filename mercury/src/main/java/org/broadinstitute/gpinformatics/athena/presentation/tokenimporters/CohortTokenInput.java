package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.boundary.CohortListBean;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.AutoCompleteToken;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;

import javax.inject.Inject;
import java.util.List;

/**
 * This class is the cohort implementation of the token object
 *
 * @author hrafal
 */
public class CohortTokenInput extends TokenInput<Cohort> {

    @Inject
    private CohortListBean cohortListBean;

    public CohortTokenInput() {
        super();
    }

    @Override
    protected Cohort getById(String cohort) {
        return cohortListBean.getCohortById(cohort);
    }

    public static String getJsonString(CohortListBean cohortListBean, String query) throws JSONException {
        List<Cohort> cohorts = cohortListBean.searchActiveCohort(query);

        JSONArray itemList = new JSONArray();
        for (Cohort cohort : cohorts) {
            itemList.put(new AutoCompleteToken(cohort.getCohortId(), cohort.getDisplayName(), false).getJSONObject());
        }
        return itemList.toString();
    }

    public static String getCohortCompleteData(CohortListBean cohortListBean, String[] cohortIds) throws JSONException {
        JSONArray itemList = new JSONArray();
        for (String cohortId : cohortIds) {
            Cohort cohort = cohortListBean.getCohortById(cohortId);
            itemList.put(new AutoCompleteToken(cohortId, cohort.getDisplayName(), false).getJSONObject());
        }

        return itemList.toString();

    }
}
