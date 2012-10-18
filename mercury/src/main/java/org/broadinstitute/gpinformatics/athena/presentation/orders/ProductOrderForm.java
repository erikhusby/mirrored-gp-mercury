package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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

    /** Raw text of sample list to be edited. */
    @Nonnull
    private String sampleIDsText = "";

    // Cached state, visible but not editable.

    /** Processed list of sample names */
    private List<String> sampleIds = new ArrayList<String>();

    private String sampleStatus;

    private Quote quote;

    public static final String SEPARATOR = ",";
    /*
     * Split sample input on whitespace or commas. This treats multiple commas as a single comma.
     */
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[" + SEPARATOR + "\\s]+");

    @Nonnull
    public String getSampleIDsText() {
        return sampleIDsText;
    }

    public void setSampleIDsText(@Nullable String sampleIDsText) {
        if (sampleIDsText == null) {
            this.sampleIDsText = "";
        } else {
            this.sampleIDsText = sampleIDsText;
        }
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

    /**
     * Prepare to show sample edit dialog by converting current list of samples to a single string.
     */
    private void dialogPrepareContents() {
        if (productOrderDetail != null && productOrderDetail.getProductOrder() != null) {
            StringBuilder sb = new StringBuilder();
            String separator = "";
            List<ProductOrderSample> samples = productOrderDetail.getProductOrder().getSamples();
            for (ProductOrderSample sample : samples) {
                sb.append(separator).append(sample.getSampleName());
                separator = SEPARATOR + " ";
            }
            sampleIDsText = sb.toString();
        }
    }

    /**
     * Update sample edit dialog's status using the dialog's current text.
     */
    private void dialogUpdateStatus() {
        Set<String> sampleSet = new HashSet<String>(sampleIds);
        sampleStatus = MessageFormat.format(
                "{0} Sample{0, choice, 0#s|1#|1<s}, {1} Duplicate{1, choice, 0#s|1#|1<s}",
                sampleIds.size(), sampleIds.size() - sampleSet.size());
    }

    public void dialogSampleIdsChanged() {
        dialogProcessText();
        // Do this every time?
        sampleIDsText = StringUtils.join(sampleIds, SEPARATOR + " ");
        dialogUpdateStatus();
    }

    /**
     * Process the text in the dialog and convert to a list of sample names.
     */
    private void dialogProcessText() {
        String[] samples =  SPLIT_PATTERN.split(sampleIDsText, 0);
        if (samples.length == 1 && samples[0].isEmpty()) {
            // Handle empty string case.
            samples = new String[0];
        }
        sampleIds.clear();
        for (String sample : samples) {
            if (!StringUtils.isBlank(sample)) {
                // FIXME: should only uppercase BSP IDs?
                sampleIds.add(sample.trim().toUpperCase());
            }
        }
    }

    /**
     * Commit the sample dialog.  Convert the dialog's sample names into a list of sample objects, and
     * replace the sample objects in the current product order with the new list.
     */
    public void dialogCommit() {
        dialogProcessText();
        List<ProductOrderSample> orderSamples = new ArrayList<ProductOrderSample>(sampleIds.size());
        for (String sampleId : sampleIds) {
            orderSamples.add(new ProductOrderSample(sampleId));
        }
        productOrderDetail.getProductOrder().setSamples(orderSamples);
    }

    public void dialogPrepareToShow() {
        dialogPrepareContents();
        dialogUpdateStatus();
    }

    /**
     * Load local state before bringing up the UI.
     */
    public void load() {
        productOrderDetail.load();
    }

    public void loadFundsRemaining() {
        String quoteId = productOrderDetail.getProductOrder().getQuoteId();
        if (quoteId != null) {
            try {
                quote = quoteService.getQuoteFromQuoteServer(quoteId);
            } catch (Exception e) {
                String errorMessage = MessageFormat.format("The Quote ID ''{0}'' is invalid.", quoteId);
                addErrorMessage("quote", errorMessage, errorMessage + ": " + e);
            }
        }
    }

    public String save() {
        ProductOrder order = productOrderDetail.getProductOrder();
        String action = order.isInDB() ? "modified" : "created";
        productOrderDao.persist(order);
        addInfoMessage(MessageFormat.format("Product Order {0}.", action),
                MessageFormat.format("Product Order ''{0}'' has been {1}.", order.getTitle(), action));
        return redirect("list");
    }
}
