package org.broadinstitute.gpinformatics.athena.presentation;

import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteConfig;

import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * This is a bean to help the UI deal with Jira links
 */
@Named
@RequestScoped
public class QuoteLink {
    private static final String QUOTE_DETAILS = "/quotes/quote/Quote.action?viewQuote=&quote.identifier=";

    @Inject
    private QuoteConfig quoteConfig;

    public String quoteUrl(String quoteId) {
        return quoteConfig.getUrl() + QUOTE_DETAILS + quoteId;
    }
}
