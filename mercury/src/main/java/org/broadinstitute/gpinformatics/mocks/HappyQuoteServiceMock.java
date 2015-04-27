package org.broadinstitute.gpinformatics.mocks;

import org.broadinstitute.gpinformatics.infrastructure.quote.*;

import javax.enterprise.inject.Alternative;
import java.util.Date;
import java.util.Set;

/**
 * Mock that returns you a new quote with some
 * funds remaining when you call #getQuoteByAlphaId.
 * DO NOT DELETE THIS CLASS!  YOU WILL NEED THIS CLASS
 * AS AN ALTERNATIVE IN BEANS.XML TO TEST THE APP
 * WHEN THE QUOTE SERVERS ARE DOWN.
 */
@Alternative
public class HappyQuoteServiceMock implements QuoteService {

    @Override
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        return new PriceList();
    }

    @Override
    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
        return new Quotes();
    }

    @Override
    public String registerNewWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                  Date reportedCompletionDate, double numWorkUnits, String callbackUrl,
                                  String callbackParameterName, String callbackParameterValue) {
        throw new RuntimeException("happy mock can't do this");
    }

    @Override
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        Quote q = new Quote();
        q.setAlphanumericId(alphaId);
        QuoteFunding funding = new QuoteFunding("99999");
        q.setQuoteFunding(funding);
        return q;
    }

    @Override
    public Quote getQuoteWithPriceItems(String alphaId) throws QuoteServerException, QuoteNotFoundException {
        return getQuoteByAlphaId(alphaId);
    }

    @Override
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException{
        return null;
    }

    @Override
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {
        return null;
    }

    @Override
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType)
            throws QuoteServerException, QuoteNotFoundException {
        return getAllPriceItems();
    }
}