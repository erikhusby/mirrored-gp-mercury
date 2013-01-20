package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.NumberFormat;

@Named
@RequestScoped
public class ProductOrderUtil {

    @Inject
    private QuoteService quoteService;

    public String getFundsRemaining(ProductOrder productOrder) throws Exception {

        if (productOrder == null) {
            return "";
        }

        String quoteId = productOrder.getQuoteId();
        return getFundsRemaining(quoteId);
    }

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
