package org.broadinstitute.gpinformatics.infrastructure.jmx;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mocks.ExplodingCache;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


@Test(groups = TestGroups.DATABASE_FREE)
public class ExternalDataCacheControlTest {

    private ExternalDataCacheControl cacheControl;

    private ExplodingCache explodingCache;

    @BeforeMethod
    public void setUp() {
        explodingCache = new ExplodingCache();
        cacheControl = new ExternalDataCacheControl();
    }



    @Test
    public void testCacheRegistrationCatchesExceptions() {
        cacheControl.registerCache(explodingCache);
        Assert.assertEquals(explodingCache.numRefreshes, 0);
    }

    @Test
    public void testCacheRemoval() {
        cacheControl.registerCache(explodingCache);
        cacheControl.unRegisterCache(explodingCache);
        cacheControl.invalidateCache();

        Assert.assertEquals(explodingCache.numRefreshes, 0);
    }

    @Test
    public void testCacheInvalidationCatchesExceptions() {
        cacheControl.registerCache(explodingCache);
        cacheControl.invalidateCache();

        Assert.assertEquals(explodingCache.numRefreshes, 1);
    }
}
