package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 12/19/12
 * Time: 5:31 PM
 */
@Stub
@Alternative
public class BSPCohortSearchServiceStub implements BSPCohortSearchService {

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
