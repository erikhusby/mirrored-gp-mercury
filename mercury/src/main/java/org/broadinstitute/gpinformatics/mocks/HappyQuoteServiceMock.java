package org.broadinstitute.gpinformatics.mocks;

import org.broadinstitute.gpinformatics.athena.boundary.billing.QuoteImportItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePlatformType;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;


import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Mock that returns you a new quote with some
 * funds remaining when you call #getQuoteByAlphaId.
 * DO NOT DELETE THIS CLASS!  YOU WILL NEED THIS CLASS
 * AS AN ALTERNATIVE IN BEANS.XML TO TEST THE APP
 * WHEN THE QUOTE SERVERS ARE DOWN.
 */
@Alternative
@ApplicationScoped
public class HappyQuoteServiceMock implements QuoteService {

    public HappyQuoteServiceMock(){}

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
                                  String callbackParameterName, String callbackParameterValue, BigDecimal priceAdjustment) {
        throw new RuntimeException("happy mock can't do this");
    }

    @Override
    public String registerNewSAPWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                     Date reportedCompletionDate, double numWorkUnits, String callbackUrl,
                                     String callbackParameterName, String callbackParameterValue, BigDecimal priceAdjustment) {
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
    public PriceList getPriceItemsForDate(List<QuoteImportItem> targetedPriceItemCriteria)
            throws QuoteServerException, QuoteNotFoundException {
        return null;
    }

    @Override
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType)
            throws QuoteServerException, QuoteNotFoundException {
        return getAllPriceItems();
    }
}