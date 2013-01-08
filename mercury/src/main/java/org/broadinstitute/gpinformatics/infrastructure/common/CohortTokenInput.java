package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.athena.boundary.CohortListBean;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;

/**
 * This class is the cohort implementation of the token object
 *
 * @author hrafal
 */
public class CohortTokenInput extends TokenInput<Cohort> {

    private CohortListBean cohortListBean;

    public CohortTokenInput(CohortListBean cohortListBean) {
        super();
        this.cohortListBean = cohortListBean;
    }

    @Override
    protected Cohort getById(String cohort) {
        return cohortListBean.getCohortById(cohort);
    }
}
