package org.broadinstitute.gpinformatics.infrastructure.quote;

import net.sourceforge.stripes.util.CollectionUtil;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A simple cache of quotes extracted
 * from the quote server, which enables
 * finding quotes for particular funding
 * sources.
 */
public class QuotesCache {
    
    private final Quotes quotes;

    /**
     * New one up, using the #quotes 
     * as the backing store.  This is because
     * we assume that #quotes is going
     * to come from {@link QuoteServiceImpl#getAllSequencingPlatformQuotes()}
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
    public Collection<Funding> getAllFundingSources() {
        Set<Funding> fundingSources = new HashSet<>();
        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (CollectionUtils.isNotEmpty(quote.getQuoteFunding().getFundingLevel())) {
                    for (FundingLevel level : quote.getQuoteFunding().getFundingLevel()) {
                        if (CollectionUtils.isNotEmpty(level.getFunding())) {
                            for (Funding funding:level.getFunding()) {
                                fundingSources.add(funding);
                            }
                        }
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
        Set<Quote> quotesForFundingSource = new HashSet<>();
        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (CollectionUtils.isNotEmpty(quote.getQuoteFunding().getFundingLevel())) {
                    for(FundingLevel level : quote.getQuoteFunding().getFundingLevel()) {
                        if (CollectionUtils.isNotEmpty(level.getFunding() )) {
                            for (Funding fundingForQuote:level.getFunding()) {
                                if (fundingForQuote.getGrantDescription() != null) {
                                    if (grantDescription.equalsIgnoreCase(fundingForQuote.getGrantDescription())) {
                                        quotesForFundingSource.add(quote);
                                    }
                                }
                                break;
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
        Map<Funding, HashSet<Quote>> quotesByFundingSource = new HashMap<>();

        for (Quote quote : quotes.getQuotes()) {
            if (quote.getQuoteFunding() != null) {
                if (CollectionUtils.isNotEmpty(quote.getQuoteFunding().getFundingLevel())) {
                    for (FundingLevel level : quote.getQuoteFunding().getFundingLevel()) {
                        if (CollectionUtils.isNotEmpty(level.getFunding())) {

                            for (Funding funding :level.getFunding()) {
                                if ((funding.getGrantDescription() != null) && (funding.getGrantNumber() != null)) {
                                    // get/create the set of quotes
                                    HashSet<Quote> quotesSet=quotesByFundingSource.get(funding);
                                    if  (quotesSet== null) {
                                        quotesSet = new HashSet<>();
                                        quotesByFundingSource.put(funding, quotesSet);
                                    }
                                    quotesSet.add(quote);
                                }
                                break;
                            }
                        }
                    }
                }
            }
        }

        return quotesByFundingSource;

    }
}
