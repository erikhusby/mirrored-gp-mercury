package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
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
        return createItemListString(cohorts);
    }

    @Override
    protected boolean isSingleLineMenuEntry() {
        return false;
    }

    @Override
    protected String getTokenId(Cohort cohort) {
        return cohort.getCohortId();
    }

    @Override
    protected String getTokenName(Cohort cohort) {
        return cohort.getDisplayName();
    }

    @Override
    protected String[] getMenuLines(Cohort cohort) {
        String[] lines = new String[2];
        lines[0] = cohort.getDisplayName();
        lines[1] = cohort.getGroup() + " " + cohort.getCategory();
        return lines;
    }
}
