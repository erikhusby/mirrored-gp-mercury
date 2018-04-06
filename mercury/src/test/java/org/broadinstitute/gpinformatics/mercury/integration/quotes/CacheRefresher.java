package org.broadinstitute.gpinformatics.mercury.integration.quotes;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;

/**
 * Moved from an inner class per JavaEE7/Wildfly requirement
 */
@Singleton
public class CacheRefresher {

    @Resource
    private TimerService timerService;

    @Timeout
    public void timeout(Timer timer) {
        PriceListCache priceListCache = (PriceListCache) timer.getInfo();
        try {
            priceListCache.refreshCache();
        } catch (Exception ignored) {
            // Throwing an exception can cause the timeout method to be retried, which we don't want here.
        }
    }

    /**
     * Queue a call to this EJB's @Timeout annotated method.
     *
     * @param priceListCache the PriceListCache to refresh
     */
    public void queueRefresh(PriceListCache priceListCache) {
        timerService.createSingleActionTimer(1, new TimerConfig(priceListCache, false));
    }
}
