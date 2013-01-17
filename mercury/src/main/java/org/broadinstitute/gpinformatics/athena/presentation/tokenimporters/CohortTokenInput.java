package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
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
    private BSPCohortList cohortList;

    public CohortTokenInput() {
    }

    @Override
    protected Cohort getById(String cohort) {
        return cohortList.getById(cohort);
    }

    public String getJsonString(String query) throws JSONException {
        List<Cohort> cohorts = cohortList.findActive(query);

        JSONArray itemList = new JSONArray();
        for (Cohort cohort : cohorts) {
            itemList.put(getJSONObject(cohort.getCohortId(), cohort.getDisplayName(), false));
        }
        return itemList.toString();
    }

    @Override
    public String generateCompleteData() throws JSONException {
        JSONArray itemList = new JSONArray();
        for (Cohort cohort : getTokenObjects()) {
            itemList.put(getJSONObject(cohort.getCohortId(), cohort.getDisplayName(), false));
        }

        return itemList.toString();
    }
}
