package org.broadinstitute.gpinformatics.mercury.integration.quotes;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Arquillian tests for PriceListCache.
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class PriceListCacheContainerTest extends StubbyContainerTest {

    public PriceListCacheContainerTest(){}

    @Inject
    private CacheRefresher cacheRefresher;

    @Inject
    private PriceListCache priceListCache;

    /**
     * Test that {@link PriceListCache} refreshes are able to use the injected {@link QuoteService}. It was discovered
     * as a result of GPLIM-2719 that, if the injected QuoteService is session-scoped, the scheduler can not invoke it
     * because it does not have a session scope.
     */
    @Test
    public void testCacheRefreshFromTimer() throws Exception {
        /*
         * Refresh the cache once to get all initialization out of the way. When the injected PriceListCache is first
         * used, the Weld proxy instantiates the actual bean, which self-registers with ExternalDataCacheControl, which
         * calls refreshCache(). By calling refreshCache() once up front before resetting the invocation count, we can
         * be sure that there won't be any unexpected invocations (more so anyway, but there's still the possibility
         * that ExternalDataCacheControl can sneak in a refresh).
         */
        try {
            priceListCache.refreshCache();
        } catch (Exception ignored) {
            // Don't care if it fails, just need to invoke it.
        }

        QuoteServiceStub.resetInvocationCount();
        cacheRefresher.queueRefresh(priceListCache);
        Thread.sleep(1000);
        assertThat(QuoteServiceStub.getInvocationCount(), equalTo(1));
    }
}
