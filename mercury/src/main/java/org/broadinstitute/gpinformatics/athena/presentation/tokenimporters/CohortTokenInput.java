package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
            createAutocomplete(itemList, cohort);
        }
        return itemList.toString();
    }

    @Override
    public String generateCompleteData() throws JSONException {
        JSONArray itemList = new JSONArray();
        for (Cohort cohort : getTokenObjects()) {
            createAutocomplete(itemList, cohort);
        }

        return itemList.toString();
    }

    private void createAutocomplete(JSONArray itemList, Cohort cohort) throws JSONException {
        JSONObject item = getJSONObject(cohort.getCohortId(), cohort.getDisplayName(), false);
        item.put("group", cohort.getGroup());
        item.put("category", cohort.getCategory());
        itemList.put(item);
    }
}
