package org.broadinstitute.gpinformatics.infrastructure.jmx;

import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.annotation.Nonnull;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 * Registry of scheduled items for the application.  To use this class, subclass AbstractCache
 * and override refreshCache().
 */
@Singleton
@Startup
public class ExternalDataCacheControl extends AbstractCacheControl {

    private static final Log logger = LogFactory.getLog(ExternalDataCacheControl.class);

    // Synchronization is expected be provided by the container since this class is annotated Singleton.
    private final ListOrderedSet<AbstractCache> caches = new ListOrderedSet<>();

    private static final int MAX_SIZE = 100000;

    private int maxCacheSize = MAX_SIZE;

    @Override
    @Schedule(minute = "*", hour = "*", persistent = false) //xxx
    public void invalidateCache() {
        for (AbstractCache cache : caches) {
            refreshCacheAndLogException(cache);
        }
    }

    /**
     * Adds the cache to the set of caches to be refreshed. This does not do an initial load
     * of the cache.
     * @param cache the cache to add
     */
    public void registerCache(AbstractCache cache) {
        caches.add(cache);
    }

    /**
     * Remove a cache from the list of caches to refresh.
     * @param cache the cache to remove
     * @return true if the cache was removed
     */
    public boolean unRegisterCache(AbstractCache cache) {
        return caches.remove(cache);
    }

    private void refreshCacheAndLogException(@Nonnull AbstractCache cache) {
        try {
            cache.refreshCache();
        }
        catch(Exception e) {
            logger.error("Could not refresh cache " + cache.getClass().getName(),e);
        }
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
