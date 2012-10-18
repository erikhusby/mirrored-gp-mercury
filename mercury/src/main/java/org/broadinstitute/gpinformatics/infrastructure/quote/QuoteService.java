package org.broadinstitute.gpinformatics.infrastructure.quote;

import java.io.Serializable;

/**
 *
 * Service to talk to the quote server.
 */
public interface QuoteService extends Serializable {
    // todo extract and marshall current quote server xml

    // todo rename impl and bsp impl to "LiveBSP" and "LiveQuote", same for jira

    // todo make registerNewWork mock

    /**
     * Asks the quote server for basic information about a quote.
     *
     * @param id Alphanumeric ID for the quote
     * @return If the quote exists the return value will be a quote object. Otherwise null.
     */
    public Quote getQuoteFromQuoteServer(String id) throws QuoteServerException, QuoteNotFoundException;

    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException;

    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException;

    public String registerNewWork(Quote quote,
                                  PriceItem priceItem,
                                  double numWorkUnits,
                                  String callbackUrl,
                                  String callbackParameterName,
                                  String callbackParameterValue);

    public Quote getQuoteByNumericId(final String numericId) throws QuoteServerException, QuoteNotFoundException;

    //TODO Remove this method, its a dupe of getQuoteFromQuoteServer
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException;
}