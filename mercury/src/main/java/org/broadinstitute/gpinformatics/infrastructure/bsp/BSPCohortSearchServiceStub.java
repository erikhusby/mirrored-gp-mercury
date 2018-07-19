package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.HashSet;
import java.util.Set;

/**
 * Stubbed version of the cohort service.
 */
@Stub
@Alternative
@Dependent
public class BSPCohortSearchServiceStub implements BSPCohortSearchService {

    public BSPCohortSearchServiceStub(){}

    private static final long serialVersionUID = -4537906882178920633L;

    @Override
    public Set<Cohort> getAllCohorts() {
        Set<Cohort> cohorts = new HashSet<>();

        Cohort cohort = new Cohort("1", "name1", "category1", "group1", true);
        cohorts.add( cohort);
        Cohort cohort2 = new Cohort("2", "name2", "category2", "group2", true);
        cohorts.add( cohort2);
        return cohorts;

    }
}
