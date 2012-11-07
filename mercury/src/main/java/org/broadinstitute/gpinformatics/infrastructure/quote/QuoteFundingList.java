package org.broadinstitute.gpinformatics.infrastructure.quote;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.logging.Log;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Application wide access to the quote funding list (POs and COs). The list is currently cached once at application
 * startup. In the future, we may want to rebuild the list regularly to account for changes to the user database.
 */
@Named
// MLC @ApplicationScoped breaks the test, as does @javax.ejb.Singleton.  @javax.inject.Singleton is the CDI version
// and does appear to work.  Much to learn about CDI still...
@Singleton
public class QuoteFundingList {

    private Set<Funding> fundingList;

    @Inject
    private PMBQuoteService quoteService;

    @Inject
    private Log logger;

    /**
     * @return list of bsp users, sorted by cohortId.
     */
    public Set<Funding> getFunding() {
        // any time the funding is null but we are asking for it, try and retrieve it again
        if (fundingList == null) {
            refreshFunding();
        }

        return fundingList;
    }

    /**
     * @param fundingTypeAndName key of cohort to look up
     *
     * @return if found, the cohort, otherwise null
     */
    public Funding getById(String fundingTypeAndName) {
        if (getFunding() != null) {
            for (Funding funding : getFunding()) {
                if (funding.getFundingTypeAndName().equals(fundingTypeAndName)) {
                    return funding;
                }
            }
        }

        return null;
    }

    /**
     * Returns the funding using the generated name using two letter type and funding name
     *
     * @param fullString - The funding type and name
     * @return the BSP user or null
     */
    public Funding getByFullString(String fullString) {
        return getById(fullString);
    }

    /**
     * Returns a list of cohorts whose id, name, group, or category match and is NOT archived
     *
     * @param query the query string to match on
     * @return a list of matching users
     */
    public List<Funding> find(String query) {
        String[] lowerQueryItems = query.toLowerCase().split("\\s");
        List<Funding> results = new ArrayList<Funding>();
        for (Funding funding : getFunding()) {
            boolean eachItemMatchesSomething = true;
            for (String lowerQuery : lowerQueryItems) {
                // If none of the fields match this item, then all items are not matched
                if (!anyFieldMatches(lowerQuery, funding)) {
                    eachItemMatchesSomething = false;
                }
            }

            if (eachItemMatchesSomething) {
                results.add(funding);
            }
        }

        return results;
    }

    private boolean anyFieldMatches(String lowerQuery, Funding funding) {
        return funding.getFundingTypeAndName().toLowerCase().contains(lowerQuery) ||
               funding.getMatchDescription().toLowerCase().contains(lowerQuery);
    }

    public synchronized void refreshFunding() {
        try {
            fundingList = ImmutableSet.copyOf(quoteService.getAllFundingSources());
        } catch (Exception ex) {
            logger.debug("Could not refresh the funding list", ex);
        }
    }
}
