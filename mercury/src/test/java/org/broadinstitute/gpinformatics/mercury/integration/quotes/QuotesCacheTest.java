package org.broadinstitute.gpinformatics.mercury.integration.quotes;


import org.broadinstitute.gpinformatics.infrastructure.quote.*;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

/**
 * Modified from Mercury
 */

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class QuotesCacheTest {

    Quote quote1 = new Quote("DNA32", new QuoteFunding(Collections.singleton(new FundingLevel("100",
            Collections.singleton(new Funding(Funding.FUNDS_RESERVATION, "Magical Infinite Grant", "Magical Infinite Grant"))))), ApprovalStatus.FUNDED);
    Quote quote2 = new Quote("DNA33", new QuoteFunding(Collections.singleton(new FundingLevel("100",
            Collections.singleton(new Funding(Funding.FUNDS_RESERVATION, "Magical Infinite Grant", "Magical Infinite Grant"))))), ApprovalStatus.FUNDED);
    Quote quote3 = new Quote("DNA34", new QuoteFunding(Collections.singleton(new FundingLevel("100",
            Collections.singleton(new Funding(Funding.FUNDS_RESERVATION, "Cheap Grant", "Cheap Grant"))))), ApprovalStatus.FUNDED);
    Quote quote4 = new Quote("DNA35", new QuoteFunding(Collections.singleton(new FundingLevel("50",
            Collections.singleton(new Funding(Funding.FUNDS_RESERVATION, "NHGRI", "NHGRI"))))), ApprovalStatus.FUNDED);


    @Test(groups = {TestGroups.EXTERNAL_INTEGRATION})
    public void test_quotes_for_funding_source() throws Exception {
        Quotes quotes = new Quotes();
        Funding targetSource = new Funding(Funding.FUNDS_RESERVATION, "Magical Infinite Grant", "Magical Infinite Grant");

        quotes.addQuote(quote1);
        quotes.addQuote(quote2);
        quotes.addQuote(quote3);

        Assert.assertEquals(3, quotes.getQuotes().size());

        QuotesCache cache = new QuotesCache(quotes);
        Collection<Quote> foundQuotes = cache.getQuotesForGrantDescription(targetSource.getGrantDescription());

        Assert.assertFalse(foundQuotes == null);
        Assert.assertEquals(2, foundQuotes.size());

        Assert.assertTrue(foundQuotes.contains(quote1));
        Assert.assertTrue(foundQuotes.contains(quote2));
        Assert.assertFalse(foundQuotes.contains(quote3));
    }

    @Test(groups = {TestGroups.EXTERNAL_INTEGRATION})
    public void test_unique_funding_soures() throws Exception {
        Quotes quotes = new Quotes();
        quotes.addQuote(quote1);
        quotes.addQuote(quote2);
        quotes.addQuote(quote3);
        quotes.addQuote(quote4);

        Assert.assertEquals(quotes.getQuotes().size(), 4);

        QuotesCache cache = new QuotesCache(quotes);
        Collection<Funding> fundingSources = cache.getAllFundingSources();

        Assert.assertFalse(fundingSources == null);
        Assert.assertEquals(fundingSources.size(), 3);

        Assert.assertTrue(fundingSources.contains(new Funding(Funding.FUNDS_RESERVATION, "Magical Infinite Grant", "Magical Infinite Grant")));
        Assert.assertTrue(fundingSources.contains(new Funding(Funding.FUNDS_RESERVATION, "Cheap Grant", "Cheap Grant")));
        Assert.assertTrue(fundingSources.contains(new Funding(Funding.FUNDS_RESERVATION, "NHGRI", "NHGRI")));

        Assert.assertEquals(quotes.getQuotes(), cache.getQuotes());
    }


    // Very Slow external test.
    @Test(groups = {TestGroups.EXTERNAL_INTEGRATION}, enabled = false)
    public void test_known_good_funding_sources() throws Exception {

        QuoteService quoteService = QuoteServiceProducer.testInstance();

        long start = System.currentTimeMillis();
        QuotesCache cache = new QuotesCache(quoteService.getAllQuotes());
        System.out.println("Quotes call took " + (System.currentTimeMillis() - start) + "ms");

        Funding nhgriGrant = new Funding(Funding.FUNDS_RESERVATION, "NHGRI_NIH_LANDER", "NHGRI_NIH_LANDER");
        start = System.currentTimeMillis();
        Collection<Quote> foundQuotes = cache.getQuotesForGrantDescription(nhgriGrant.getGrantDescription());
        System.out.println("Search for quotes took " + (System.currentTimeMillis() - start) + "ms");

        Assert.assertFalse(foundQuotes == null);
        Assert.assertTrue(0 < foundQuotes.size());

        for (Quote foundQuote : foundQuotes) {
            StringBuilder quoteInfo =
                    new StringBuilder(foundQuote.getAlphanumericId());
            quoteInfo.append(" ");
            for(FundingLevel level : foundQuote.getQuoteFunding().getFundingLevel()) {

                for (Funding funding :level.getFunding()) {
                    quoteInfo.append("--");
                    quoteInfo.append(funding.getCostObject());
                }
            }
            System.out.println(quoteInfo);
        }

        // print out the quotes per grant.
        Map<Funding, HashSet<Quote>> myMap = cache.getQuotesByFundingSource();
        for (Funding funding : myMap.keySet()) {
            System.out.print(funding.getSponsorName() + "\t" + funding.getCostObject() + "\t"
                    + funding.getGrantNumber() + "\t" + funding.getGrantDescription() + "\t" + funding.getGrantStartDate() + "\t" + funding.getGrantEndDate() + "\t");
            for (Quote quote : myMap.get(funding)) {
                System.out.print(quote.getAlphanumericId() + ", ");
            }
            System.out.println();
        }
    }

}
