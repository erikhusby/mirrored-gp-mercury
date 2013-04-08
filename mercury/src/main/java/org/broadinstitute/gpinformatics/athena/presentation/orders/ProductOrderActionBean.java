package org.broadinstitute.gpinformatics.athena.presentation.orders;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SampleSearchLink;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.mercury.MercuryClientService;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jvnet.inflector.Noun;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * This handles all the needed interface processing elements.
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
    private static final String ADD_SAMPLES_ACTION = "addSamples";
    private static final String ABANDON_SAMPLES_ACTION = "abandonSamples";
    private static final String DELETE_SAMPLES_ACTION = "deleteSamples";
    private static final String SET_RISK = "setRisk";
    private static final String PLACE_ORDER = "placeOrder";

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private QuoteService quoteService;

    @Inject
    private ProductOrderUtil productOrderUtil;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private ProductOrderSampleDao sampleDao;

    @Inject
    private ProductOrderListEntryDao orderListEntryDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private SampleSearchLink sampleSearchLink;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao projectDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private ProductTokenInput productTokenInput;

    @Inject
    private ProjectTokenInput projectTokenInput;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private MercuryClientService mercuryClientService;

    private List<ProductOrderListEntry> allProductOrderListEntries;

    private String sampleList;

    @Validate(required = true, on = {VIEW_ACTION, EDIT_ACTION})
    private String productOrder;

    private List<Long> sampleIdsForGetBspData;

    private final CompletionStatusFetcher progressFetcher = new CompletionStatusFetcher();

    @ValidateNestedProperties({
        @Validate(field="comments", maxlength=2000, on={SAVE_ACTION}),
        @Validate(field="title", required = true, maxlength=255, on={SAVE_ACTION}, label = "Name"),
        @Validate(field="count", on={SAVE_ACTION}, label="Number of Lanes")
    })
    private ProductOrder editOrder;

    // For create, we can also have a research project key to default.
    private String researchProjectKey;

    private List<String> selectedProductOrderBusinessKeys;
    private List<ProductOrder> selectedProductOrders;

    @Validate(required = true, on = {ABANDON_SAMPLES_ACTION, DELETE_SAMPLES_ACTION})
    private List<Long> selectedProductOrderSampleIds;
    private List<ProductOrderSample> selectedProductOrderSamples;

    private String quoteIdentifier;

    private String product;

    private List<String> addOnKeys = new ArrayList<String>();

    @Validate(required = true, on = ADD_SAMPLES_ACTION)
    private String addSamplesText;

    @Validate(required = true, on = SET_RISK)
    private boolean onlyNew = true;

    @Validate(required = true, on = SET_RISK)
    private boolean riskStatus = true;

    @Validate(required = true, on = SET_RISK)
    private String riskComment;

    // This is used for prompting why the abandon button is disabled.
    private String abandonDisabledReason;

    // This is used to determine whether a special warning message needs to be confirmed before normal abandon.
    private boolean abandonWarning = false;
    /**
     * Single {@link ProductOrderListEntry} for the view page, gives us billing session information.
     */
    private ProductOrderListEntry productOrderListEntry;

    /*
     * The search query.
     */
    private String q;

    /** The owner of the Product Order, stored as createdBy in ProductOrder and Reporter in JIRA */
    @Inject
    private UserTokenInput owner;

    /**
     * Initialize the product with the passed in key for display in the form or create it, if not specified.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {"!" + LIST_ACTION, "!getQuoteFunding"})
    public void init() {
        productOrder = getContext().getRequest().getParameter(PRODUCT_ORDER_PARAMETER);
        if (!StringUtils.isBlank(productOrder)) {
            editOrder = productOrderDao.findByBusinessKey(productOrder);

            progressFetcher.loadProgress(productOrderDao, Collections.singletonList(editOrder.getBusinessKey()));
        } else {
            // If this was a create with research project specified, find that.
            // This is only used for save, when creating a new product order.
            editOrder = new ProductOrder();
        }
    }

    @ValidationMethod(on = SAVE_ACTION)
    public void saveValidations() throws Exception {
        // If the research project has no original title, then it was not fetched from hibernate, so this is a create
        // OR if this was fetched and the title has been changed.
        if ((editOrder.getOriginalTitle() == null) ||
                (!editOrder.getTitle().equalsIgnoreCase(editOrder.getOriginalTitle()))) {

            // Check if there is an existing research project and error out if it already exists.
            ProductOrder existingOrder = productOrderDao.findByTitle(editOrder.getTitle());
            if (existingOrder != null) {
                addValidationError("title", "A product order already exists with this name.");
            }
        }

        // Whether we are draft or not, we should populate the proper edit fields for validation.
        updateTokenInputFields();

        // If this is not a draft, some fields are required.
        if (!editOrder.isDraft()) {
            doValidation("save");
        } else {
            // Even in draft, created by must be set. This can't be checked using @Validate (yet),
            // since its value isn't set until updateTokenInputFields() has been called.
            requireField(editOrder.getCreatedBy(), "an owner", "save");
        }
    }

    /**
     * Validate a required field.
     * @param value if null, field is missing
     * @param name name of field
     */
    private void requireField(Object value, String name, String action) {
        requireField(value != null, name, action);
    }

    /**
     * Validate a required field.
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
        requireField(editOrder.getCreatedBy(), "an owner", action);

        if (editOrder.getCreatedBy() != null) {
            String ownerUsername = bspUserList.getById(editOrder.getCreatedBy()).getUsername();
            requireField(jiraService.isValidUser(ownerUsername), "an owner with a JIRA account", action);
        }

        requireField(!editOrder.getSamples().isEmpty(), "any samples", action);
        requireField(editOrder.getResearchProject(), "a research project", action);
        requireField(editOrder.getQuoteId() != null, "a quote specified", action);
        requireField(editOrder.getProduct(), "a product", action);
        if (editOrder.getProduct() != null && editOrder.getProduct().getSupportsNumberOfLanes()) {
            requireField(editOrder.getLaneCount() > 0, "a specified number of lanes", action);
        }

        try {
            quoteService.getQuoteByAlphaId(editOrder.getQuoteId());
        } catch (QuoteServerException ex) {
            addGlobalValidationError("The quote id {2} is not valid: {3}", editOrder.getQuoteId(), ex.getMessage());
        } catch (QuoteNotFoundException ex) {
            addGlobalValidationError("The quote id {2} was not found ", editOrder.getQuoteId());
        }

        // Since we are only validating from view, we can persist without worry of saving something bad.
        // We are doing on risk calculation only when everything passes, but informing the user no matter what.
        if (!hasErrors()) {
            int numSamplesOnRisk = editOrder.calculateRisk();
            editOrder.prepareToSave(getUserBean().getBspUser());
            productOrderDao.persist(editOrder);

            if (numSamplesOnRisk == 0) {
                addMessage("None of the samples for this order are on risk");
            } else {
                addMessage("{0} {1} for this order {2} on risk",
                        numSamplesOnRisk, Noun.pluralOf("sample", numSamplesOnRisk), numSamplesOnRisk == 1 ? "is" : "are");
            }
        } else {
            addGlobalValidationError("On risk was not calculated.  Please fix the other errors first.");
            // Initialize ProductOrderListEntry if we're implicitly going to redisplay the source page.
            entryInit();
        }
    }

    @ValidationMethod(on = PLACE_ORDER)
    public void validatePlacedOrder() {
        doValidation("place order");
    }

    @ValidationMethod(on = {"startBilling", "downloadBillingTracker"})
    public void validateOrderSelection() {
        String validatingFor = getContext().getEventName().equals("startBilling") ? "billing session" :
                "tracker download";

        if (!getUserBean().isValidBspUser()) {
            addGlobalValidationError("A valid bsp user is needed to start a {2}", validatingFor);
        }

        if ((selectedProductOrderBusinessKeys == null) || selectedProductOrderBusinessKeys.isEmpty()) {
            addGlobalValidationError("You must select at least one product order to start a {2}", validatingFor);
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

            // Go through each products and report invalid duplicate price item names.
            for (Product product : products) {
                String[] duplicatePriceItems = product.getDuplicatePriceItemNames();
                if (duplicatePriceItems != null) {
                    addGlobalValidationError(
                            "The Product {2} has duplicate price items: {3}", product.getPartNumber(),
                            StringUtils.join(duplicatePriceItems, ", "));
                }
            }

            // If there are locked out orders, then do not allow the session to start. Using a DAO to do this is a quick
            // way to do this without having to go through all the objects.
            Set<LedgerEntry> lockedOutOrders = ledgerEntryDao.findLockedOutByOrderList(selectedProductOrderBusinessKeys);
            if (!lockedOutOrders.isEmpty()) {
                Set<String> lockedOutOrderStrings = new HashSet<String>(lockedOutOrders.size());
                for (LedgerEntry ledger : lockedOutOrders) {
                    lockedOutOrderStrings.add(ledger.getProductOrderSample().getProductOrder().getTitle());
                }

                String lockedOutString = StringUtils.join(lockedOutOrderStrings, ", ");

                addGlobalValidationError(
                        "The following orders are locked out by active billing sessions: " + lockedOutString);
            }
        }

        // If there are errors, will reload the page, so need to fetch the list.
        if (hasErrors()) {
            listInit();
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = LIST_ACTION)
    public void listInit() {
        allProductOrderListEntries = orderListEntryDao.findProductOrderListEntries();
        progressFetcher.loadProgress(productOrderDao);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = EDIT_ACTION)
    public void createInit() {
        // Once validation is all set for edit (so that any errors can show the originally Checked Items),
        // set the add ons to the current.
        addOnKeys.clear();
        for (ProductOrderAddOn addOnProduct : editOrder.getAddOns()) {
            addOnKeys.add(addOnProduct.getAddOn().getBusinessKey());
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = VIEW_ACTION)
    public void entryInit() {
        productOrderListEntry = editOrder.isDraft() ? ProductOrderListEntry.createDummy() :
                orderListEntryDao.findSingle(editOrder.getJiraTicketKey());
    }

    private void validateUser(String validatingFor) {
        if (!userBean.ensureUserValid()) {
            addGlobalValidationError(MessageFormat.format(UserBean.LOGIN_WARNING, validatingFor + " an order"));
        }
    }

    @HandlesEvent("getQuoteFunding")
    public Resolution getQuoteFunding() {

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
                // Don't really care if this gets an exception.
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

        // If a research project key was specified then use that as the default.
        String[] projectKey;
        if (!StringUtils.isBlank(researchProjectKey)) {
            projectKey = new String[] { researchProjectKey };
        } else {
            projectKey = (editOrder.getResearchProject() == null) ? new String[0] : new String[] { editOrder.getResearchProject().getBusinessKey() };
        }

        projectTokenInput.setup(projectKey);
    }

    @HandlesEvent(PLACE_ORDER)
    public Resolution placeOrder() {
        try {
            editOrder.prepareToSave(userBean.getBspUser());
            editOrder.placeOrder();
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

            // Save it!
            productOrderDao.persist(editOrder);
        } catch (Exception e) {
            // Need to quote the message contents to prevent errors.
            addGlobalValidationError("{2}", e.getMessage());
            // Make sure ProductOrderListEntry is initialized if returning source page resolution.
            entryInit();
            return getSourcePageResolution();
        }

        addMessage("Product Order \"{0}\" has been placed", editOrder.getTitle());

        handleSamplesAdded(editOrder.getSamples());

        return createViewResolution();
    }

    /**
     * There is a validation for this only being allowed for drafts.
     *
     * @return The resolution
     */
    @HandlesEvent("deleteOrder")
    public Resolution deleteOrder() {
        String title = editOrder.getTitle();
        String businessKey = editOrder.getBusinessKey();

        productOrderDao.remove(editOrder);
        addMessage("Deleted order {0} ({1})", title, businessKey);
        return new ForwardResolution(ProductOrderActionBean.class, LIST_ACTION);
    }

    private Resolution createViewResolution() {
        return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(PRODUCT_ORDER_PARAMETER,
                editOrder.getBusinessKey());
    }

    @HandlesEvent("validate")
    public Resolution validate() {
        validatePlacedOrder();

        if (!hasErrors()) {
            addMessage("Draft Order is valid and ready to be placed");
        }

        // entryInit() must be called explicitly here since it does not run automatically with source page resolution
        // and the ProductOrderListEntry that provides billing data would otherwise be null.
        entryInit();

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

        // Save it!
        productOrderDao.persist(editOrder);

        addMessage("Product Order \"{0}\" has been saved.", editOrder.getTitle());
        return createViewResolution();
    }

    private void updateTokenInputFields() {
        // Set the project, product and addOns for the order.
        ResearchProject project = projectDao.findByBusinessKey(projectTokenInput.getTokenObject());
        Product product = productDao.findByPartNumber(productTokenInput.getTokenObject());
        List<Product> addOnProducts = productDao.findByPartNumbers(addOnKeys);
        editOrder.updateData(project, product, addOnProducts, stringToSampleList(sampleList));
        List<BspUser> ownerList = owner.getTokenObjects();
        if (ownerList.isEmpty()) {
            editOrder.setCreatedBy(null);
        } else {
            editOrder.setCreatedBy(ownerList.get(0).getUserId());
        }
    }

    @HandlesEvent("downloadBillingTracker")
    public Resolution downloadBillingTracker() {
        Resolution resolution =
            ProductOrderActionBean.getTrackerForOrders(this, selectedProductOrders, bspUserList, priceListCache);
        if (hasErrors()) {
            // Need to regenerate the list so it's displayed along with the errors.
            listInit();
        }
        return resolution;
    }

    @HandlesEvent("startBilling")
    public Resolution startBilling() {
        Set<LedgerEntry> ledgerItems =
                ledgerEntryDao.findWithoutBillingSessionByOrderList(selectedProductOrderBusinessKeys);
        if (CollectionUtils.isEmpty(ledgerItems)) {
            addGlobalValidationError("There are no items to bill on any of the selected orders");
            return new ForwardResolution(ProductOrderActionBean.class, LIST_ACTION);
        }

        BillingSession session = new BillingSession(getUserBean().getBspUser().getUserId(), ledgerItems);
        billingSessionDao.persist(session);

        return new RedirectResolution(BillingSessionActionBean.class, VIEW_ACTION)
                .addParameter("sessionKey", session.getBusinessKey());
    }

    @HandlesEvent("abandonOrders")
    public Resolution abandonOrders() {

        for (String businessKey : selectedProductOrderBusinessKeys) {
            try {
                productOrderEjb.abandon(businessKey, businessKey + " abandoned by " + userBean.getLoginUserName());
            } catch (ProductOrderEjb.NoTransitionException e) {
                throw new RuntimeException(e);
            } catch (ProductOrderEjb.NoSuchPDOException e) {
                throw new RuntimeException(e);
            } catch (ProductOrderEjb.SampleDeliveryStatusChangeException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new RedirectResolution(ProductOrderActionBean.class, LIST_ACTION);
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
        List<ProductOrderSample> samples = sampleDao.findListByList(
                ProductOrderSample.class, ProductOrderSample_.productOrderSampleId, sampleIdsForGetBspData);

        JSONArray itemList = new JSONArray();

        if (samples != null) {
            // Assuming all samples come from same product order here.
            List<String> sampleNames = ProductOrderSample.getSampleNames(samples);
            ProductOrder.loadBspData(sampleNames, samples);

            for (ProductOrderSample sample : samples) {
                JSONObject item = new JSONObject();

                item.put("sampleId", sample.getProductOrderSampleId());
                item.put("collaboratorSampleId", sample.getBspDTO().getCollaboratorsSampleName());
                item.put("patientId", sample.getBspDTO().getPatientId());
                item.put("collaboratorParticipantId", sample.getBspDTO().getCollaboratorParticipantId());
                item.put("volume", sample.getBspDTO().getVolume());
                item.put("concentration", sample.getBspDTO().getConcentration());
                item.put("rin", sample.getBspDTO().getRin());
                item.put("total", sample.getBspDTO().getTotal());
                item.put("hasFingerprint", sample.getBspDTO().getHasFingerprint());
                item.put("hasSampleKitUploadRackscanMismatch", sample.getBspDTO().getHasSampleKitUploadRackscanMismatch());

                itemList.put(item);
            }
        }

        return createTextResolution(itemList.toString());
    }

    @HandlesEvent("getSupportsNumberOfLanes")
    public Resolution getSupportsNumberOfLanes() throws Exception {
        boolean supportsNumberOfLanes = false;
        JSONObject item = new JSONObject();

        if (this.product != null) {
            Product product = productDao.findByBusinessKey(this.product);
            supportsNumberOfLanes = product.getSupportsNumberOfLanes();
        }
        item.put("supportsNumberOfLanes", supportsNumberOfLanes);

        return createTextResolution(item.toString());
    }

    @ValidationMethod(on = "deleteOrder")
    public void validateDeleteOrder() {
        if (!editOrder.isDraft()) {
            addGlobalValidationError("Orders can only be deleted in draft mode");
        }
    }

    @ValidationMethod(on = {DELETE_SAMPLES_ACTION, ABANDON_SAMPLES_ACTION, SET_RISK}, priority = 0)
    public void validateSampleListOperation() {
        if (selectedProductOrderSampleIds != null) {
            selectedProductOrderSamples = new ArrayList<ProductOrderSample>(selectedProductOrderSampleIds.size());
            for (ProductOrderSample sample : editOrder.getSamples()) {
                if (selectedProductOrderSampleIds.contains(sample.getProductOrderSampleId())) {
                    selectedProductOrderSamples.add(sample);
                }
            }
        } else {
            selectedProductOrderSamples = Collections.emptyList();
        }
    }

    @ValidationMethod(on = {DELETE_SAMPLES_ACTION, ABANDON_SAMPLES_ACTION}, priority = 1)
    public void validateDeleteOrAbandonOperation() {
        if (selectedProductOrderSamples.isEmpty()) {
            addGlobalValidationError("You must select at least one sample.");
        }
    }

    @ValidationMethod(on = DELETE_SAMPLES_ACTION, priority = 2)
    public void validateDeleteSamplesOperation() {
        // Report an error if any sample has billing data associated with it.
        for (ProductOrderSample sample : selectedProductOrderSamples) {
            if (!sample.getLedgerItems().isEmpty()) {
                addGlobalValidationError("Cannot delete sample {2} because billing has started.",
                        sample.getSampleName());
            }
        }
    }

    @HandlesEvent(DELETE_SAMPLES_ACTION)
    public Resolution deleteSamples() throws Exception {
        // If removeAll returns false, no samples were removed -- should never happen.
        if (editOrder.getSamples().removeAll(selectedProductOrderSamples)) {
            String nameList = StringUtils.join(ProductOrderSample.getSampleNames(selectedProductOrderSamples), ",");
            editOrder.prepareToSave(getUserBean().getBspUser());
            productOrderDao.persist(editOrder);
            addMessage("Deleted samples: {0}.", nameList);
            JiraIssue issue = jiraService.getIssue(editOrder.getJiraTicketKey());
            issue.addComment(MessageFormat.format("{0} deleted samples: {1}.", userBean.getLoginUserName(), nameList));
            issue.setCustomFieldUsingTransition(ProductOrder.JiraField.SAMPLE_IDS,
                    editOrder.getSampleString(),
                    ProductOrder.TransitionStates.DeveloperEdit.getStateName());
        }
        return createViewResolution();
    }

    @HandlesEvent(SET_RISK)
    public Resolution setRisk() throws Exception {

        // If we are creating a manual on risk, then need to set it up and persist it for reuse.
        RiskCriterion criterion = null;
        if (riskStatus) {
            criterion = RiskCriterion.createManual();
            productOrderDao.persist(criterion);
        }

        for (ProductOrderSample sample : selectedProductOrderSamples) {
            if (riskStatus) {
                sample.setManualOnRisk(criterion, riskComment);
            } else {
                sample.setManualNotOnRisk(riskComment);
            }
        }

        editOrder.prepareToSave(getUserBean().getBspUser());
        productOrderDao.persist(editOrder);

        addMessage("Set manual on risk for {0} samples.", selectedProductOrderSampleIds.size());
        JiraIssue issue = jiraService.getIssue(editOrder.getJiraTicketKey());
        issue.addComment(MessageFormat.format("{0} set manual on risk for {1} samples.",
                userBean.getLoginUserName(), selectedProductOrderSampleIds.size()));
        return createViewResolution();
    }

    @HandlesEvent(ABANDON_SAMPLES_ACTION)
    public Resolution abandonSamples() throws Exception {
        // Handle case where user is trying to abandon samples that are already abandoned.
        Iterator<ProductOrderSample> samples = selectedProductOrderSamples.iterator();
        while (samples.hasNext()) {
            ProductOrderSample sample = samples.next();
            if (sample.getDeliveryStatus() == ProductOrderSample.DeliveryStatus.ABANDONED) {
                samples.remove();
            }
        }
        if (!selectedProductOrderSamples.isEmpty()) {
            productOrderEjb.abandonSamples(editOrder.getJiraTicketKey(), selectedProductOrderSamples);
        }
        return createViewResolution();
    }

    @HandlesEvent(ADD_SAMPLES_ACTION)
    public Resolution addSamples() throws Exception {
        List<ProductOrderSample> samplesToAdd = stringToSampleList(addSamplesText);
        editOrder.addSamples(samplesToAdd);
        editOrder.prepareToSave(getUserBean().getBspUser());
        productOrderDao.persist(editOrder);
        String nameList = StringUtils.join(ProductOrderSample.getSampleNames(samplesToAdd), ",");
        addMessage("Added samples: {0}.", nameList);
        JiraIssue issue = jiraService.getIssue(editOrder.getJiraTicketKey());
        issue.addComment(MessageFormat.format("{0} added samples: {1}.", userBean.getLoginUserName(), nameList));
        issue.setCustomFieldUsingTransition(ProductOrder.JiraField.SAMPLE_IDS,
                editOrder.getSampleString(),
                ProductOrder.TransitionStates.DeveloperEdit.getStateName());

        handleSamplesAdded(samplesToAdd);

        return createViewResolution();
    }

    private void handleSamplesAdded(List<ProductOrderSample> newSamples) {
        Collection<ProductOrderSample> samples = mercuryClientService.addSampleToPicoBucket(editOrder, newSamples);
        if (!samples.isEmpty()) {
                addMessage("{0} samples have been added to the pico bucket.", samples.size());
        }
    }

    public List<String> getSelectedProductOrderBusinessKeys() {
        return selectedProductOrderBusinessKeys;
    }

    public void setSelectedProductOrderBusinessKeys(List<String> selectedProductOrderBusinessKeys) {
        this.selectedProductOrderBusinessKeys = selectedProductOrderBusinessKeys;
    }

    public List<Long> getSelectedProductOrderSampleIds() {
        return selectedProductOrderSampleIds;
    }

    public void setSelectedProductOrderSampleIds(List<Long> selectedProductOrderSampleIds) {
        this.selectedProductOrderSampleIds = selectedProductOrderSampleIds;
    }

    public List<ProductOrderListEntry> getAllProductOrderListEntries() {
        return allProductOrderListEntries;
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

    public String sampleSearchUrlForBspSample(ProductOrderSample sample) {
        return sampleSearchLink.getUrl(sample);
    }

    public static Resolution getTrackerForOrders(
        final CoreActionBean actionBean,
        List<ProductOrder> productOrderList,
        BSPUserList bspUserList,
        PriceListCache priceListCache) {

        OutputStream outputStream = null;

        try {
            String filename =
                "BillingTracker-" + AbstractSpreadsheetExporter.DATE_FORMAT.format(Calendar.getInstance().getTime());

            // Colon is a metacharacter in Windows separating the drive letter from the rest of the path.
            filename = filename.replaceAll(":", "_");

            final File tempFile = File.createTempFile(filename, ".xls");
            outputStream = new FileOutputStream(tempFile);

            SampleLedgerExporter sampleLedgerExporter =
                new SampleLedgerExporter(bspUserList, priceListCache, productOrderList);
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
            sampleList = editOrder.getSampleString();
        }

        return sampleList;
    }

    private static List<ProductOrderSample> stringToSampleList(String sampleListText) {
        List<ProductOrderSample> samples = new ArrayList<ProductOrderSample>();
        for (String sampleName : SearchActionBean.cleanInputStringForSamples(sampleListText)) {
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
     * Sample list edit is only enabled if this is a DRAFT order.  Once an order has been placed, users must use the
     * UI in the view PDO page to edit the samples in an order.
     *
     * @return true if user can edit the sample list
     */
    public boolean getAllowSampleListEdit() {
        return editOrder.isDraft();
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

    public List<Long> getSampleIdsForGetBspData() {
        return sampleIdsForGetBspData;
    }

    public void setSampleIdsForGetBspData(List<Long> sampleIdsForGetBspData) {
        this.sampleIdsForGetBspData = sampleIdsForGetBspData;
    }

    public String getAddSamplesText() {
        return addSamplesText;
    }

    public void setAddSamplesText(String addSamplesText) {
        this.addSamplesText = addSamplesText;
    }

    public boolean isRiskStatus() {
        return riskStatus;
    }

    public void setRiskStatus(boolean riskStatus) {
        this.riskStatus = riskStatus;
    }

    public boolean isOnlyNew() {
        return onlyNew;
    }

    public void setOnlyNew(boolean onlyNew) {
        this.onlyNew = onlyNew;
    }

    public String getRiskComment() {
        return riskComment;
    }

    public void setRiskComment(String riskComment) {
        this.riskComment = riskComment;
    }

    public CompletionStatusFetcher getProgressFetcher() {
        return progressFetcher;
    }

    /**
     * Getter for the {@link ProductOrderListEntry} for the PDO view page.
     *
     * @return {@link ProductOrderListEntry} for the view page's PDO.
     */
    public ProductOrderListEntry getProductOrderListEntry() {
        return productOrderListEntry;
    }


    /**
     * Convenience method to determine if the current PDO for the view page is eligible for billing.
     *
     * @return Boolean eligible for billing.
     */
    public boolean isEligibleForBilling() {
        return editOrder.getBusinessKey() != null && getProductOrderListEntry().isEligibleForBilling();
    }


    /**
     * Convenience method to return the billing session ID for the current PDO in the view page, if any.  Throws
     * an exception if the current PDO is not eligible for billing.
     *
     * @return Billing session for this PDO if any, null if none.
     */
    public String getBillingSessionBusinessKey() {
        if (!isEligibleForBilling()) {
            throw new RuntimeException("Product Order is not eligible for billing");
        }

        return getProductOrderListEntry().getBillingSessionBusinessKey();
    }

    /**
     * Convenience method for verifying whether a PDO is able to be abandoned.
     *
     * @return Boolean eligible for abandoning
     */
    public boolean isAbandonable() {
        if (!editOrder.isSubmitted()) {
            setAbandonDisabledReason("the order status of the PDO is not 'Submitted'.");
            return false;
        } else if (productOrderSampleDao.countSamplesWithLedgerEntries(editOrder) > 0) {
            setAbandonWarning(true);
        }
        return true;
    }

    /**
     * Convenience method for PDO view page to show percentage of samples abandoned for the current PDO.
     *
     * @return Percentage of sample abandoned.
     */
    public int getPercentAbandoned() {
        return progressFetcher.getPercentAbandoned(editOrder.getBusinessKey());
    }


    /**
     * Convenience method for PDO view page to show percentage of samples completed for the current PDO.
     *
     * @return Percentage of sample completed.
     */
    public int getPercentCompleted() {
        return progressFetcher.getPercentCompleted(editOrder.getBusinessKey());
    }


    /**
     * Convenience method for PDO view page to show percentage of samples in progress for the current PDO.
     *
     * @return Percentage of sample in progress.
     */
    public int getPercentInProgress() {
        return progressFetcher.getPercentInProgress(editOrder.getBusinessKey());
    }

    /**
     * Computes the String to show for sample progress, taking into account Draft status and excluding mention of
     * zero-valued progress categories.
     *
     * @return Progress String suitable for display under progress bar.
     */
    public String getProgressString() {

        if (editOrder.isDraft()) {
            return "Draft order, work has not begun";
        }
        else {
            List<String> progressPieces = new ArrayList<String>();
            if (getPercentAbandoned() != 0) {
                progressPieces.add(getPercentAbandoned() + "% Abandoned");
            }
            if (getPercentCompleted() != 0) {
                progressPieces.add(getPercentCompleted() + "% Completed");
            }
            if (getPercentInProgress() != 0) {
                progressPieces.add(getPercentInProgress() + "% In Progress");
            }

            return StringUtils.join(progressPieces, ", ");
        }
    }

    /**
     * @return Reason that abandon button is disabled
     */
    public String getAbandonDisabledReason() {
        return abandonDisabledReason;
    }

    /**
     * Update reason why the abandon button is disabled.
     * @param abandonDisabledReason String of text indicating why the abandon button is disabled
     */
    public void setAbandonDisabledReason(String abandonDisabledReason) {
        this.abandonDisabledReason = abandonDisabledReason;
    }

    /**
     * @return Whether there is a need to use the special warning format
     */
    public boolean getAbandonWarning() {
        return abandonWarning;
    }

    /**
     * Update whether to use special warning.
     * @param abandonWarning Boolean flag used to determine whether we need to use special message confirmation
     */
    public void setAbandonWarning(boolean abandonWarning) {
        this.abandonWarning = abandonWarning;
    }

    public boolean isSupportsRin() {
        return (editOrder != null) && (editOrder.getProduct() != null) && editOrder.getProduct().isSupportsRin();
    }
}
