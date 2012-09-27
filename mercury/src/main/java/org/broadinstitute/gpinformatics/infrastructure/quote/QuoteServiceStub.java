package org.broadinstitute.gpinformatics.infrastructure.quote;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

@Stub
public class QuoteServiceStub implements QuoteService {

    private static int workItemId = 1;

    public QuoteServiceStub() {

    }

    @Override
    public Quote getQuoteFromQuoteServer(String id) throws QuoteServerException, QuoteNotFoundException {
        Quote quote = null;

        for (Quote aQuote : getAllSequencingPlatformQuotes().getQuotes()) {
            if (aQuote.getAlphanumericId().equals(id)) {
                quote = aQuote;
            }
        }
        return quote;
    }

    @Override
    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
        PriceList priceList = null;

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
        Quotes quotes = null;
        try {
            quotes = QuoteServerDataSnapshotter.readAllQuotesFromTestFile();
        }
        catch(Exception e) {
            throw new RuntimeException("Failed to read quotes from disk",e);
        }
        return quotes;
    }

    @Override
    public String registerNewWork(Quote quote, PriceItem priceItem, double numWorkUnits, String callbackUrl, String callbackParameterName, String callbackParameterValue) {
        return Integer.toString(workItemId++);
    }

    @Override
    public Quote getQuoteByNumericId(final String numericId) throws QuoteServerException, QuoteNotFoundException {
        //TODO PMB remove this impl method
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Quote getQuoteByAlphaId(final String alphaId) throws QuoteServerException, QuoteNotFoundException {
        //TODO PMB remove this impl method
        throw new IllegalStateException("Not Yet Implemented");
        //return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
