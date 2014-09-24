package org.broadinstitute.gpinformatics.mocks;


import org.broadinstitute.gpinformatics.infrastructure.jmx.AbstractCache;

/**
 * Cache that throws a runtime exception it's refreshed
 */
public class ExplodingCache extends AbstractCache {

    public static final String EXCEPTION_TEXT = "Kablooie";

    public int numRefreshes = 0;

    @Override
    public void refreshCache() {
        numRefreshes++;
        throw new RuntimeException(EXCEPTION_TEXT);
    }

}