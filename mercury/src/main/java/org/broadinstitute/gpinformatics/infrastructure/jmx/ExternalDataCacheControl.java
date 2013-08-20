package org.broadinstitute.gpinformatics.infrastructure.jmx;

import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import java.util.ArrayList;
import java.util.List;

/**
 * Registry of scheduled items for the application.  To use this class, subclass AbstractCache
 * and override refreshCache().
 */
@Singleton
@Startup
public class ExternalDataCacheControl extends AbstractCacheControl {

    private final List<AbstractCache> caches = new ArrayList<>();

    private static final int MAX_SIZE = 100000;

    private int maxCacheSize = MAX_SIZE;

    @Override
    @Schedule(minute = "*/5", hour = "*", persistent = false)
    public void invalidateCache() {
        for (AbstractCache cache : caches) {
            cache.refreshCache();
        }
    }

    /**
     * Add a cache to the list of caches to refresh.
     * @param cache the cache to add
     */
    public void registerCache(AbstractCache cache) {
        caches.add(cache);
        cache.refreshCache();
    }

    /**
     * Remove a cache from the list of caches to refresh.
     * @param cache the cache to remove
     * @return true if the cache was removed
     */
    public boolean unRegisterCache(AbstractCache cache) {
        return caches.remove(cache);
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
