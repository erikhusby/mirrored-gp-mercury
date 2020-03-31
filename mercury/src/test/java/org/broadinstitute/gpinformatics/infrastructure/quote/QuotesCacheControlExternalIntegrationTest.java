/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2020 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.quote;


import org.apache.commons.lang.BooleanUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class QuotesCacheControlExternalIntegrationTest {
    public void testQuoteServiceLooksUpQuoteWhenTheyShouldNotBeCached() throws Exception {
        QuotesCache quotesCache = Mockito.mock(QuotesCache.class);
        QuoteServiceImpl mockQuoteService = Mockito.spy(new QuoteServiceImpl(QuoteConfig.produce(DEV), quotesCache));

        try {
            mockQuoteService.getQuoteByAlphaId("1234");
        } catch (QuoteNotFoundException e) {
            // ignoring
        }
        Mockito.verify(mockQuoteService, Mockito.times(1)).getJaxRsClient();
        Mockito.verify(quotesCache, Mockito.never()).getQuote(Mockito.anyString());
    }

    @Test(dataProviderClass = QuotesCacheDataProvider.class, dataProvider = "quotesForCacheProvider")
    public void testQuoteServiceDoesQueryCachedQuotes(String testQuoteId, boolean inQuotesCache) throws Exception {
        Quote testQuote = new Quote(testQuoteId, null, null);

        QuotesCache quotesCache = Mockito.mock(QuotesCache.class);
        Mockito.when(quotesCache.getQuote(Mockito.anyString())).thenReturn(testQuote);

        QuoteServiceImpl quoteService = new QuoteServiceImpl(QuoteConfig.produce(DEV), quotesCache);
        QuoteServiceImpl spyQuoteService = Mockito.spy(quoteService);

        try {
            assertThat(spyQuoteService.getQuoteByAlphaId(testQuoteId), equalTo(testQuote));
        } catch (QuoteNotFoundException e) {
            // ignore me
        }

        Mockito.verify(spyQuoteService, Mockito.times(BooleanUtils.toInteger(!inQuotesCache))).getJaxRsClient();
        Mockito.verify(quotesCache, Mockito.times(BooleanUtils.toInteger(inQuotesCache))).getQuote(Mockito.anyString());
    }
}
