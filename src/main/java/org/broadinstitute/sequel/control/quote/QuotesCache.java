package org.broadinstitute.sequel.control.quote;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A simple cache of quotes extracted
 * from the quote server, which enables
 * finding quotes for particular funding
 * sources.
 */
public class QuotesCache {
    
    private final Quotes quotes;
    
    public QuotesCache(Quotes quotes) {
        if (quotes == null) {
            throw new NullPointerException("Quotes cannot be null");
        }
        this.quotes = quotes;
    }
    
    public Collection<Quote> getQuotes() {
        return this.quotes.getQuotes();
    }
    
    public Collection<Funding> getAllFundingSources() {
        Set<Funding> fundingSources = new HashSet<Funding>();
        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (quote.getQuoteFunding().getFundingLevel() != null) {
                    if (quote.getQuoteFunding().getFundingLevel().getFunding() != null) {
                        fundingSources.add(quote.getQuoteFunding().getFundingLevel().getFunding());
                    }
                }
            }
        }
        return fundingSources;
    }
    
    public Collection<Quote> getQuotesForGrantDescription(String grantDescription) {
        if (grantDescription == null) {
            throw new NullPointerException("grantDescription cannot be null.");
        }
        Set<Quote> quotesForFundingSource = new HashSet<Quote>();
        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (quote.getQuoteFunding().getFundingLevel() != null) {
                    if (quote.getQuoteFunding().getFundingLevel().getFunding() != null) {
                        Funding fundingForQuote = quote.getQuoteFunding().getFundingLevel().getFunding();
                        if (fundingForQuote.getGrantDescription() != null) {
                            if (grantDescription.equalsIgnoreCase(fundingForQuote.getGrantDescription())) {
                                quotesForFundingSource.add(quote);
                            }
                        }
                    }
                }
            }
        }
        return quotesForFundingSource;
    }
}
