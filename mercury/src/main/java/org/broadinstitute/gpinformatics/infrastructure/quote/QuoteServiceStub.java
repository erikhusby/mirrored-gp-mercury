package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.Date;
import java.util.Set;

@Stub
@Alternative
public class QuoteServiceStub implements QuoteService {

    private static int workItemId = 1;

    public QuoteServiceStub() {

    }

    @Override
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        PriceList priceList;

        try {
            priceList = QuoteServerDataSnapshotter.readPriceListFromTestFile();
        }
        catch(Exception e) {
            throw new RuntimeException("Failed to read price list from disk",e);
        }
        return priceList;
    }


    @Override
    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
        Quotes quotes;
        try {
            quotes = QuoteServerDataSnapshotter.readAllQuotesFromTestFile();
        }
        catch(Exception e) {
            throw new RuntimeException("Failed to read quotes from disk",e);
        }
        return quotes;
    }


    @Override
    public String registerNewWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing, Date reportedCompletionDate,
                                  double numWorkUnits, String callbackUrl, String callbackParameterName, String callbackParameterValue) {
        return Integer.toString(workItemId++);
    }

    @Override
    public Quote getQuoteByAlphaId(String id) throws QuoteServerException, QuoteNotFoundException {
        Quote quote = null;

        for (Quote aQuote : getAllSequencingPlatformQuotes().getQuotes()) {
            if (aQuote.getAlphanumericId().equals(id)) {
                quote = aQuote;
            }
        }
        return quote;
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
