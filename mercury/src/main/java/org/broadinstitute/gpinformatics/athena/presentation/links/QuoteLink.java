package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteConfig;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

/**
 * This class is used to generate Quote server links for the UI.
 */
@Dependent
public class QuoteLink {
    private static final String QUOTE_DETAILS = "/quotes/quote/Quote.action?viewQuote=&quote.identifier=";

    @Inject
    private QuoteConfig quoteConfig;

    public String quoteUrl(String quoteId) {
        return quoteConfig.getUrl() + QUOTE_DETAILS + quoteId;
    }

    public String workUrl(String quoteId, String workId) {
        return quoteConfig.getUrl() + QUOTE_DETAILS + quoteId + "&selectWorkTab=true&workId=" + workId;
    }
}
