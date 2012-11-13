package org.broadinstitute.gpinformatics.infrastructure.jmx;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;

/**
 * Update singleton caches used for athena side of mercury
 */
@Singleton
public class ExternalDataCacheControl extends AbstractCacheControl {
    @Inject
    private BSPCohortList cohortList;

    @Inject
    private BSPUserList userList;

    @Inject
    private QuoteFundingList fundingList;

    @Inject
    private PriceListCache priceListCache;

    private static final int MAX_SIZE = 100000;

    private int maxCacheSize = MAX_SIZE;

    @Override
    @Schedule(minute = "*/20", hour = "*")
    public void invalidateCache() {
        cohortList.refreshCohorts();
        userList.refreshUsers();
        priceListCache.refreshPriceList();
        fundingList.refreshFunding();
    }

    @Override
    public int getMaximumCacheSize() {
        return maxCacheSize;
    }

    @Override
    public void setMaximumCacheSize(int max) {
        maxCacheSize = max;
    }
}
