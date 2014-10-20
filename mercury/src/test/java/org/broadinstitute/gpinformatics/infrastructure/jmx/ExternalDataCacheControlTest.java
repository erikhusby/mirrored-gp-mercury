package org.broadinstitute.gpinformatics.infrastructure.jmx;

import org.broadinstitute.gpinformatics.infrastructure.common.TestLogHandler;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mocks.ExplodingCache;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;


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
        Assert.assertTrue(explodingCache.numRefreshes == 1);
    }

    @Test
    public void testCacheRemoval() {
        cacheControl.registerCache(explodingCache);
        cacheControl.unRegisterCache(explodingCache);
        cacheControl.invalidateCache();

        Assert.assertTrue(explodingCache.numRefreshes == 1);
    }

    @Test
    public void testCacheInvalidationCatchesExceptions() {
        cacheControl.registerCache(explodingCache);
        cacheControl.invalidateCache();

        Assert.assertTrue(explodingCache.numRefreshes == 2);
    }



}
