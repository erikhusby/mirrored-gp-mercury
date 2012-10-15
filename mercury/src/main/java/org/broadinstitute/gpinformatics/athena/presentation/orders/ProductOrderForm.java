package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;

/**
 * Class for creating a product order, or editing a draft product order.
 */
@Named
@RequestScoped
public class ProductOrderForm extends AbstractJsfBean {
    @Inject
    ProductOrderDetail productOrderDetail;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    private QuoteService quoteService;

    // Add state that can be edited here.

    private String sampleIDs = "";

    private Quote quote;

    public String getSampleIDs() {
        return sampleIDs;
    }

    public void setSampleIDs(String sampleIDs) {
        this.sampleIDs = sampleIDs;
    }

    public String getFundsRemaining() {
        if (quote != null) {
            return quote.getQuoteFunding().getFundsRemaining();
        }
        return "";
    }

    /**
     * Load local state before bringing up the UI.
     */
    public void load() {
        productOrderDetail.load();
        if (sampleIDs == null && productOrderDetail != null) {
            StringBuilder sb = new StringBuilder();
            String separator = "";
            for (ProductOrderSample sample : productOrderDetail.getProductOrder().getSamples()) {
                sb.append(separator).append(sample.getSampleName());
                separator = ", ";
            }

            sampleIDs = sb.toString();
        }
    }

    public void loadFundsRemaining() {
        String quoteId = productOrderDetail.getProductOrder().getQuoteId();
        String errorMessage = MessageFormat.format("The Quote ID ''{0}'' is invalid.", quoteId);
        if (quoteId != null) {
            try {
                quote = quoteService.getQuoteFromQuoteServer(quoteId);
            } catch (QuoteServerException e) {
                addErrorMessage("quote", errorMessage, errorMessage + ": " + e);
            } catch (QuoteNotFoundException e) {
                addErrorMessage("quote", errorMessage, errorMessage + ": " + e);
            }
        }
    }
}
