package org.broadinstitute.sequel.quotes;


import org.broadinstitute.sequel.control.quote.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;

public class QuotesCacheTest {

    Quote quote1 = new Quote("DNA32",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"Magical Infinite Grant"))));
    Quote quote2 = new Quote("DNA33",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"Magical Infinite Grant"))));
    Quote quote3 = new Quote("DNA34",new QuoteFunding(new FundingLevel("100",new Funding(Funding.FUNDS_RESERVATION,"Cheap Grant"))));
    Quote quote4 = new Quote("DNA35",new QuoteFunding(new FundingLevel("50",new Funding(Funding.FUNDS_RESERVATION,"NHGRI"))));

    @Test
    public void test_quotes_for_funding_source() throws Exception {
        Quotes quotes = new Quotes();
        Funding targetSource = new Funding(Funding.FUNDS_RESERVATION,"Magical Infinite Grant");

        quotes.addQuote(quote1);
        quotes.addQuote(quote2);
        quotes.addQuote(quote3);
        
        Assert.assertEquals(3,quotes.getQuotes().size());
        
        QuotesCache cache = new QuotesCache(quotes);
        Collection<Quote> foundQuotes = cache.getQuotesForGrantDescription(targetSource.getGrantDescription());
        
        Assert.assertFalse(foundQuotes == null);
        Assert.assertEquals(2,foundQuotes.size());
        
        Assert.assertTrue(foundQuotes.contains(quote1));
        Assert.assertTrue(foundQuotes.contains(quote2));
        Assert.assertFalse(foundQuotes.contains(quote3));
    }

    @Test
    public void test_unique_funding_soures() throws Exception {
        Quotes quotes = new Quotes();
        quotes.addQuote(quote1);
        quotes.addQuote(quote2);
        quotes.addQuote(quote3);
        quotes.addQuote(quote4);

        Assert.assertEquals(4,quotes.getQuotes().size());

        QuotesCache cache = new QuotesCache(quotes);
        Collection<Funding> fundingSources = cache.getAllFundingSources();

        Assert.assertFalse(fundingSources == null);
        Assert.assertEquals(3, fundingSources.size());

        Assert.assertTrue(fundingSources.contains(new Funding(Funding.FUNDS_RESERVATION,"Magical Infinite Grant")));
        Assert.assertTrue(fundingSources.contains(new Funding(Funding.FUNDS_RESERVATION,"Cheap Grant")));
        Assert.assertTrue(fundingSources.contains(new Funding(Funding.FUNDS_RESERVATION,"NHGRI")));
    }

    @Test(groups = {"slow"})
    public void test_known_good_funding_sources() throws Exception {

        long start = System.currentTimeMillis();
        QuotesCache cache = new QuotesCache(new QuoteServiceImpl(new QAQuoteConnectionParams(QuoteConnectionParameters.GET_ALL_SEQUENCING_QUOTES_URL)).getAllSequencingPlatformQuotes());
        System.out.println("Quotes call took " + (System.currentTimeMillis() - start) + "ms");
        
        Funding nhgriGrant = new Funding(Funding.FUNDS_RESERVATION,"NHGRI_NIH_LANDER");
        start = System.currentTimeMillis();
        Collection<Quote> foundQuotes = cache.getQuotesForGrantDescription(nhgriGrant.getGrantDescription());
        System.out.println("Search for quotes took " + (System.currentTimeMillis() - start) + "ms");
        
        Assert.assertFalse(foundQuotes == null);
        Assert.assertTrue(0 < foundQuotes.size());

        Assert.assertTrue(foundQuotes.contains(new Quote("DNA3PI")));
        Assert.assertTrue(foundQuotes.contains(new Quote("DNA3PK")));
        Assert.assertFalse(foundQuotes.contains(new Quote("DNA4DW")));

    }
}
