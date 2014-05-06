package org.broadinstitute.gpinformatics.infrastructure.quote;


import java.util.Set;

/**
 *
 * Service to talk to the quote server.
 */

// todo: merge this into QuoteService and delete (GPLIM-2719)
public interface PMBQuoteService {

    /**
     * Method tries to retrieve the quote using the alphaNumeric quoteId.
     *
     * @param alphaId Alphanumeric ID for the quote
     * @return If the quote exists the return value will be a quote object. Otherwise null.
     */
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException;

    /**
     * Method to return all sources of funding from the quote server
     * @return The set of funding objects
     *
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException;

    /**
     * Method to return a list of PriceItems per Platform.
     * @return The full price list
     *
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType) throws QuoteServerException, QuoteNotFoundException;


    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException;


    /**
     *  Method to return a list of all quotes
     */
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException;

}
