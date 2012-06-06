package org.broadinstitute.pmbridge.infrastructure.quote;


import java.util.Set;

/**
 *
 * Service to talk to the quote server.
 */

public interface QuoteService {

    /**
     * Method tries to retrieve the quote using the alphaNumeric quoteId.
     *
     * @param alphaId Alphanumeric ID for the quote
     * @return If the quote exists the return value will be a quote object. Otherwise null.
     */
    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException;

    /**
     * Method tries to retrieve the quote using the alphaNumeric quoteId.
     *
     * @param numericId Numeric ID for the quote
     * @return If the quote exists the return value will be a quote object. Otherwise null.
     */
    public Quote getQuoteByNumericId(String numericId) throws QuoteServerException, QuoteNotFoundException;

    /**
     * Method to return all sources of funding from the quote server
     * @return
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException;

    /**
     * Method to return a list of Quotes for a particular funding source.
     * Any quotes that are returned from the quoteServer which have this funding source
     * associated will be returned.
     * @param fundingSource
     * @return
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public Set<Quote> getQuotesInFundingSource(Funding fundingSource) throws QuoteServerException, QuoteNotFoundException;

    /**
     * Method to return a list of PriceItems per Platform.
     * @return
     * @throws QuoteServerException
     * @throws QuoteNotFoundException
     */
    public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType) throws QuoteServerException, QuoteNotFoundException;


    /**
     *  Method to return a list of all quotes
     */
    public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException;

}
