package org.broadinstitute.gpinformatics.infrastructure.quote;

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

    private Set<Funding> fundingList = null;

    @Inject
    private PMBQuoteService quoteService;

    /**
     * @return list of bsp users, sorted by cohortId.
     */
    public Set<Funding> getFunding() {
        if (fundingList == null) {
            try {
                fundingList = quoteService.getAllFundingSources();
            } catch (Exception ex) {
                // If there are any problems with BSP, just leave the cohort list null for later when BSP does exist
            }
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
        String lowerQuery = query.toLowerCase();
        List<Funding> results = new ArrayList<Funding>();
        if (getFunding() != null) {
            for (Funding funding : getFunding()) {
                if (funding.getFundingTypeAndName().toLowerCase().contains(lowerQuery) ||
                    funding.getMatchDescription().toLowerCase().contains(lowerQuery)) {
                    results.add(funding);
                }
            }
        }

        return results;
    }
}
