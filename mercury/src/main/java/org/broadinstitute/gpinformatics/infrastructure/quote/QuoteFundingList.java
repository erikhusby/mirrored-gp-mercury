package org.broadinstitute.gpinformatics.infrastructure.quote;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Application wide access to the quote funding list (POs and COs). The list is regularly refreshed by
 * ExternalDataCacheControl.
 */
// MLC @ApplicationScoped breaks the test, as does @javax.ejb.Singleton.  @javax.inject.Singleton is the CDI version
// and does appear to work.  Much to learn about CDI still...
@ApplicationScoped
public class QuoteFundingList extends AbstractCache {

    private Set<Funding> fundingList;

    private QuoteService quoteService;

    private static final Log log = LogFactory.getLog(QuoteFundingList.class);

    @Inject
    private Deployment deployment;


    public QuoteFundingList() {
    }

    @Inject
    public QuoteFundingList(QuoteService quoteService) {
        this.quoteService = quoteService;
    }

    /**
     * @return list of bsp users, sorted by cohortId.
     */
    public Set<Funding> getFunding() {
        // any time the funding is null but we are asking for it, try and retrieve it again
        if (fundingList == null) {
            refreshCache();
        }

        return fundingList;
    }

    /**
     * @param fundingTypeAndName key of cohort to look up
     *
     * @return if found, the cohort, otherwise null
     */
    public Funding getById(final String fundingTypeAndName) {
        if (getFunding() != null) {
            for (Funding funding : getFunding()) {
                if (funding.getDisplayName().equals(fundingTypeAndName)) {
                    return funding;
                }
            }
        }

        // In case of error, return a placeholder object to show to the user.
        return new Funding() {
            @Override
            public String getDisplayName() {
                return "Unknown Funding: " + fundingTypeAndName;
            }
        };
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
        List<Funding> results = new ArrayList<>();
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

    private static boolean anyFieldMatches(String lowerQuery, Funding funding) {
        return funding.getDisplayName().toLowerCase().contains(lowerQuery) ||
               funding.getMatchDescription().toLowerCase().contains(lowerQuery);
    }

    @Override
    public synchronized void refreshCache() {
        try {
            Set<Funding> rawFunding = quoteService.getAllFundingSources();

            // if fails, use previous cache entry (even if it's null)
            if (rawFunding == null) {
                return;
            }

            fundingList = ImmutableSet.copyOf(rawFunding);
        } catch (QuoteServerException | QuoteNotFoundException ex) {
            log.error("Could not refresh the funding list.", ex);
        }
    }
}
