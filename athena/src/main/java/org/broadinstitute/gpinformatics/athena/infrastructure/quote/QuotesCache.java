package org.broadinstitute.gpinformatics.athena.infrastructure.quote;

import java.util.*;

/**
 * A simple cache of quotes extracted
 * from the quote server, which enables
 * finding quotes for particular funding
 * sources.
 * @author From SequeL
 */
public class QuotesCache {
    
    private final Quotes quotes;

    /**
     * New one up, using the #quotes 
     * as the backing store.  This is because
     * we assume that #quotes is going
     * to come from {@link org.broadinstitute.gpinformatics.athena.infrastructure.quote.QuoteServiceImpl#getAllQuotes()}
     * @param quotes
     */
    public QuotesCache(Quotes quotes) {
        if (quotes == null) {
            throw new NullPointerException("Quotes cannot be null");
        }
        this.quotes = quotes;
    }



    /**
     * Returns the underlying
     * list of {@link Quote}s
     * @return
     */
    public Collection<Quote> getQuotes() {
        return this.quotes.getQuotes();
    }

    /**
     * Returns the unique set of
     * {@link Funding} objects from
     * the cache.
     * @return
     */
    public Set<Funding> getAllFundingSources() {
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

    /**
     * Finds all {@link Quote} objects that reference
     * the given #grantDescription as {@link Funding#grantDescription}.
     * @param grantDescription
     * @return
     */
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



    public Map<Funding, HashSet<Quote>> getQuotesByFundingSource(){

//        Collection<Funding> fundingSources = getAllFundingSources();
        Map<Funding, HashSet<Quote>> quotesByFundingSource = new HashMap<Funding, HashSet<Quote>>();

        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (quote.getQuoteFunding().getFundingLevel() != null) {
                    if (quote.getQuoteFunding().getFundingLevel().getFunding() != null) {
                        Funding funding = quote.getQuoteFunding().getFundingLevel().getFunding();
                        if ((funding.getGrantDescription() != null) && (funding.getGrantNumber() != null)) {
                            // get/create the set of quotes
                            HashSet<Quote> quotesSet=quotesByFundingSource.get(funding);
                            if  (quotesSet== null) {
                                quotesSet = new HashSet<Quote>();
                                quotesByFundingSource.put(funding, quotesSet);
                            }
                            quotesSet.add(quote);
                        }
                    }
                }
            }
        }


        return quotesByFundingSource;

    }
}
