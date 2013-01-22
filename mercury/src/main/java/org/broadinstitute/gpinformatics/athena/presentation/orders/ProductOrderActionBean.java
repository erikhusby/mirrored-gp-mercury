package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.links.BspLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.text.MessageFormat;
import java.util.*;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding(ProductOrderActionBean.ACTIONBEAN_URL_BINDING)
public class ProductOrderActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/orders/order.action";
    public static final String PRODUCT_ORDER_PARAMETER = "productOrder";

    private static final String CURRENT_OBJECT = "Product Order";
    public static final String CREATE_ORDER = CoreActionBean.CREATE + CURRENT_OBJECT;
    public static final String EDIT_ORDER = CoreActionBean.EDIT + CURRENT_OBJECT;

    private static final String ORDER_CREATE_PAGE = "/orders/create.jsp";
    private static final String ORDER_LIST_PAGE = "/orders/list.jsp";
    private static final String ORDER_VIEW_PAGE = "/orders/view.jsp";

    @Inject
    private QuoteService quoteService;

    @Inject
    private ProductOrderUtil productOrderUtil;

    @Inject
    private ProductOrderSampleDao sampleDao;

    @Inject
    private ProductOrderListEntryDao orderListEntryDao;

    @Inject
    private JiraLink jiraLink;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private BspLink bspLink;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao projectDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingLedgerDao billingLedgerDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private UserBean userBean;

    @Inject
    private ProductTokenInput productTokenInput;

    @Inject
    private ProjectTokenInput projectTokenInput;

    @Inject
    private JiraService jiraService;

    private List<ProductOrderListEntry> allProductOrders;

    private String sampleList;

    @Validate(required = true, on = {VIEW_ACTION, EDIT_ACTION})
    private String productOrder;

    private long sampleId;

    @ValidateNestedProperties({
        @Validate(field="comments", maxlength=2000, on={SAVE_ACTION}),
        @Validate(field="title", required = true, maxlength=255, on={SAVE_ACTION}, label = "Name"),
        @Validate(field="count", on={SAVE_ACTION}, label="Number of Lanes")
    })
    private ProductOrder editOrder;

    // For create, we can also have a research project key to default
    private String researchProjectKey;

    private List<String> selectedProductOrderBusinessKeys;
    private List<ProductOrder> selectedProductOrders;

    private String quoteIdentifier;

    private String product;

    private List<String> addOnKeys = new ArrayList<String> ();

    /*
     * The search query.
     */
    private String q;

    /** The owner of the Product Order, stored as createdBy in ProductOrder and Reporter in JIRA */
    @Inject
    private UserTokenInput owner;

    /**
     * Initialize the product with the passed in key for display in the form or create it, if not specified
     */
    @Before(stages = LifecycleStage.BindingAndValidation)
    public void init() {
        productOrder = getContext().getRequest().getParameter(PRODUCT_ORDER_PARAMETER);
        if (!StringUtils.isBlank(productOrder)) {
            editOrder = productOrderDao.findByBusinessKey(productOrder);
        } else {
            // If this was a create with research project specified, find that.
            // This is only used for save, when creating a new product order.
            editOrder = new ProductOrder();
        }
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void saveValidations() throws Exception {
        // If the research project has no original title, then it was not fetched from hibernate, so this is a create
        // OR if this was fetched and the title has been changed
        if ((editOrder.getOriginalTitle() == null) ||
                (!editOrder.getTitle().equalsIgnoreCase(editOrder.getOriginalTitle()))) {

            // Check if there is an existing research project and error out if it already exists
            ProductOrder existingOrder = productOrderDao.findByTitle(editOrder.getTitle());
            if (existingOrder != null) {
                addValidationError("title", "A product order already exists with this name.");
            }
        }

        // Whether we are draft or not, we should populate the proper edit fields for validation.
        updateTokenInputFields();

        // If this is not a draft, some fields are required
        if (!editOrder.isDraft()) {
            doValidation("save");
        } else {
            // Even in draft, created by must be set. This can't be checked using @Validate (yet),
            // since its value isn't set until updateTokenInputFields() has been called.
            requireField(editOrder.getCreatedBy(), "an owner", "save");
        }
    }

    /**
     * Validate a required field
     * @param value if null, field is missing
     * @param name name of field
     */
    private void requireField(Object value, String name, String action) {
        requireField(value != null, name, action);
    }

    /**
     * Validate a required field
     * @param hasValue if false, field is missing
     * @param name name of field
     */
    private void requireField(boolean hasValue, String name, String action) {
        if (!hasValue) {
            addGlobalValidationError("Cannot {2} ''{3}'' because it does not have {4}.",
                    action, editOrder.getBusinessKey(), name);
        }
    }

    private void doValidation(String action) {
        String ownerUsername = bspUserList.getById(editOrder.getCreatedBy()).getUsername();
        requireField(jiraService.isValidUser(ownerUsername), "an owner with a JIRA account", action);
        requireField(!editOrder.getSamples().isEmpty(), "any samples", action);
        requireField(editOrder.getResearchProject(), "a research project", action);
        requireField(editOrder.getQuoteId(), "a quote specified", action);
        requireField(editOrder.getProduct(), "a product", action);
        requireField(editOrder.getCount() > 0, "a specified number of lanes", action);
        requireField(editOrder.getCreatedBy(), "an owner", action);

        try {
            quoteService.getQuoteByAlphaId(editOrder.getQuoteId());
        } catch (QuoteServerException ex) {
            addGlobalValidationError("The quote id " + editOrder.getQuoteId() + " is not valid: " + ex.getMessage());
        } catch (QuoteNotFoundException ex) {
            addGlobalValidationError("The quote id " + editOrder.getQuoteId() + " was not found.");
        }
    }

    @ValidationMethod(on = "placeOrder")
    public void validatePlacedOrder() {
        doValidation("place order");
    }

    @ValidationMethod(on = {"startBilling", "downloadBillingTracker"})
    public void validateOrderSelection() {
        String validatingFor = getContext().getEventName().equals("startBilling") ? "billing session" :
                "tracker download";

        if (!getUserBean().isValidBspUser()) {
            addGlobalValidationError("A valid bsp user is needed to start a " + validatingFor);
        }

        if ((selectedProductOrderBusinessKeys == null) || selectedProductOrderBusinessKeys.isEmpty()) {
            addGlobalValidationError("You must select at least one product order to start a " + validatingFor);
        } else {
            Set<Product> products = new HashSet<Product> ();
            selectedProductOrders =
                productOrderDao.findListByBusinessKeyList(
                    selectedProductOrderBusinessKeys,
                    ProductOrderDao.FetchSpec.Product,
                    ProductOrderDao.FetchSpec.ResearchProject,
                    ProductOrderDao.FetchSpec.Samples);

            for (ProductOrder order : selectedProductOrders) {
                products.add(order.getProduct());
            }

            // Go through each products and report invalid duplicate price item names
            for (Product product : products) {
                String[] duplicatePriceItems = product.getDuplicatePriceItemNames();
                if (duplicatePriceItems != null) {
                    addGlobalValidationError(
                            "The Product " + product.getPartNumber() + " has duplicate price items: " +
                            StringUtils.join(duplicatePriceItems, ", "));
                }
            }

            // If there are locked out orders, then do not allow the session to start.
            // TODO: It looks like this could be done by traversing the entity tree, e.g. ProductOrder -> ProductOrderSample -> BusinessLedger.
            Set<BillingLedger> lockedOutOrders = billingLedgerDao.findLockedOutByOrderList(getSelectedProductOrderBusinessKeys());
            if (!lockedOutOrders.isEmpty()) {
                Set<String> lockedOutOrderStrings = new HashSet<String>(lockedOutOrders.size());
                for (BillingLedger ledger : lockedOutOrders) {
                    lockedOutOrderStrings.add(ledger.getProductOrderSample().getProductOrder().getTitle());
                }

                String lockedOutString = StringUtils.join(lockedOutOrderStrings, ", ");

                addGlobalValidationError(
                        "The following orders are locked out by active billing sessions: " + lockedOutString);
            }
        }

        // If there are errors, will reload the page, so need to fetch the list
        if (hasErrors()) {
            listInit();
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {LIST_ACTION})
    public void listInit() {
        allProductOrders = orderListEntryDao.findProductOrderListEntries();
    }

    private void validateUser(String validatingFor) {
        if (!userBean.ensureUserValid()) {
            addGlobalValidationError(MessageFormat.format(UserBean.LOGIN_WARNING, validatingFor + " an order"));
        }
    }

    @HandlesEvent("getQuoteFunding")
    public Resolution getQuoteFunding() {

        StringReader returnStringStream;

        JSONObject item = new JSONObject();

        try {
            item.put("key", quoteIdentifier);
            if (quoteIdentifier != null) {
                String fundsRemaining = productOrderUtil.getFundsRemaining(quoteIdentifier);
                item.put("fundsRemaining", fundsRemaining);
            }

        } catch (Exception ex) {
            try {
                item.put("error", ex.getMessage());
            } catch (Exception ex1) {
                // Don't really care if this gets an exception
            }
        }

        return createTextResolution(item.toString());
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(ORDER_LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (editOrder.isDraft()) {
            validateUser("place");
        }
        return new ForwardResolution(ORDER_VIEW_PAGE);
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution create() {
        setSubmitString(CREATE_ORDER);
        populateTokenListsFromObjectData();
        owner.setup(userBean.getBspUser().getUserId());
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution edit() {
        validateUser(EDIT_ACTION);
        setSubmitString(EDIT_ORDER);
        populateTokenListsFromObjectData();
        owner.setup(editOrder.getCreatedBy());
        return new ForwardResolution(ORDER_CREATE_PAGE);
    }

    /**
     * For the prepopulate to work on opening create and edit page, we need to take values from the editOrder. After,
     * the pages have the values passed in.
     */
    private void populateTokenListsFromObjectData() {
        String[] productKey = (editOrder.getProduct() == null) ? new String[0] : new String[] { editOrder.getProduct().getBusinessKey() };
        productTokenInput.setup(productKey);

        // If a research project key was specified then use that as the default
        String[] projectKey;
        if (!StringUtils.isBlank(researchProjectKey)) {
            projectKey = new String[] { researchProjectKey };
        } else {
            projectKey = (editOrder.getResearchProject() == null) ? new String[0] : new String[] { editOrder.getResearchProject().getBusinessKey() };
        }

        projectTokenInput.setup(projectKey);
    }

    @HandlesEvent("placeOrder")
    public Resolution placeOrder() {
        try {
            editOrder.prepareToSave(userBean.getBspUser(), isCreating());
            editOrder.submitProductOrder();
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

            // save it!
            productOrderDao.persist(editOrder);
        } catch (Exception e) {
            // Need to quote the message contents to prevent errors.
            addLiteralErrorMessage(e.getMessage());
            return getSourcePageResolution();
        }

        addMessage("Product Order \"" + editOrder.getTitle() + "\" has been placed");
        return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(PRODUCT_ORDER_PARAMETER, editOrder.getBusinessKey());
    }

    @HandlesEvent("validate")
    public Resolution validate() {
        validatePlacedOrder();

        if (getContext().getValidationErrors().isEmpty()) {
            addMessage("Draft Order is valid and ready to be placed");
        }

        return getSourcePageResolution();
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() throws Exception {

        // Update the modified by and created by, if necessary.
        editOrder.prepareToSave(userBean.getBspUser(), isCreating());

        if (editOrder.isDraft()) {
            // mlc isDraft checks if the status is Draft and if so, we set it to Draft again?
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);
        } else {
            productOrderEjb.updateJiraIssue(editOrder);
        }

        // save it!
        productOrderDao.persist(editOrder);

        addMessage("Product Order \"" + editOrder.getTitle() + "\" has been saved.");
        return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(PRODUCT_ORDER_PARAMETER, editOrder.getBusinessKey());
    }

    private void updateTokenInputFields() {
        // set the project, product and addOns for the order
        ResearchProject project = projectDao.findByBusinessKey(projectTokenInput.getTokenObject());
        Product product = productDao.findByPartNumber(productTokenInput.getTokenObject());
        List<Product> addOnProducts = productDao.findByPartNumbers(addOnKeys);
        editOrder.updateData(project, product, addOnProducts, getSamplesAsList());
        List<BspUser> ownerList = owner.getTokenObjects();
        if (ownerList.isEmpty()) {
            editOrder.setCreatedBy(null);
        } else {
            editOrder.setCreatedBy(ownerList.get(0).getUserId());
        }
    }

    @HandlesEvent("downloadBillingTracker")
    public Resolution downloadBillingTracker() {
        Resolution resolution = ProductOrderActionBean.getTrackerForOrders(this, selectedProductOrders, bspUserList);
        if (hasErrors()) {
            // Need to regenerate the list so it's displayed along with the errors.
            listInit();
        }
        return resolution;
    }

    @HandlesEvent("startBilling")
    public Resolution startBilling() {
        Set<BillingLedger> ledgerItems =
                billingLedgerDao.findWithoutBillingSessionByOrderList(getSelectedProductOrderBusinessKeys());
        if ((ledgerItems == null) || (ledgerItems.isEmpty())) {
            addGlobalValidationError("There is nothing to bill");
            return new RedirectResolution(ProductOrderActionBean.class, LIST_ACTION);
        }

        BillingSession session = new BillingSession(getUserBean().getBspUser().getUserId(), ledgerItems);
        billingSessionDao.persist(session);

        return new RedirectResolution(BillingSessionActionBean.class, VIEW_ACTION)
                .addParameter("sessionKey", session.getBusinessKey());
    }

    @HandlesEvent("getAddOns")
    public Resolution getAddOns() throws Exception {
        JSONArray itemList = new JSONArray();

        if (product != null) {
            Product product = productDao.findByBusinessKey(this.product);
            for (Product addOn : product.getAddOns()) {
                JSONObject item = new JSONObject();
                item.put("key", addOn.getBusinessKey());
                item.put("value", addOn.getProductName());

                itemList.put(item);
            }
        }

        return createTextResolution(itemList.toString());
    }

    @HandlesEvent("getSummary")
    public Resolution getSummary() throws Exception {
        JSONArray itemList = new JSONArray();

        List<String> comments = editOrder.getSampleSummaryComments();
        for (String comment : comments) {
            JSONObject item = new JSONObject();
            item.put("comment", comment);

            itemList.put(item);
        }

        return createTextResolution(itemList.toString());
    }

    @HandlesEvent("getBspData")
    public Resolution getBspData() throws Exception {
        JSONArray itemList = new JSONArray();

        ProductOrderSample sample = sampleDao.findById(ProductOrderSample.class, sampleId);

        JSONObject item = new JSONObject();

        item.put("sampleId", sample.getProductOrderSampleId());
        item.put("patientId", sample.getBspDTO().getPatientId());
        item.put("volume", sample.getBspDTO().getVolume());
        item.put("concentration", sample.getBspDTO().getConcentration());
        item.put("total", sample.getBspDTO().getTotal());
        item.put("hasFingerprint", sample.getBspDTO().getHasFingerprint());

        return createTextResolution(item.toString());
    }

    @HandlesEvent("getSupportsNumberOfLanes")
    public Resolution getSupportsNumberOfLanes() throws Exception {
        boolean lanesSupported = true;
        JSONObject item = new JSONObject();

        if ( this.product != null ) {
            Product product = productDao.findByBusinessKey(this.product);
            lanesSupported =  product.getSupportsNumberOfLanes();
        }
        item.put("supports", lanesSupported);

        return createTextResolution(item.toString());
    }

    public List<String> getSelectedProductOrderBusinessKeys() {
        return selectedProductOrderBusinessKeys;
    }

    public void setSelectedProductOrderBusinessKeys(List<String> selectedProductOrderBusinessKeys) {
        this.selectedProductOrderBusinessKeys = selectedProductOrderBusinessKeys;
    }

    public List<ProductOrderListEntry> getAllProductOrders() {
        return allProductOrders;
    }

    public String getJiraUrl() {
        return jiraLink.browseUrl();
    }

    public ProductOrder getEditOrder() {
        return editOrder;
    }

    public void setEditOrder(ProductOrder editOrder) {
        this.editOrder = editOrder;
    }

    public String getQuoteUrl() {
        return quoteLink.quoteUrl(editOrder.getQuoteId());
    }

    public String getEditOrderSampleSearchUrl() {
        return bspLink.sampleSearchUrl();
    }

    public static Resolution getTrackerForOrders(
        final CoreActionBean actionBean,
        List<ProductOrder> productOrderList,
        BSPUserList bspUserList) {

        OutputStream outputStream = null;

        try {
            String filename =
                    "BillingTracker-" + AbstractSpreadsheetExporter.DATE_FORMAT.format(Calendar.getInstance().getTime());

            final File tempFile = File.createTempFile(filename, ".xls");
            outputStream = new FileOutputStream(tempFile);

            SampleLedgerExporter sampleLedgerExporter = new SampleLedgerExporter(bspUserList, productOrderList);
            sampleLedgerExporter.writeToStream(outputStream);
            IOUtils.closeQuietly(outputStream);

            return new Resolution() {
                @Override
                public void execute(HttpServletRequest request, HttpServletResponse response)
                        throws Exception {
                    InputStream inputStream = new FileInputStream(tempFile);

                    try {
                        actionBean.setFileDownloadHeaders("application/excel", tempFile.getName());
                        IOUtils.copy(inputStream, actionBean.getContext().getResponse().getOutputStream());
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                        FileUtils.deleteQuietly(tempFile);
                    }
                }
            };
        } catch (Exception ex) {
            actionBean.addGlobalValidationError("Got an exception trying to download the billing tracker: " + ex.getMessage());
            return actionBean.getSourcePageResolution();
        } finally {
            IOUtils.closeQuietly(outputStream);
        }
    }

    public String getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(String productOrder) {
        this.productOrder = productOrder;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    @HandlesEvent("projectAutocomplete")
    public Resolution projectAutocomplete() throws Exception {
        return createTextResolution(projectTokenInput.getJsonString(getQ()));
    }

    @HandlesEvent("productAutocomplete")
    public Resolution productAutocomplete() throws Exception {
        return createTextResolution(productTokenInput.getJsonString(getQ()));
    }

    public List<String> getAddOnKeys() {
        return addOnKeys;
    }

    public void setAddOnKeys(List<String> addOnKeys) {
        this.addOnKeys = addOnKeys;
    }

    public String getSaveButtonText() {
        return ((editOrder == null) || editOrder.isDraft()) ? "Save and Preview" : "Save";
    }

    /**
     * @return true if Place Order is a valid operation.
     */
    public boolean getCanPlaceOrder() {
        // User must be logged into JIRA to place an order.
        return userBean.isValidUser();
    }

    /**
     * @return true if Save is a valid operation.
     */
    public boolean getCanSave() {
        // Unless we're in draft mode, or creating a new order, user must be logged into JIRA to
        // change fields in an order.
        return editOrder.isDraft() || isCreating() || userBean.isValidUser();
    }


    public String getSampleList() {
        if (sampleList == null) {
            sampleList = "";
            for (ProductOrderSample sample : getEditOrder().getSamples()) {
                sampleList += sample.getSampleName() + "\n";
            }
        }

        return sampleList;
    }

    public List<ProductOrderSample> getSamplesAsList() {
        List<ProductOrderSample> samples = new ArrayList<ProductOrderSample>();
        for (String sampleName : SearchActionBean.cleanInputStringForSamples(sampleList)) {
            samples.add(new ProductOrderSample(sampleName));
        }

        return samples;
    }

    public void setSampleList(String sampleList) {
        this.sampleList = sampleList;
    }

    public String getQuoteIdentifier() {
        return quoteIdentifier;
    }

    public void setQuoteIdentifier(String quoteIdentifier) {
        this.quoteIdentifier = quoteIdentifier;
    }

    public UserTokenInput getOwner() {
        return owner;
    }

    /**
     * Sample list edit should be enabled if this is a DRAFT order or this is a non-DRAFT order with no billing
     * ledger entries.
     *
     * @return
     */
    public boolean getAllowSampleListEdit() {
        return editOrder.isDraft() || billingLedgerDao.findByOrderList(editOrder).isEmpty();
    }

    /**
     * The logic here is currently the same as allow sample list edit, but these may both change as we snapshot
     * quotes into ledger entries and/or support sample merging / name overwrites in the sample list
     *
     * @return
     */
    public boolean getAllowQuoteEdit() {
        return editOrder.isDraft() || billingLedgerDao.findByOrderList(editOrder).isEmpty();
    }

    public String getQ() {
        return q;
    }

    public void setQ(String q) {
        this.q = q;
    }

    public ProductTokenInput getProductTokenInput() {
        return productTokenInput;
    }

    public ProjectTokenInput getProjectTokenInput() {
        return projectTokenInput;
    }

    public String getResearchProjectKey() {
        return researchProjectKey;
    }

    public void setResearchProjectKey(String researchProjectKey) {
        this.researchProjectKey = researchProjectKey;
    }

    public long getSampleId() {
        return sampleId;
    }

    public void setSampleId(long sampleId) {
        this.sampleId = sampleId;
    }
}
