package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.text.NumberFormat;

@RequestScoped
public class ProductOrderUtil {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private QuoteService quoteService;

    public String getFundsRemaining(String quoteId) throws Exception {
        String fundsRemainingString;
        if (!StringUtils.isBlank(quoteId)) {
            Quote quote = quoteService.getQuoteByAlphaId(quoteId);
            fundsRemainingString = quote.getQuoteFunding().getFundsRemaining();
            double fundsRemaining = Double.parseDouble(fundsRemainingString);
            return NumberFormat.getCurrencyInstance().format(fundsRemaining);
        }

        return "";
    }

}
