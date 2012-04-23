package org.broadinstitute.pmbridge.infrastructure.quote;


/**
 *
 * Service to talk to the quote server.
 */

public interface QuoteService {

    /**
     * Asks the quote server for basic information about a quote.
     *
     * @param id Alphanumeric ID for the quote
     * @return If the quote exists the return value will be a quote object. Otherwise null.
     */

    Quote getQuoteFromQuoteServer(String id) throws QuoteServerException, QuoteNotFoundException;


}
