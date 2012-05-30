package org.broadinstitute.sequel.infrastructure.jmx;

/**
 * A cache control JMX bean for {@link org.broadinstitute.sequel.boundary.zims.IlluminaRunResource}.
 */
public class ZimsCacheControl extends AbstractCacheControl {

    private int maxCacheSize = 1000;

    boolean wasInvalidated;

    @Override
    public void invalidateCache() {
        wasInvalidated = true;
    }

    @Override
    public int getMaximumCacheSize() {
        return maxCacheSize;
    }

    @Override
    public void setMaximumCacheSize(int max) {
        this.maxCacheSize = max;
    }

    public boolean wasInvalidated() {
        return wasInvalidated;
    }

    public void reset() {
        wasInvalidated = false;
    }

}
