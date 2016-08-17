package org.broadinstitute.gpinformatics.mercury.integration.quotes;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.STUBBY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Arquillian tests for PriceListCache.
 */
@Test(groups = TestGroups.STUBBY)
public class PriceListCacheContainerTest extends Arquillian {

    @Inject
    private CacheRefresher cacheRefresher;

    @Inject
    private PriceListCache priceListCache;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(STUBBY);
    }

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
        Thread.sleep(100);
        assertThat(QuoteServiceStub.getInvocationCount(), equalTo(1));
    }
}
