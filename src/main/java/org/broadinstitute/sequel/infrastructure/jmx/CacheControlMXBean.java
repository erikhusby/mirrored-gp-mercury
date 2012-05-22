package org.broadinstitute.sequel.infrastructure.jmx;

/**
 * Basic JMX bean that you can use to change cache size
 * and invalidate a cache via JConsole.  See {@link org.broadinstitute.sequel.boundary.zims.IlluminaRunResource#controlBean}
 * for an example of how to use.
 */
public interface CacheControlMXBean {

    public void invalidateCache();

    public int getMaximumCacheSize();

    public void setMaximumCacheSize(int max);

}
