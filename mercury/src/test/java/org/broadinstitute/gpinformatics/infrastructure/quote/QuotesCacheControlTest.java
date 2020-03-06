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


import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Mockito;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class QuotesCacheControlTest {
    private QuotesCache quotesCache;

    @Test(dataProviderClass = QuotesCacheDataProvider.class, dataProvider = "quotesForCacheProvider")
    public void testGetQuotesFindsQuotes(String quote, boolean inQuoteCache) throws Exception {
        setupQuoteService();
        Quote foundQuote = quotesCache.getQuote(quote);
        if (inQuoteCache) {
            assertThat(foundQuote, not(nullValue()));
        } else {
            assertThat(foundQuote, nullValue());
        }
    }

    public void testGetQuotesReallyCachesQuote() throws Exception {
        QuoteService mockQuoteService = setupQuoteService();
        String testQuoteId = QuoteService.DEV_QUOTES.get(0);

        final int desiredInvocationCount = QuoteService.DEV_QUOTES.size();

        Quote foundQuote = quotesCache.getQuote(testQuoteId);
        assertThat(foundQuote, not(nullValue()));

        Mockito.verify(mockQuoteService, Mockito.times(desiredInvocationCount)).getQuoteByAlphaId(Mockito.anyString());
        foundQuote = quotesCache.getQuote(testQuoteId);
        assertThat(foundQuote, not(nullValue()));
        Mockito.verify(mockQuoteService, Mockito.times(desiredInvocationCount)).getQuoteByAlphaId(Mockito.anyString());

    }

    public QuoteService setupQuoteService() throws Exception {
        QuoteService mockQuoteService = Mockito.mock(QuoteService.class);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(Mockito.anyString())).thenAnswer(invocation -> {
            String quoteId = (String) invocation.getArguments()[0];
            return new Quote(quoteId, null, null);
        });
        quotesCache = new QuotesCache(mockQuoteService);
        return mockQuoteService;
    }

    @DataProvider
    public static Iterator<Object[]> nullOrBlankInputProvider() {
        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{null,});
        testCases.add(new Object[]{StringUtils.EMPTY});
        testCases.add(new Object[]{" "});

        return testCases.iterator();
    }

    @Test(dataProvider = "nullOrBlankInputProvider")
    public void testGetQuotesCanHandleNullInput(String input) throws Exception {
        setupQuoteService();

        try {
            Quote foundQuote = quotesCache.getQuote(input);
            assertThat(foundQuote, is(nullValue()));
        } catch (Exception e) {
            assertThat("Should Not Have Happened", false);
        }
    }
}
