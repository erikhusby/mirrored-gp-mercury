package org.broadinstitute.gpinformatics.mocks;

import org.broadinstitute.gpinformatics.infrastructure.quote.*;

import javax.enterprise.inject.Alternative;
import java.util.Date;

/**
 * Mock that returns you a new quote with some
 * funds remaining when you call #getQuoteByAlphaId.
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
    public String registerNewWork(Quote quote, PriceItem priceItem, Date reportedCompletionDate, double numWorkUnits, String callbackUrl, String callbackParameterName, String callbackParameterValue) {
        throw new RuntimeException("happy mock can't do this");
    }

    @Override
    public Quote getQuoteByNumericId(String numericId) throws QuoteServerException, QuoteNotFoundException {
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
}
