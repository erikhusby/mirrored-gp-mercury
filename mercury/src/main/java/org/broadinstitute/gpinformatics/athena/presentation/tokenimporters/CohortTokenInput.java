package org.broadinstitute.gpinformatics.athena.presentation.tokenimporters;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONException;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.List;

/**
 * This class is the cohort implementation of the token object
 *
 * @author hrafal
 */
@Dependent
public class CohortTokenInput extends TokenInput<Cohort> {

    @Inject
    private BSPCohortList cohortList;

    public CohortTokenInput() {
        super(DOUBLE_LINE_FORMAT);
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
    protected String getTokenId(Cohort cohort) {
        return cohort.getCohortId();
    }

    @Override
    protected String getTokenName(Cohort cohort) {
        return cohort.getDisplayName();
    }

    @Override
    protected String formatMessage(String messageString, Cohort cohort) {
        return MessageFormat.format(
            messageString, cohort.getDisplayName(), cohort.getGroup() + " " + cohort.getCategory());
    }
}
