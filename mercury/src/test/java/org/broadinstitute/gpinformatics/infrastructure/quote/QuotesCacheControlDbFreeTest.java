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
import org.broadinstitute.gpinformatics.mocks.ExplodingCache;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class QuotesCacheControlDbFreeTest {

    private static final String EXPECTED_LOG_MESSAGE_REGEX = ".*" + ExplodingCache.class.getName() + ".*";

    private QuotesCache quotesCache;

    @Test
    public void testGetQuotesFindsQuotes() throws QuoteNotFoundException, QuoteServerException {
        setupQuoteService();

        QuoteService.DEV_QUOTES.forEach(quote->{
            quotesCache.getQuote(quote);
            assertThat(quote, not(nullValue()));
        });
    }

    public void testGetQuotesReallyCachesQuote() throws QuoteNotFoundException, QuoteServerException {
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

    public QuoteService setupQuoteService() throws QuoteServerException, QuoteNotFoundException {
        QuoteService mockQuoteService = Mockito.mock(QuoteService.class);
        Mockito.when(mockQuoteService.getQuoteByAlphaId(Mockito.anyString())).thenAnswer(invocation -> {
            String quoteId = (String) invocation.getArguments()[0];
            return new Quote(quoteId, null, null);
        });
        quotesCache = new QuotesCache(mockQuoteService);
        return mockQuoteService;
    }

    @Test
    public void testGetQuotesDoesNotFindQuotes() throws QuoteNotFoundException, QuoteServerException {
        QuoteService quoteService = setupQuoteService();
        Mockito.verify(quoteService, Mockito.never()).getQuoteByAlphaId(Mockito.anyString());
        String quote = "13444";
        Quote foundQuote = quotesCache.getQuote(quote);
        assertThat(foundQuote, is(nullValue()));

    }

    @Test
    public void testGetQuotesCanHandleNullInput() throws QuoteNotFoundException, QuoteServerException {
        QuoteService quoteService = setupQuoteService();
        String quote = null;
        Quote foundQuote = quotesCache.getQuote(quote);
        assertThat(foundQuote, is(nullValue()));
    }

    @Test
    public void testGetQuotesCanHandleBlankInput() throws QuoteNotFoundException, QuoteServerException {
        setupQuoteService();
        String quote = StringUtils.EMPTY;
        Quote foundQuote = quotesCache.getQuote(quote);
        assertThat(foundQuote, is(nullValue()));
    }

    public void testGetQuotesDoesntQueryDevQuotes() throws QuoteNotFoundException, QuoteServerException {
        QuotesCache quotesCache = Mockito.mock(QuotesCache.class);
        Mockito.when(quotesCache.getQuote(Mockito.anyString())).thenReturn(new Quote("GP87U", null, null));

        QuoteServiceImpl mockQuoteService = Mockito.spy(new QuoteServiceImpl(QuoteConfig.produce(DEV), quotesCache));

        try {
            mockQuoteService.getQuoteByAlphaId("1234");
        } catch (QuoteNotFoundException e) {
            // ignoring
        }
        Mockito.verify(mockQuoteService, Mockito.times(1)).getJaxRsClient();
        Mockito.verify(quotesCache, Mockito.never()).getQuote(Mockito.anyString());
    }
}
