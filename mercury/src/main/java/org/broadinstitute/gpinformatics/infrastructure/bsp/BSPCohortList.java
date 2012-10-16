package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Application wide access to BSP's cohort list (collections). The list is currently cached once at application startup. In the
 * future, we may want to rebuild the list regularly to account for changes to the user database.
 */
@Named
// MLC @ApplicationScoped breaks the test, as does @javax.ejb.Singleton.  @javax.inject.Singleton is the CDI version
// and does appear to work.  Much to learn about CDI still...
@Singleton
public class BSPCohortList {

    private final Set<Cohort> cohortList;

    /**
     * @return list of bsp users, sorted by cohortId.
     */
    public Set<Cohort> getCohorts() {
        return cohortList;
    }

    /**
     * @param cohortId key of cohort to look up
     *
     * @return if found, the cohort, otherwise null
     */
    public Cohort getById(String cohortId) {
        // Could improve performance here by storing users in a TreeMap.  Wait until performance becomes
        // an issue, then fix.
        for (Cohort cohort : cohortList) {
            if (cohort.getCohortId().equals(cohortId)) {
                return cohort;
            }
        }
        return null;
    }

    /**
     * Returns the BSP cohort for the given string, or null if no no cohort exists with that name. Username comparison
     * ignores case.
     *
     * @param fullString - The cohort object string:
     *                      cohort.getCohortId + ":" cohort.getName() + "(" + cohort.getGroup() + ", " + cohort.getCategory() + ")"
     * @return the BSP user or null
     */
    public Cohort getByFullString(String fullString) {
        // cohort name does not have parens, so can split at the '('
        String cohortId = fullString.split(":")[0];
        return getById(cohortId);
    }

    /**
     * Returns a list of cohorts whose id, name, group, or category match and is NOT archived
     *
     * @param query the query string to match on
     * @return a list of matching users
     */
    public List<Cohort> findActive(String query) {
        String lowerQuery = query.toLowerCase();
        List<Cohort> results = new ArrayList<Cohort>();
        for (Cohort cohort : cohortList) {
            if (!cohort.isArchived()  &&
                 ((cohort.getCohortId().toLowerCase().contains(lowerQuery) ||
                   cohort.getName().toLowerCase().contains(lowerQuery) ||
                   cohort.getGroup().toLowerCase().contains(lowerQuery) ||
                   cohort.getCategory().contains(lowerQuery)))) {
                results.add(cohort);
            }
        }
        return results;
    }

    /**
     * Returns a list of users whose id, name, group, or category match
     *
     * @param query the query string to match on
     * @return a list of matching users
     */
    public List<Cohort> find(String query) {
        String lowerQuery = query.toLowerCase();
        List<Cohort> results = new ArrayList<Cohort>();
        for (Cohort cohort : cohortList) {
            if (cohort.getCohortId().toLowerCase().contains(lowerQuery) ||
                cohort.getName().toLowerCase().contains(lowerQuery) ||
                cohort.getGroup().toLowerCase().contains(lowerQuery) ||
                cohort.getCategory().contains(lowerQuery)) {
                results.add(cohort);
            }
        }
        return results;
    }

    @Inject
    public BSPCohortList(BSPCohortSearchService cohortSearchService) {
        cohortList = cohortSearchService.getAllCohorts();
    }
}
