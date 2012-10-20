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
import java.util.*;
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
    // FIXME: use null to indicate no change, vs removing all samples on PDO.
    @Nonnull
    private String sampleIDsText = "";

    // Cached state, visible but not editable.

    private String sampleStatus;

    private static final String SEPARATOR = ",";

    /*
     * Split sample input on whitespace or commas. This treats multiple commas as a single comma.
     */
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[" + SEPARATOR + "\\s]+");

    @Nonnull
    public String getSampleIDsText() {
        if (sampleIDsText.isEmpty()) {
            dialogPrepareToShow();
        }
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
        String quoteId = productOrderDetail.getProductOrder().getQuoteId();
        if (!StringUtils.isBlank(quoteId)) {
            try {
                Quote quote = quoteService.getQuoteFromQuoteServer(quoteId);
                String fundsRemainingString = quote.getQuoteFunding().getFundsRemaining();
                try {
                    double fundsRemaining = Double.parseDouble(fundsRemainingString);
                    return NumberFormat.getCurrencyInstance().format(fundsRemaining);
                } catch (NumberFormatException e) {
                    return fundsRemainingString;
                }
            } catch (Exception e) {
                String errorMessage = MessageFormat.format("The Quote ID ''{0}'' is invalid.", quoteId);
                addErrorMessage("quote", errorMessage, errorMessage + ": " + e);
            }
        }
        return "";
    }

    /**
     * Prepare to show sample edit dialog by converting current list of samples to a single string.
     */
    private List<String> dialogConvertOrderSamplesToList() {
        if (productOrderDetail == null || productOrderDetail.getProductOrder() == null) {
            return Collections.emptyList();
        }
        List<ProductOrderSample> samples = productOrderDetail.getProductOrder().getSamples();
        List<String> sampleIds = new ArrayList<String>(samples.size());
        for (ProductOrderSample sample : samples) {
            sampleIds.add(sample.getSampleName());
        }
        return sampleIds;
    }

    /**
     * Update sample edit dialog's status using the dialog's current text.
     */
    private void dialogUpdateStatus(List<String> sampleIds) {
        Set<String> sampleSet = new HashSet<String>(sampleIds);
        sampleStatus = MessageFormat.format(
                "{0} Sample{0, choice, 0#s|1#|1<s}, {1} Duplicate{1, choice, 0#s|1#|1<s}",
                sampleIds.size(), sampleIds.size() - sampleSet.size());
    }

    private void dialogUpdateSampleText(List<String> sampleIds) {
        sampleIDsText = StringUtils.join(sampleIds, SEPARATOR + " ");
        dialogUpdateStatus(sampleIds);
    }

    public void dialogSampleTextChanged() {
        dialogUpdateSampleText(dialogConvertTextToList());
    }

    /**
     * Process the text in the dialog and convert to a list of sample names.
     */
    private List<String> dialogConvertTextToList() {
        String[] samples =  SPLIT_PATTERN.split(sampleIDsText, 0);
        if (samples.length == 1 && samples[0].isEmpty()) {
            // Handle empty string case.
            samples = new String[0];
        }
        List<String> sampleIds = new ArrayList<String>(samples.length);
        for (String sample : samples) {
            if (!StringUtils.isBlank(sample)) {
                // FIXME: should only uppercase BSP IDs?
                sampleIds.add(sample.trim().toUpperCase());
            }
        }
        return sampleIds;
    }

    public void dialogCancel() {
        sampleIDsText = "";
    }

    /**
     * Commit the sample dialog.  Convert the dialog's sample names into a list of sample objects, and
     * replace the sample objects in the current product order with the new list.
     */
    public List<ProductOrderSample> dialogConvertTextToOrderSamples() {
        List<String> sampleIds = dialogConvertTextToList();
        List<ProductOrderSample> orderSamples = new ArrayList<ProductOrderSample>(sampleIds.size());
        for (String sampleId : sampleIds) {
            orderSamples.add(new ProductOrderSample(sampleId));
        }
        return orderSamples;
    }

    public void dialogPrepareToShow() {
        dialogUpdateSampleText(dialogConvertOrderSamplesToList());
    }

    /**
     * Load local state before bringing up the UI.
     */
    public void load() {
        // If present, copy sample ID list into Product Order object.
        if (!StringUtils.isBlank(sampleIDsText)) {
            productOrderDetail.getProductOrder().setSamples(dialogConvertTextToOrderSamples());
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
