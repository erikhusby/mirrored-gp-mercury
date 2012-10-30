package org.broadinstitute.gpinformatics.athena.presentation.orders;

import org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;
import org.primefaces.event.SelectEvent;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
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
    ProductDao productDao;

    @Inject
    private QuoteService quoteService;

    @Inject
    private FacesContext facesContext;

    @Inject
    private ProductOrderConversationData conversationData;

    private List<String> selectedAddOns = new ArrayList<String>();

    /**
     * This is required to get the editIdsCache value in the case where we want to skip the process
     * validations lifecycle.  This is to avoid validating components when we bring up the edit samples dialog.
     */
    private UIInput editIdsCacheBinding;

    // Add state that can be edited here.

    private static final String SEPARATOR = ",";

    private final SamplesDialog samplesDialog = new SamplesDialog();

    /*
     * Split sample input on whitespace or commas. This treats multiple commas as a single comma.
     */
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[" + SEPARATOR + "\\s]+");

    /** Automatically convert known BSP IDs (SM-, SP-) to uppercase. */
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[sS][mMpP]-.*");

    private final List<String> sampleValidationMessages = new ArrayList<String>();

    public UIInput getEditIdsCacheBinding() {
        return editIdsCacheBinding;
    }

    public void setEditIdsCacheBinding(UIInput editIdsCacheBinding) {
        this.editIdsCacheBinding = editIdsCacheBinding;
    }

    private String getEditIdsCache() {
        return String.valueOf(editIdsCacheBinding.getValue());
    }

    private void setEditIdsCache(String s) {
        editIdsCacheBinding.setValue(s);
    }

    public SamplesDialog getSamplesDialog() {
        return samplesDialog;
    }

    public List<String> getSampleValidationMessages() {
        return sampleValidationMessages;
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
     * Convert current list of samples to a single string.
     */
    private List<String> convertOrderSamplesToList() {
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
     * Process the text in the dialog and convert to a list of sample names.
     */
    private static List<String> convertTextToList(String text) {
        if (text == null) {
            return Collections.emptyList();
        }
        String[] samples =  SPLIT_PATTERN.split(text, 0);
        if (samples.length == 1 && samples[0].isEmpty()) {
            // Handle empty string case.
            samples = new String[0];
        }
        List<String> sampleIds = new ArrayList<String>(samples.length);
        for (String sample : samples) {
            if (!StringUtils.isBlank(sample)) {
                sample = sample.trim();
                if (UPPERCASE_PATTERN.matcher(sample).matches()) {
                    sample = sample.toUpperCase();
                }
                sampleIds.add(sample);
            }
        }
        return sampleIds;
    }

    /**
     * Convert the sample names into a list of sample objects, and replace the sample objects in the current product
     * order with the new list.
     */
    public List<ProductOrderSample> convertTextToOrderSamples(@Nonnull String text) {
        List<String> sampleIds = convertTextToList(text);
        List<ProductOrderSample> orderSamples = new ArrayList<ProductOrderSample>(sampleIds.size());
        for (String sampleId : sampleIds) {
            orderSamples.add(new ProductOrderSample(sampleId, productOrderDetail.getProductOrder()));
        }
        return orderSamples;
    }

    public List<String> getSelectedAddOns() {
        return selectedAddOns;
    }

    public void setSelectedAddOns(@Nonnull List<String> selectedAddOns) {
        this.selectedAddOns = selectedAddOns;
    }

    public List<String> getAddOns() {
        return conversationData.getAddOnsForProduct();
    }

    public void setAddOns(@Nonnull List<String> addOns) {
        conversationData.setAddOnsForProduct(addOns);
    }

    /**
     * Set up the add ons when the product selection event happens
     *
     * @param productSelectEvent The selection event on the project
     */
    public void setupAddOns(SelectEvent productSelectEvent) {
        Product product = (Product) productSelectEvent.getObject();
        setupAddOns(product);
    }

    /**
     * Get all the add on products for the specified product
     *
     * @param product The product
     */
    public void setupAddOns(Product product) {
        conversationData.setAddOnsForProduct(new ArrayList<String>());
        if (product != null) {
            for (Product productAddOn : product.getAddOns()) {
                conversationData.getAddOnsForProduct().add(productAddOn.getProductName());
            }
        }
    }

    /**
     * Class that contains operations specific to the sample list samplesDialog.
     *
     * The edited sample list has three states:
     * 1 the database state, read from the current product order, used to display table
     * 2 the committed edited state, not shown to user
     * 3 the uncommitted edited state, used in samplesDialog
     *
     * Here are the operations we need to support, and the data flow for each.
     * - first ever page load: 1 => 2; subsequent page loads 2 => 1
     * - save to database: 2 => 1 (automatic due to page load)
     * - show samplesDialog: 2 => 3
     * - confirm samplesDialog: 3 => 2
     * - the user can make changes to (3) by typing in the samplesDialog.
     * - both (2) and (3) are stored in the JSF form data. (1) is stored in the database.
     */
    public class SamplesDialog {
        private String text = "";

        private String status;

        public String getStatus() {
            return status;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public void textChanged() {
            List<String> sampleIds = convertTextToList(text);
            text = StringUtils.join(sampleIds, SEPARATOR + " ");
            Set<String> sampleSet = new HashSet<String>(sampleIds);
            status = MessageFormat.format(
                    "{0} Sample{0, choice, 0#s|1#|1<s}, {1} Duplicate{1, choice, 0#s|1#|1<s}",
                    sampleIds.size(), sampleIds.size() - sampleSet.size());
        }

        public void commit() {
            // 3 => 2
            setEditIdsCache(text);
        }

        public void prepareToShow() {
            // 2 => 3
            text = getEditIdsCache();
        }
    }

    private static void checkCount(int count, String message, List<String> output) {
        if (count != 0) {
            output.add(MessageFormat.format(message, count));
        }
    }

    /**
     * Check to see if the samples are valid.
     * - for all BSP formatted sample IDs, do we have BSP data?
     * - all BSP samples are RECEIVED
     * - all BSP samples' stock is ACTIVE
     */
    private void validateSamples() {
        sampleValidationMessages.clear();
        ProductOrder order = productOrderDetail.getProductOrder();
        checkCount(order.getMissingBspMetaDataCount(), "Samples Missing BSP Data: {0}", sampleValidationMessages);
        checkCount(order.getBspSampleCount() - order.getActiveSampleCount(), "Samples Not ACTIVE: {0}", sampleValidationMessages);
        checkCount(order.getBspSampleCount() - order.getReceivedSampleCount(), "Samples Not RECEIVED: {0}", sampleValidationMessages);
    }

    /**
     * Load local state before rendering the sample table.
     */
    public void load() {
        if (facesContext.isPostback()) {
            // Restoring the view, replace entity state with form state.
            // 2 => 1
            productOrderDetail.getProductOrder().setSamples(convertTextToOrderSamples(getEditIdsCache()));
        } else {
            // First time, load from entity state.
            // 1 => 2
            setEditIdsCache(StringUtils.join(convertOrderSamplesToList(), SEPARATOR + " "));
        }

        productOrderDetail.load();
        validateSamples();
    }

    // FIXME: handle db store errors, JIRA server errors.
    public String save() throws IOException {
        ProductOrder order = productOrderDetail.getProductOrder();
        order.setSamples(convertTextToOrderSamples(getEditIdsCache()));

        // Validations.
        if (order.getSamples().isEmpty()) {
            // FIXME: instead of doing this here, it can be done as a validator on the hidden editIDsCache field.
            String message = "You must add at least one sample before placing an order.";
            addErrorMessage(message, message);
            return null;
        }

        order.updateAddOnProducts(getSelectedAddOnProducts());

        // DRAFT orders not yet supported; force state of new PDOs to Submitted.
        order.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        String action = order.isInDB() ? "modified" : "created";
        order.submitProductOrder();
        productOrderDao.persist(order);

        addFlashMessage(
            MessageFormat.format("Product Order ''{0}'' ({1}) has been {2}.",
            order.getTitle(), order.getJiraTicketKey(), action));
        return redirect("view");
    }

    private List<Product> getSelectedAddOnProducts() {
        if (getSelectedAddOns().isEmpty()) {
            return new ArrayList<Product> ();
        }

        return productDao.findListByList(Product.class, Product_.productName, getSelectedAddOns());
    }

    public void initForm() {
        conversationData.beginConversation(productOrderDetail.getProductOrder());
    }
}
