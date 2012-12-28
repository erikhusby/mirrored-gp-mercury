package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
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

    public String getFundsRemaining(ProductOrder productOrder) throws QuoteNotFoundException {

        if (productOrder == null) {
            return "";
        }

        String quoteId = productOrder.getQuoteId();
        String fundsRemainingString;
        if ( ! StringUtils.isBlank(quoteId)) {
            try {

                Quote quote = quoteService.getQuoteByAlphaId(quoteId);
                fundsRemainingString = quote.getQuoteFunding().getFundsRemaining();
            }
            catch (QuoteServerException e) {
                throw new RuntimeException(e);
            }

            try {
                double fundsRemaining = Double.parseDouble(fundsRemainingString);
                return NumberFormat.getCurrencyInstance().format(fundsRemaining);
            } catch (NumberFormatException e) {
                return fundsRemainingString;
            }
        }

        return "";
    }

}
