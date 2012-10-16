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
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    private String sampleIDs;

    int numDuplicateSamples;

    private String sampleStatus;

    private Quote quote;

    public String getSampleIDs() {
        return sampleIDs;
    }

    public void setSampleIDs(String sampleIDs) {
        this.sampleIDs = sampleIDs;
    }

    public String getSampleStatus() {
        return sampleStatus;
    }

    public String getFundsRemaining() {
        if (quote != null) {
            String fundsRemainingString = quote.getQuoteFunding().getFundsRemaining();
            try {
                double fundsRemaining = Double.parseDouble(fundsRemainingString);
                return NumberFormat.getCurrencyInstance().format(fundsRemaining);
            } catch (NumberFormatException e) {
                return fundsRemainingString;
            }
        }
        return "";
    }

    public void loadSampleStatus() {
        if (sampleIDs != null) {
            String[] samples = sampleIDs.split(",");
            if (samples.length == 1 && samples[0].isEmpty()) {
                // Handle empty string case.
                samples = new String[0];
            }
            Set<String> sampleSet = new HashSet<String>(samples.length);
            for (String sample : samples) {
                sampleSet.add(sample.trim().toUpperCase());
            }

            sampleStatus = MessageFormat.format("{0} Sample{0, choice, 0#s|1#|1<s}, {1} Duplicate{1, choice, 0#s|1#|1<s}",
                    samples.length, samples.length - sampleSet.size());
        }
    }

    /**
     * Load local state before bringing up the UI.
     */
    public void load() {
        productOrderDetail.load();
        if (sampleIDs == null && productOrderDetail != null) {
            StringBuilder sb = new StringBuilder();
            String separator = "";
            List<ProductOrderSample> samples = productOrderDetail.getProductOrder().getSamples();
            for (ProductOrderSample sample : samples) {
                sb.append(separator).append(sample.getSampleName());
                separator = ", ";
            }
            sampleIDs = sb.toString();
        }
        loadSampleStatus();
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
