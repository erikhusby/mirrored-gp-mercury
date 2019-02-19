package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Application wide access to BSP's cohort list (collections). The list is currently cached once at application startup. In the
 * future, we may want to rebuild the list regularly to account for changes to the user database.
 */
// MLC @ApplicationScoped breaks the test, as does @javax.ejb.Singleton.  @javax.inject.Singleton is the CDI version
// and does appear to work.  Much to learn about CDI still...
@ApplicationScoped
@Default
public class BSPCohortList extends AbstractCache {

    private static final Log logger = LogFactory.getLog(BSPCohortList.class);

    private Set<Cohort> cohortList;

    @Inject
    private Deployment deployment;

    private BSPCohortSearchService cohortSearchService;

    public BSPCohortList() {
    }

    @Inject
    public BSPCohortList(BSPCohortSearchService cohortSearchService) {
        this.cohortSearchService = cohortSearchService;
    }

    /**
     * @return list of bsp users, sorted by cohortId.
     */
    public Set<Cohort> getCohorts() {
        if (cohortList == null) {
            refreshCache();
        }
        return cohortList;
    }

    /**
     * @param cohortId key of cohort to look up
     *
     * @return if found, the cohort, otherwise null
     */
    public Cohort getById(String cohortId) {
        if (getCohorts() != null) {
            for (Cohort cohort : getCohorts()) {
                if (cohort.getCohortId().equals(cohortId)) {
                    return cohort;
                }
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

        String[] lowerQueryItems = query.toLowerCase().split("\\s");
        List<Cohort> results = new ArrayList<>();
        if (getCohorts() != null) {
            for (Cohort cohort : getCohorts()) {
                if (!cohort.isArchived()) {
                    addCohortIfMatches(lowerQueryItems, results, cohort);
                }
            }
        }

        return results;
    }

    private static void addCohortIfMatches(String[] lowerQueryItems, List<Cohort> results, Cohort cohort) {
        for (String lowerQuery : lowerQueryItems) {
            // If none of the fields match this item, then all items are not matched
            if (!anyFieldMatches(lowerQuery, cohort)) {
                return;
            }
        }

        results.add(cohort);
    }

    /**
     * Returns a list of users whose id, name, group, or category match
     *
     * @param query the query string to match on
     * @return a list of matching users
     */
    public List<Cohort> find(String query) {

        String[] lowerQueryItems = query.toLowerCase().split("\\s");
        List<Cohort> results = new ArrayList<>();
        if (getCohorts() != null) {
            for (Cohort cohort : getCohorts()) {
                addCohortIfMatches(lowerQueryItems, results, cohort);
            }
        }

        return results;
    }

    private static boolean anyFieldMatches(String lowerQuery, Cohort cohort) {
        return cohort.getCohortId().toLowerCase().contains(lowerQuery) ||
               cohort.getName().toLowerCase().contains(lowerQuery) ||
               cohort.getGroup().toLowerCase().contains(lowerQuery) ||
               cohort.getCategory().toLowerCase().contains(lowerQuery);
    }

    @Override
    public synchronized void refreshCache() {
        try {
            Set<Cohort> rawCohorts = cohortSearchService.getAllCohorts();

            // if fails, use previous cache entry (even if it's null)
            if (rawCohorts == null) {
                return;
            }

            cohortList = ImmutableSet.copyOf(rawCohorts);
        } catch (Exception ex) {
            logger.error("Could not refresh the cohort list", ex);
        }
    }

    public String getCohortName(String cohortId) {
        String cohortName = "";
        if (cohortId != null) {
            Cohort cohort = getById(cohortId);

            if (cohort == null) {
                return "(Unknown cohort: " + cohortId + ")";
            }
            return cohort.getName();
        }

        return cohortName;
    }

    public String getCohortListString(String[] cohortIds) {
        String cohortListString = "";

        if ((cohortIds != null) && (cohortIds.length > 0)) {
            String[] nameList = new String[cohortIds.length];
            int i=0;
            for (String cohortId : cohortIds) {
                nameList[i++] = getCohortName(cohortId);
            }

            cohortListString = StringUtils.join(nameList, ", ");
        }

        return cohortListString;
    }
}
