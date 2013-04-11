package org.broadinstitute.gpinformatics.infrastructure.quote;

import java.io.Serializable;
import java.util.Date;

/**
 *
 * Service to talk to the quote server.
 */
public interface QuoteService extends Serializable {
    // todo extract and marshall current quote server xml

    // todo rename impl and bsp impl to "LiveBSP" and "LiveQuote", same for jira

    // todo make registerNewWork mock

    public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException;

    public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException;

    public String registerNewWork(Quote quote,
                                  PriceItem priceItem,
                                  PriceItem itemIsReplacing,
                                  Date reportedCompletionDate,
                                  double numWorkUnits,
                                  String callbackUrl,
                                  String callbackParameterName,
                                  String callbackParameterValue);

    public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException;
}