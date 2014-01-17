package org.broadinstitute.gpinformatics.athena.presentation.orders;

import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.PostReceiveOption;
import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.exception.SourcePageNotFoundException;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidateNestedProperties;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.util.IOUtils;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.kit.KitTypeAllowanceSpecification;
import org.broadinstitute.bsp.client.workrequest.kit.MaterialInfo;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.util.AbstractSpreadsheetExporter;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.preference.NameValueDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspGroupCollectionTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspShippingLocationTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPKitRequestService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jvnet.inflector.Noun;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.text.Format;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This handles all the needed interface processing elements.
 */
@SuppressWarnings("unused")
@UrlBinding(ProductOrderActionBean.ACTIONBEAN_URL_BINDING)
public class ProductOrderActionBean extends CoreActionBean {

    private static Log logger = LogFactory.getLog(ProductOrderActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/orders/order.action";
    public static final String PRODUCT_ORDER_PARAMETER = "productOrder";

    private static final String PRODUCT_ORDER = "Product Order";
    public static final String CREATE_ORDER = CoreActionBean.CREATE + PRODUCT_ORDER;
    public static final String EDIT_ORDER = CoreActionBean.EDIT + PRODUCT_ORDER;

    private static final String ORDER_CREATE_PAGE = "/orders/create.jsp";
    private static final String ORDER_LIST_PAGE = "/orders/list.jsp";
    private static final String ORDER_VIEW_PAGE = "/orders/view.jsp";

    private static final String ADD_SAMPLES_ACTION = "addSamples";
    private static final String ABANDON_SAMPLES_ACTION = "abandonSamples";
    private static final String DELETE_SAMPLES_ACTION = "deleteSamples";
    private static final String SET_RISK = "setRisk";
    private static final String RECALCULATE_RISK = "recalculateRisk";
    private static final String PLACE_ORDER = "placeOrder";
    private static final String VALIDATE_ORDER = "validate";
    // Search field constants
    private static final String FAMILY = "productFamily";
    private static final String PRODUCT = "product";
    private static final String STATUS = "status";
    private static final String LEDGER_STATUS = "ledgerStatus";
    private static final String DATE = "date";
    private static final String OWNER = "owner";
    private static final String ADD_SAMPLES_TO_BUCKET = "addSamplesToBucket";

    public ProductOrderActionBean() {
        super(CREATE_ORDER, EDIT_ORDER, PRODUCT_ORDER_PARAMETER);
    }

    @Inject
    private ProductFamilyDao productFamilyDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao projectDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private PreferenceEjb preferenceEjb;

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
    private ProductOrderEjb productOrderEjb;

    @Inject
    private ProductTokenInput productTokenInput;

    @Inject
    private ProjectTokenInput projectTokenInput;

    @Inject
    private BspShippingLocationTokenInput bspShippingLocationTokenInput;

    @Inject
    private BspGroupCollectionTokenInput bspGroupCollectionTokenInput;

    @Inject
    private BSPManagerFactory bspManagerFactory;

    @Inject
    private UserTokenInput notificationListTokenInput;

    @Inject
    private BSPConfig bspConfig;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private JiraService jiraService;

    @Inject
    private BSPKitRequestService bspKitRequestService;

    private List<ProductOrderListEntry> displayedProductOrderListEntries;

    private String sampleList;

    @Validate(required = true, on = {EDIT_ACTION})
    private String productOrder;

    private boolean skipQuote = false;

    private List<Long> sampleIdsForGetBspData;

    private final CompletionStatusFetcher progressFetcher = new CompletionStatusFetcher();

    private static final Format dateFormatter = FastDateFormat.getInstance(DATE_PATTERN);

    @ValidateNestedProperties({
            @Validate(field = "comments", maxlength = 2000, on = {SAVE_ACTION}),
            @Validate(field = "title", required = true, maxlength = 255, on = {SAVE_ACTION}, label = "Name"),
            @Validate(field = "count", on = {SAVE_ACTION}, label = "Number of Lanes")
    })
    private ProductOrder editOrder;

    // For create, we can also have a research project key to default.
    private String researchProjectKey;

    private List<String> selectedProductOrderBusinessKeys;
    private List<ProductOrder> selectedProductOrders;

    @Validate(required = true, on = {ABANDON_SAMPLES_ACTION, DELETE_SAMPLES_ACTION, ADD_SAMPLES_TO_BUCKET})
    private List<Long> selectedProductOrderSampleIds;
    private List<ProductOrderSample> selectedProductOrderSamples;

    // used only as part of ajax call to get funds remaining.  Quote field is bound to editOrder.
    private String quoteIdentifier;

    private String product;

    private String materialInfo;

    private List<String> addOnKeys = new ArrayList<>();
    private List<String> postReceiveOptionKeys = new ArrayList<>();

    @Validate(required = true, on = ADD_SAMPLES_ACTION)
    private String addSamplesText;

    @Validate(required = true, on = SET_RISK)
    private boolean riskStatus = true;

    @Validate(required = true, on = SET_RISK)
    private String riskComment;

    private String abandonComment;

    // This is used for prompting why the abandon button is disabled.
    private String abandonDisabledReason;

    // This is used to determine whether a special warning message needs to be confirmed before normal abandon.
    private boolean abandonWarning;

    // Single {@link ProductOrderListEntry} for the view page, gives us billing session information.
    private ProductOrderListEntry productOrderListEntry;

    private Long productFamilyId;

    private List<ProductOrder.OrderStatus> selectedStatuses;

    private List<ProductOrder.LedgerStatus> selectedLedgerStatuses;

    private static List<MaterialInfo> dnaMatrixMaterialTypes=
            Arrays.asList(KitTypeAllowanceSpecification.DNA_MATRIX_KIT.getMaterialInfo());

    /*
     * The search query.
     */
    private String q;

    /**
     * The owner of the Product Order, stored as createdBy in ProductOrder and Reporter in JIRA
     */
    @Inject
    private UserTokenInput owner;

    // Search uses product family list.
    private List<ProductFamily> productFamilies;


    @Inject
    private LabVesselDao labVesselDao;

    private Map<String, Date> productOrderSampleReceiptDates;

    /**
     * Initialize the product with the passed in key for display in the form or create it, if not specified.
     */
    @Before(stages = LifecycleStage.BindingAndValidation,
            on = {"!" + LIST_ACTION, "!getQuoteFunding", "!" + VIEW_ACTION})
    public void init() {
        productOrder = getContext().getRequest().getParameter(PRODUCT_ORDER_PARAMETER);
        if (!StringUtils.isBlank(productOrder)) {
            editOrder = productOrderDao.findByBusinessKey(productOrder);
            if (editOrder != null) {
                skipQuote = !StringUtils.isEmpty(editOrder.getSkipQuoteReason());
                progressFetcher.loadProgress(productOrderDao, Collections.singletonList(editOrder.getProductOrderId()));
            }
        } else {
            // If this was a create with research project specified, find that.
            // This is only used for save, when creating a new product order.
            editOrder = new ProductOrder();
        }
    }

    /**
     * Initialize the product with the passed in key for display in the form or create it, if not specified.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION})
    public void editInit() {
        productOrder = getContext().getRequest().getParameter(PRODUCT_ORDER_PARAMETER);
        // If there's no product order parameter, send an error.
        if (StringUtils.isBlank(productOrder)) {
            addGlobalValidationError("No product order was specified.");
        } else {
            // Since just getting the one item, get all the lazy data.
            editOrder = productOrderDao.findByBusinessKey(productOrder, ProductOrderDao.FetchSpec.RISK_ITEMS);
            if (editOrder != null) {
                progressFetcher.loadProgress(productOrderDao, Collections.singletonList(editOrder.getProductOrderId()));
            }
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
            doValidation(SAVE_ACTION);
        } else {
            // Even in draft, created by must be set. This can't be checked using @Validate (yet),
            // since its value isn't set until updateTokenInputFields() has been called.
            requireField(editOrder.getCreatedBy(), "an owner", "save");
            validateSkipQuoteOptions();
        }
    }

    /**
     * Validate a required field.
     *
     * @param value if null, field is missing
     * @param name  name of field
     */
    private void requireField(Object value, String name, String action) {
        requireField(value != null, name, action);
    }

    /**
     * Validate a required field.
     *
     * @param hasValue if false, field is missing
     * @param name     name of field
     */
    private void requireField(boolean hasValue, String name, String action) {
        if (!hasValue) {
            addGlobalValidationError("Cannot {2} ''{3}'' because it does not have {4}.", action, editOrder.getName(),
                    name);
        }
    }

    /**
     * Validates the rin scores for every
     * sample in the pdo.
     * @see #validateRinScores(java.util.Collection)
     */
    void validateRinScores(@Nonnull ProductOrder pdo) {
        if (pdo.isRinScoreValidationRequired()) {
            validateRinScores(pdo.getSamples());
        }
    }

    /**
     * Checks that the rin score is a numeric value for all BSP samples in the list of pdoSamples.
     *
     * @param pdoSamples The samples
     */
    void validateRinScores(Collection<ProductOrderSample> pdoSamples) {
        for (ProductOrderSample pdoSample : pdoSamples) {
            if (pdoSample.isInBspFormat() && !pdoSample.canRinScoreBeUsedForOnRiskCalculation()) {
                addGlobalValidationError(
                        "RIN '" + pdoSample.getBspSampleDTO().getRawRin() + "' for " + pdoSample.getName() +
                        " isn't a number." + "  Please correct this by going to BSP -> Utilities -> Upload Sample " +
                        "Annotation and updating the 'RIN Number' annotation for this sample.");
            }
        }
    }

    private void doValidation(String action) {
        requireField(editOrder.getCreatedBy(), "an owner", action);

        if (editOrder.getCreatedBy() != null) {
            String ownerUsername = bspUserList.getById(editOrder.getCreatedBy()).getUsername();
            requireField(jiraService.isValidUser(ownerUsername), "an owner with a JIRA account", action);
        }

        ResearchProject researchProject = editOrder.getResearchProject();
        if (!isSampleInitiation()) {
            requireField(!editOrder.getSamples().isEmpty(), "any samples", action);
        } else {
            ProductOrderKit kit = editOrder.getProductOrderKit();
            Long numberOfSamples = kit.getNumberOfSamples();
            if (kit.getNumberOfSamples() == null) {
                numberOfSamples = (long) 0;
            }
            requireField(numberOfSamples > 0, "a specified number of samples", action);
            requireField(kit.getSiteId(), "a site", action);
            requireField(kit.getKitType().getKitName(), "a kit type", action);
            requireField(kit.getBspMaterialName(), "a material information", action);
            requireField(kit.getSampleCollectionId(), "a collection", action);
            requireField(kit.getOrganismId(), "an organism", action);

            // Avoid NPE if Research Project isn't set yet.
            if (researchProject != null) {
                requireField(researchProject.getBroadPIs().length > 0,
                        "a Research Project with a primary investigator", action);
                requireField(researchProject.getExternalCollaborators().length > 0,
                        "a Research Project with an external collaborator", action);
            }
            if (editOrder.getProductOrderKit().getTransferMethod() == SampleKitWorkRequest.TransferMethod.PICK_UP) {
                String validationMessage = String.format("a notification list, which required when \"%s\" is selected",
                        SampleKitWorkRequest.TransferMethod.PICK_UP.getValue());
                requireField(editOrder.getProductOrderKit().getNotificationIds().length > 0, validationMessage, action);
            }
        }
        requireField(researchProject, "a research project", action);
        if (!ApplicationInstance.CRSP.isCurrent()) {
            validateQuoteOptions(action);
        }
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
        if (editOrder != null) {
            validateRinScores(editOrder);
        }
    }

    private void doOnRiskUpdate() {
        try {
            // Calculate risk here and get back any error message.
            productOrderEjb.calculateRisk(editOrder.getBusinessKey());

            // refetch the order to get updated risk status on the order.
            editOrder = productOrderDao.findByBusinessKey(editOrder.getBusinessKey());
            int numSamplesOnRisk = editOrder.countItemsOnRisk();

            if (numSamplesOnRisk == 0) {
                addMessage("None of the samples for this order are on risk");
            } else {
                addMessage("{0} {1} for this order {2} on risk",
                        numSamplesOnRisk, Noun.pluralOf("sample", numSamplesOnRisk),
                        numSamplesOnRisk == 1 ? "is" : "are");
            }
        } catch (Exception e) {
            addGlobalValidationError(e.getMessage());
        }
    }

    @ValidationMethod(on = PLACE_ORDER)
    public void validatePlacedOrder() {
        validatePlacedOrder("place order");
    }

    public void validatePlacedOrder(String action) {

        doValidation(action);

        if (!hasErrors()) {
            doOnRiskUpdate();
        }

        updateFromInitiationTokenInputs();
    }

    @ValidationMethod(on = {"startBilling", "downloadBillingTracker"})
    public void validateOrderSelection() throws Exception {
        String validatingFor = getContext().getEventName().equals("startBilling") ? "billing session" :
                "tracker download";

        if (!getUserBean().isValidBspUser()) {
            addGlobalValidationError("A valid BSP user is needed to start a {2}", validatingFor);
        }

        if ((selectedProductOrderBusinessKeys == null) || selectedProductOrderBusinessKeys.isEmpty()) {
            addGlobalValidationError("You must select at least one product order to start a {2}", validatingFor);
        } else {
            Set<Product> products = new HashSet<>();
            selectedProductOrders =
                    productOrderDao.findListByBusinessKeys(selectedProductOrderBusinessKeys,
                            ProductOrderDao.FetchSpec.PRODUCT, ProductOrderDao.FetchSpec.RESEARCH_PROJECT,
                            ProductOrderDao.FetchSpec.SAMPLES);

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
            Set<LedgerEntry> lockedOutOrders =
                    ledgerEntryDao.findLockedOutByOrderList(selectedProductOrderBusinessKeys);
            if (!lockedOutOrders.isEmpty()) {
                Set<String> lockedOutOrderStrings = new HashSet<>(lockedOutOrders.size());
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
            setupListDisplay();
        }
    }

    /**
     * Set up defaults before binding and validation and then let any binding override that. Note that the Core Action
     * Bean sets up the single date range object to be this month.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = LIST_ACTION)
    public void setupSearchCriteria() throws Exception {
        // Get the saved search for the user.
        List<Preference> preferences =
                preferenceEjb.getPreferences(getUserBean().getBspUser().getUserId(), PreferenceType.PDO_SEARCH);

        productFamilyId = null;

        if (CollectionUtils.isEmpty(preferences)) {
            populateSearchDefaults();
        } else {
            try {
                // Since we have a preference and there is only one, grab it and set up all the search terms.
                Preference preference = preferences.get(0);
                populateSearchFromPreference(preference);
            } catch (Throwable t) {
                // If there are any errors with this preference, just use defaults and populate a message that
                // the defaults were ignored for some reason.
                populateSearchDefaults();
                logger.error("Could not read user preference on search for product orders.");
                addMessage("Could not read search preference");
            }
        }
    }

    private void populateSearchDefaults() {
        selectedStatuses = new ArrayList<>();
        selectedStatuses.add(ProductOrder.OrderStatus.Draft);
        selectedStatuses.add(ProductOrder.OrderStatus.Submitted);

        setDateRange(new DateRangeSelector(DateRangeSelector.THIS_MONTH));
    }

    /**
     * This populates all the search date from the first preference object.
     *
     * @param preference The single preference for PDO search.
     *
     * @throws Exception Any errors.
     */
    private void populateSearchFromPreference(Preference preference) throws Exception {
        PreferenceDefinitionValue value = preference.getPreferenceDefinition().getDefinitionValue();
        Map<String, List<String>> preferenceData = ((NameValueDefinitionValue) value).getDataMap();

        // The family id. By default there is no choice.
        List<String> productFamilyIds = preferenceData.get(FAMILY);
        if (!CollectionUtils.isEmpty(productFamilyIds)) {
            String familyId = preferenceData.get(FAMILY).get(0);
            if (!StringUtils.isBlank(familyId)) {
                productFamilyId = Long.parseLong(familyId);
            }
        }

        // Set up all the simple fields from the values in the preference data.
        productTokenInput.setListOfKeys(preferenceData.get(PRODUCT));
        selectedStatuses = ProductOrder.OrderStatus.getFromNames(preferenceData.get(STATUS));
        selectedLedgerStatuses = ProductOrder.LedgerStatus.getFromNames(preferenceData.get(LEDGER_STATUS));
        setDateRange(new DateRangeSelector(preferenceData.get(DATE)));
        owner.setListOfKeys(preferenceData.get(OWNER));
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = LIST_ACTION)
    public void listInit() throws Exception {

        // Do the specified find and then add all the % complete info for the progress bars.
        displayedProductOrderListEntries =
                orderListEntryDao.findProductOrderListEntries(
                        productFamilyId, productTokenInput.getBusinessKeyList(), selectedStatuses, getDateRange(),
                        owner.getOwnerIds(), selectedLedgerStatuses);


        progressFetcher.loadProgress(productOrderDao,
                ProductOrderListEntry.getProductOrderIDs(displayedProductOrderListEntries));

        // Get the sorted family list.
        productFamilies = productFamilyDao.findAll();
        Collections.sort(productFamilies);
    }

    /**
     * If we have successfully run the search, save the selected values to the preference.
     *
     * @throws Exception Any errors
     */
    @After(stages = LifecycleStage.EventHandling, on = LIST_ACTION)
    private void saveSearchPreference() throws Exception {
        NameValueDefinitionValue definitionValue = new NameValueDefinitionValue();

        definitionValue.put(FAMILY, productFamilyId == null ? "" : productFamilyId.toString());

        definitionValue.put(PRODUCT, productTokenInput.getBusinessKeyList());

        List<String> statusStrings = ProductOrder.OrderStatus.getStrings(selectedStatuses);
        List<String> ledgerStatusStrings = ProductOrder.LedgerStatus.getStrings(selectedLedgerStatuses);

        definitionValue.put(STATUS, statusStrings);

        definitionValue.put(LEDGER_STATUS, ledgerStatusStrings);

        definitionValue.put(DATE, getDateRange().createDateStrings());

        definitionValue.put(OWNER, owner.getTokenBusinessKeys());

        preferenceEjb.add(userBean.getBspUser().getUserId(), PreferenceType.PDO_SEARCH, definitionValue);
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = EDIT_ACTION)
    public void createInit() {
        // Once validation is all set for edit (so that any errors can show the originally Checked Items),
        // set the add ons to the current.
        addOnKeys.clear();
        for (ProductOrderAddOn addOnProduct : editOrder.getAddOns()) {
            addOnKeys.add(addOnProduct.getAddOn().getBusinessKey());
        }
        postReceiveOptionKeys.clear();
        for (PostReceiveOption postReceiveOption : editOrder.getProductOrderKit().getPostReceiveOptions()) {
            postReceiveOptionKeys.add(postReceiveOption.getText());
        }
    }

    // All actions that can result in the view page loading (either by a validation error or view itself)
    @After(stages = LifecycleStage.BindingAndValidation,
            on = {EDIT_ACTION, VIEW_ACTION, ADD_SAMPLES_ACTION, SET_RISK, RECALCULATE_RISK, ABANDON_SAMPLES_ACTION,
                    DELETE_SAMPLES_ACTION, PLACE_ORDER, VALIDATE_ORDER})
    public void entryInit() {
        if (editOrder != null) {
            productOrderListEntry = editOrder.isDraft() ? ProductOrderListEntry.createDummy() :
                    orderListEntryDao.findSingle(editOrder.getJiraTicketKey());

            ProductOrder.loadLabEventSampleData(editOrder.getSamples());
        }
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
        if (editOrder == null) {
            addGlobalValidationError("A PDO named '" + productOrder + "' could not be found.");
            Resolution errorResolution;

            try {
                errorResolution = getContext().getSourcePageResolution();
            } catch (SourcePageNotFoundException e) {
                errorResolution = new ForwardResolution(ORDER_LIST_PAGE);
            }

            return errorResolution;
        }

        updateFromInitiationTokenInputs();
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
     * Set all the transients using the Injected Token Input, even though the JSP doesn't set them
     */
    private void updateFromInitiationTokenInputs() {
        ProductOrderKit productOrderKit = editOrder.getProductOrderKit();

        String sampleCollectionKey = productOrderKit.getSampleCollectionId() != null ?
                String.valueOf(productOrderKit.getSampleCollectionId()) : null;
        bspGroupCollectionTokenInput.setup(
                !StringUtils.isBlank(sampleCollectionKey) ? new String[]{sampleCollectionKey} : new String[0]);

        productOrderKit.setOrganismName(null);

        SampleCollection sampleCollection = bspGroupCollectionTokenInput.getTokenObject();
        if (sampleCollection != null) {
            if (productOrderKit.getOrganismId() != null) {
                for (Pair<Long, String> organism : sampleCollection.getOrganisms()) {
                    if (productOrderKit.getOrganismId().equals(organism.getLeft())) {
                        productOrderKit.setOrganismName(organism.getRight());
                        break;
                    }
                }
            }

            productOrderKit.setSampleCollectionName(sampleCollection.getCollectionName());
        }

        String siteKey = productOrderKit.getSiteId() != null ?
                String.valueOf(productOrderKit.getSiteId()) : null;

        // For view, there are no token input objects, so need to set it up here.
        bspShippingLocationTokenInput.setup(
                !StringUtils.isBlank(siteKey) ? new String[]{siteKey} : new String[0]);
        if (productOrderKit.getSiteId() != null) {
            Site tokenObject = bspShippingLocationTokenInput.getTokenObject();
            if (tokenObject != null) {
                productOrderKit.setSiteName(tokenObject.getName());
            }
        }
    }

    /**
     * For the pre-populate to work on opening create and edit page, we need to take values from the editOrder. After,
     * the pages have the values passed in.
     */
    private void populateTokenListsFromObjectData() {
        String[] productKey = (editOrder.getProduct() == null) ? new String[0] :
                new String[]{editOrder.getProduct().getBusinessKey()};
        productTokenInput.setup(productKey);

        // If a research project key was specified then use that as the default.
        String[] projectKey;
        if (!StringUtils.isBlank(researchProjectKey)) {
            projectKey = new String[]{researchProjectKey};
        } else {
            projectKey = (editOrder.getResearchProject() == null) ? new String[0] :
                    new String[]{editOrder.getResearchProject().getBusinessKey()};
        }
        projectTokenInput.setup(projectKey);

        updateFromInitiationTokenInputs();

        if (bspShippingLocationTokenInput != null) {
            String siteKey = editOrder.getProductOrderKit().getSiteId() != null ? String.valueOf(editOrder.getProductOrderKit().getSiteId()) : null;
            bspShippingLocationTokenInput.setup(!StringUtils.isBlank(siteKey) ? new String[]{siteKey} : new String[0]);
        }

        notificationListTokenInput.setup(editOrder.getProductOrderKit().getNotificationIds());
    }

    @HandlesEvent(PLACE_ORDER)
    public Resolution placeOrder() {
        String originalBusinessKey = editOrder.getBusinessKey();

        try {
            editOrder.prepareToSave(userBean.getBspUser());
            editOrder.placeOrder();
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);

            if (isSampleInitiation()) {
                String workRequestBarcode = bspKitRequestService.createAndSubmitKitRequestForPDO(editOrder);
                editOrder.getProductOrderKit().setWorkRequestId(workRequestBarcode);
                addMessage("Created BSP work request ''{0}'' for this order.", workRequestBarcode);
            }

            originalBusinessKey = null;
            productOrderDao.persist(editOrder);
        } catch (Exception e) {

            // If we get here with an original business key, then clear out the session and refetch the order.
            if (originalBusinessKey != null) {
                productOrderDao.clear();
                editOrder = productOrderDao.findByBusinessKey(originalBusinessKey);
            }

            updateFromInitiationTokenInputs();

            addGlobalValidationError(e.toString());
            // Make sure ProductOrderListEntry is initialized if returning source page resolution.
            entryInit();
            return getSourcePageResolution();
        }

        addMessage("Product Order \"{0}\" has been placed", editOrder.getTitle());
        productOrderEjb.handleSamplesAdded(editOrder.getBusinessKey(), editOrder.getSamples(), this);

        return createViewResolution(editOrder.getBusinessKey());
    }

    private Resolution handlePlaceError(Exception e) {
        addGlobalValidationError(e.toString());
        entryInit();
        return getSourcePageResolution();
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

    private Resolution createViewResolution(String businessKey) {
        return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(PRODUCT_ORDER_PARAMETER,
                businessKey);
    }

    @HandlesEvent(VALIDATE_ORDER)
    public Resolution validate() {
        validatePlacedOrder("validate");
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
        ProductOrder.SaveType saveType = ProductOrder.SaveType.UPDATING;
        if (isCreating()) {
            saveType = ProductOrder.SaveType.CREATING;
        }
        editOrder.prepareToSave(userBean.getBspUser(), saveType);

        if (editOrder.isDraft()) {
            // mlc isDraft checks if the status is Draft and if so, we set it to Draft again?
            editOrder.setOrderStatus(ProductOrder.OrderStatus.Draft);
        } else {
            productOrderEjb.updateJiraIssue(editOrder);
        }

        // Save it!
        productOrderDao.persist(editOrder);

        addMessage("Product Order \"{0}\" has been saved.", editOrder.getTitle());
        return createViewResolution(editOrder.getBusinessKey());
    }

    private void updateTokenInputFields() {
        // Set the project, product and addOns for the order.
        ResearchProject tokenProject = projectTokenInput.getTokenObject();
        ResearchProject project =
                tokenProject != null ? projectDao.findByBusinessKey(tokenProject.getBusinessKey()) : null;
        Product tokenProduct = productTokenInput.getTokenObject();
        Product product = tokenProduct != null ? productDao.findByPartNumber(tokenProduct.getPartNumber()) : null;
        List<Product> addOnProducts = productDao.findByPartNumbers(addOnKeys);
        editOrder.updateData(project, product, addOnProducts, stringToSampleList(sampleList));
        BspUser tokenOwner = owner.getTokenObject();
        editOrder.setCreatedBy(tokenOwner != null ? tokenOwner.getUserId() : null);

        // For sample initiation fields we will set token input fields.
        if (isSampleInitiation()) {
            Collection<PostReceiveOption> postReceiveOptions = PostReceiveOption.getByText(postReceiveOptionKeys);
            editOrder.getProductOrderKit().getPostReceiveOptions().clear();
            editOrder.getProductOrderKit().getPostReceiveOptions().addAll(postReceiveOptions);
            editOrder.getProductOrderKit().setSampleCollectionId(
                    bspGroupCollectionTokenInput.getTokenObject() != null ?
                            bspGroupCollectionTokenInput.getTokenObject().getCollectionId() : null);
            editOrder.getProductOrderKit().setSiteId(
                    bspShippingLocationTokenInput.getTokenObject() != null ?
                            bspShippingLocationTokenInput.getTokenObject().getId() : null);
            editOrder.getProductOrderKit().setNotificationIds(notificationListTokenInput.getTokenBusinessKeys());
        }
    }

    @HandlesEvent("downloadBillingTracker")
    public Resolution downloadBillingTracker() throws Exception {
        Resolution resolution =
                ProductOrderActionBean.getTrackerForOrders(
                        this, selectedProductOrders, priceItemDao, bspUserList, priceListCache);

        if (hasErrors()) {
            setupListDisplay();
        }

        return resolution;
    }

    /**
     * This method makes sure to read the stored preference AND then do the appropraite find for the list page. This
     * is because we need to regenerate the list so it's displayed along with the errors.
     *
     * @throws Exception Any errors.
     */
    private void setupListDisplay() throws Exception {
        setupSearchCriteria();
        listInit();
    }

    @HandlesEvent("startBilling")
    public Resolution startBilling() {
        List<String> errorMessages = new ArrayList<>();
        Set<LedgerEntry> ledgerItems =
                ledgerEntryDao.findWithoutBillingSessionByOrderList(selectedProductOrderBusinessKeys, errorMessages);

        // Add error messages
        addGlobalValidationErrors(errorMessages);

        if (CollectionUtils.isEmpty(ledgerItems)) {
            addGlobalValidationError("There are no items to bill on any of the selected orders");
            return new ForwardResolution(ProductOrderActionBean.class, LIST_ACTION);
        }

        if (hasErrors()) {
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
            } catch (JiraIssue.NoTransitionException | ProductOrderEjb.NoSuchPDOException |
                    ProductOrderEjb.SampleDeliveryStatusChangeException | IOException e) {
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
    @HandlesEvent("getPostReceiveOptions")
    public Resolution getPostReceiveOptions() throws Exception {
        JSONArray itemList = new JSONArray();
        productOrder = getContext().getRequest().getParameter(PRODUCT_ORDER_PARAMETER);

        boolean savedOrder = !StringUtils.isBlank(productOrder);
        MaterialInfo materialInfo = MaterialInfo.fromText(this.materialInfo);
        for (PostReceiveOption postReceiveOption : materialInfo.getPostReceiveOptions()) {
            JSONObject item = new JSONObject();
            item.put("key", postReceiveOption.getText());
            if (!savedOrder) {
                item.put("value", postReceiveOption.getDefaultToChecked());
            } else {
                item.put("value", false);
            }
            itemList.put(item);
        }

        return createTextResolution(itemList.toString());
    }

    @HandlesEvent("getSummary")
    public Resolution getSummary() throws Exception {
        JSONArray itemList = new JSONArray();
        if (editOrder != null) {
            List<String> comments = editOrder.getSampleSummaryComments();
            for (String comment : comments) {
                JSONObject item = new JSONObject();
                item.put("comment", comment);

                itemList.put(item);
            }
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
            ProductOrder.loadBspData(samples);

            for (ProductOrderSample sample : samples) {
                JSONObject item = new JSONObject();

                if (sample.isInBspFormat()) {
                    setupSampleDTOItems(sample, item);
                } else {
                    setupEmptyItems(sample, item);
                }

                itemList.put(item);
            }
        }

        return createTextResolution(itemList.toString());
    }

    private static void setupSampleDTOItems(ProductOrderSample sample, JSONObject item) throws JSONException {
        BSPSampleDTO bspSampleDTO = sample.getBspSampleDTO();

        item.put(BSPSampleDTO.SAMPLE_ID, sample.getProductOrderSampleId());
        item.put(BSPSampleDTO.COLLABORATOR_SAMPLE_ID, bspSampleDTO.getCollaboratorsSampleName());
        item.put(BSPSampleDTO.PATIENT_ID, bspSampleDTO.getPatientId());
        item.put(BSPSampleDTO.COLLABORATOR_PARTICIPANT_ID, bspSampleDTO.getCollaboratorParticipantId());
        item.put(BSPSampleDTO.VOLUME, bspSampleDTO.getVolume());
        item.put(BSPSampleDTO.CONCENTRATION, bspSampleDTO.getConcentration());
        item.put(BSPSampleDTO.JSON_RIN_KEY, bspSampleDTO.getRinScore());
        item.put(BSPSampleDTO.PICO_DATE, formatPicoRunDate(bspSampleDTO.getPicoRunDate()));
        item.put(BSPSampleDTO.TOTAL, bspSampleDTO.getTotal());
        item.put(BSPSampleDTO.HAS_FINGERPRINT, bspSampleDTO.getHasFingerprint());
        item.put(BSPSampleDTO.HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH, bspSampleDTO.getHasSampleKitUploadRackscanMismatch());
        item.put(BSPSampleDTO.COMPLETELY_BILLED, sample.isCompletelyBilled());

        LabEventSampleDTO labEventSampleDTO = sample.getLabEventSampleDTO();

        if (labEventSampleDTO != null) {
            item.put(BSPSampleDTO.PACKAGE_DATE, labEventSampleDTO.getSamplePackagedDate());
            item.put(BSPSampleDTO.RECEIPT_DATE, labEventSampleDTO.getSampleReceiptDate());
        } else {
            item.put(BSPSampleDTO.PACKAGE_DATE, "");
            item.put(BSPSampleDTO.RECEIPT_DATE, "");
        }
    }

    private static String formatPicoRunDate(Date picoRunDate) {
        if (picoRunDate == null) {
            return "No Pico";
        }

        return dateFormatter.format(picoRunDate);
    }

    private static void setupEmptyItems(ProductOrderSample sample, JSONObject item) throws JSONException {
        item.put(BSPSampleDTO.SAMPLE_ID, sample.getProductOrderSampleId());
        item.put(BSPSampleDTO.COLLABORATOR_SAMPLE_ID, "");
        item.put(BSPSampleDTO.PATIENT_ID, "");
        item.put(BSPSampleDTO.COLLABORATOR_PARTICIPANT_ID, "");
        item.put(BSPSampleDTO.VOLUME, "");
        item.put(BSPSampleDTO.CONCENTRATION, "");
        item.put(BSPSampleDTO.JSON_RIN_KEY, "");
        item.put(BSPSampleDTO.PICO_DATE, "");
        item.put(BSPSampleDTO.TOTAL, "");
        item.put(BSPSampleDTO.HAS_FINGERPRINT, "");
        item.put(BSPSampleDTO.HAS_SAMPLE_KIT_UPLOAD_RACKSCAN_MISMATCH, "");
        item.put(BSPSampleDTO.PACKAGE_DATE, "");
        item.put(BSPSampleDTO.RECEIPT_DATE, "");
    }

    @HandlesEvent("getSupportsSkippingQuote")
    public Resolution getSupportsSkippingQuote() throws Exception {
        boolean supportsSkippingQuote = false;
        JSONObject item = new JSONObject();
        if (!StringUtils.isEmpty(product)) {
            supportsSkippingQuote = productDao.findByBusinessKey(product).getSupportsSkippingQuote();
        }
        item.put(Product.SUPPORTS_SKIPPING_QUOTE, supportsSkippingQuote);
        return createTextResolution(item.toString());
    }

    @HandlesEvent("getSupportsNumberOfLanes")
    public Resolution getSupportsNumberOfLanes() throws Exception {
        boolean supportsNumberOfLanes = false;
        JSONObject item = new JSONObject();

        if (product != null) {
            supportsNumberOfLanes = productDao.findByBusinessKey(product).getSupportsNumberOfLanes();
        }

        item.put(BSPSampleDTO.SUPPORTS_NUMBER_OF_LANES, supportsNumberOfLanes);

        return createTextResolution(item.toString());
    }

    @ValidationMethod(on = "deleteOrder")
    public void validateDeleteOrder() {
        if (!editOrder.isDraft()) {
            addGlobalValidationError("Orders can only be deleted in draft mode");
        }
    }

    @ValidationMethod(on = {DELETE_SAMPLES_ACTION, ABANDON_SAMPLES_ACTION, SET_RISK, RECALCULATE_RISK, ADD_SAMPLES_TO_BUCKET}, priority = 0)
    public void validateSampleListOperation() {
        if (selectedProductOrderSampleIds != null) {
            selectedProductOrderSamples = new ArrayList<>(selectedProductOrderSampleIds.size());
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
                        sample.getName());
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
                    ProductOrderEjb.JiraTransition.DEVELOPER_EDIT.getStateName());
            productOrderEjb.updateOrderStatus(editOrder.getJiraTicketKey(), this);
        }
        return createViewResolution(editOrder.getBusinessKey());
    }


    @HandlesEvent(ADD_SAMPLES_TO_BUCKET)
    public Resolution addSamplesToBucket(){
        productOrderEjb.handleSamplesAdded(editOrder.getBusinessKey(), editOrder.getSamples(), this);
        return new ForwardResolution(ORDER_VIEW_PAGE);
    }

    @HandlesEvent(SET_RISK)
    public Resolution setRisk() throws Exception {

        productOrderEjb.setManualOnRisk(
                getUserBean().getBspUser(), editOrder.getBusinessKey(), selectedProductOrderSamples, riskStatus,
                riskComment);

        addMessage("Set manual on risk to {1} for {0} samples.", selectedProductOrderSampleIds.size(), riskStatus);
        JiraIssue issue = jiraService.getIssue(editOrder.getJiraTicketKey());
        issue.addComment(MessageFormat.format("{0} set manual on risk to {2} for {1} samples.",
                userBean.getLoginUserName(), selectedProductOrderSampleIds.size(), riskStatus));
        return createViewResolution(editOrder.getBusinessKey());
    }

    /**
     * Builds a List of ProductOrderSamples by finding
     * PDOSamples in the given pdo that have an id
     * that matches an id in the pdoIds list.
     */
    private List<ProductOrderSample> getSelectedProductOrderSamplesFor(ProductOrder pdo,
                                                                       Collection<Long> pdoIds) {
        List<ProductOrderSample> selectedPdoSamples = new ArrayList<> (getSelectedProductOrderSampleIds().size());
        for (ProductOrderSample pdoSample : editOrder.getSamples()) {
            if (pdoIds.contains(pdoSample.getProductOrderSampleId())) {
                selectedPdoSamples.add(pdoSample);
            }
        }
        return selectedPdoSamples;
    }

    @ValidationMethod(on = RECALCULATE_RISK)
    public void validateRiskRecalculation() {
        if (editOrder.isRinScoreValidationRequired()) {
            validateRinScores(getSelectedProductOrderSamplesFor(editOrder,getSelectedProductOrderSampleIds()));
        }
    }

    @HandlesEvent(RECALCULATE_RISK)
    public Resolution recalculateRisk() throws Exception {
        int originalOnRiskCount = editOrder.countItemsOnRisk();

        try {
            productOrderEjb.calculateRisk(editOrder.getBusinessKey(), selectedProductOrderSamples);

            // refetch the order to get updated risk status on the order.
            editOrder = productOrderDao.findByBusinessKey(editOrder.getBusinessKey());
            int afterOnRiskCount = editOrder.countItemsOnRisk();

            String fullString = addMessage(
                    "Successfully recalculated On Risk. {0} samples are on risk. Previously there were {1} samples on risk.",
                    afterOnRiskCount, originalOnRiskCount);
            JiraIssue issue = jiraService.getIssue(editOrder.getJiraTicketKey());
            issue.addComment(fullString);
        } catch (Exception ex) {
            addGlobalValidationError(ex.getMessage());
        }

        return createViewResolution(editOrder.getBusinessKey());
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
            if (!StringUtils.isBlank(abandonComment)) {
                sample.setSampleComment(abandonComment);
            }
        }

        if (!selectedProductOrderSamples.isEmpty()) {
            productOrderEjb.abandonSamples(editOrder.getJiraTicketKey(), selectedProductOrderSamples, abandonComment);
            addMessage("Abandoned samples: {0}.",
                    StringUtils.join(ProductOrderSample.getSampleNames(selectedProductOrderSamples), ", "));
            productOrderEjb.updateOrderStatus(editOrder.getJiraTicketKey(), this);
        }
        return createViewResolution(editOrder.getBusinessKey());
    }

    @HandlesEvent(ADD_SAMPLES_ACTION)
    public Resolution addSamples() throws Exception {
        List<ProductOrderSample> samplesToAdd = stringToSampleList(addSamplesText);
        productOrderEjb.addSamples(userBean.getBspUser(), editOrder.getJiraTicketKey(), samplesToAdd, this);
        return createViewResolution(editOrder.getBusinessKey());
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

    public List<ProductOrderListEntry> getDisplayedProductOrderListEntries() {
        return displayedProductOrderListEntries;
    }

    public ProductOrder getEditOrder() {
        return editOrder;
    }

    public void setEditOrder(ProductOrder editOrder) {
        this.editOrder = editOrder;
    }

    public String getQuoteUrl(String quoteIdentifier) {
        return quoteLink.quoteUrl(quoteIdentifier);
    }

    public String getQuoteUrl() {
        return getQuoteUrl(editOrder.getQuoteId());
    }

    public static Resolution getTrackerForOrders(
            final CoreActionBean actionBean,
            List<ProductOrder> productOrderList,
            PriceItemDao priceItemDao,
            BSPUserList bspUserList,
            PriceListCache priceListCache) {

        OutputStream outputStream = null;

        try {
            String filename =
                    "BillingTracker-" + AbstractSpreadsheetExporter.DATE_FORMAT
                            .format(Calendar.getInstance().getTime());

            // Colon is a metacharacter in Windows separating the drive letter from the rest of the path.
            filename = filename.replaceAll(":", "_");

            final File tempFile = File.createTempFile(filename, ".xlsx");
            outputStream = new FileOutputStream(tempFile);

            SampleLedgerExporter sampleLedgerExporter =
                    new SampleLedgerExporter(priceItemDao, bspUserList, priceListCache, productOrderList);
            sampleLedgerExporter.writeToStream(outputStream);
            IOUtils.closeQuietly(outputStream);

            return new Resolution() {
                @Override
                public void execute(HttpServletRequest request, HttpServletResponse response) throws Exception {
                    InputStream inputStream = new FileInputStream(tempFile);

                    try {
                        actionBean.setFileDownloadHeaders("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", tempFile.getName());
                        IOUtils.copy(inputStream, actionBean.getContext().getResponse().getOutputStream());
                    } finally {
                        IOUtils.closeQuietly(inputStream);
                        FileUtils.deleteQuietly(tempFile);
                    }
                }
            };
        } catch (Exception ex) {
            actionBean.addGlobalValidationError(
                    "Got an exception trying to download the billing tracker: " + ex.getMessage());
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

    public String getMaterialInfo() {
        return materialInfo;
    }

    public void setMaterialInfo(String materialInfo) {
        this.materialInfo = materialInfo;
    }

    @HandlesEvent("projectAutocomplete")
    public Resolution projectAutocomplete() throws Exception {
        return createTextResolution(projectTokenInput.getJsonString(getQ()));
    }

    @HandlesEvent("productAutocomplete")
    public Resolution productAutocomplete() throws Exception {
        return createTextResolution(productTokenInput.getJsonString(getQ()));
    }

    @HandlesEvent("shippingLocationAutocomplete")
    public Resolution shippingLocationAutocomplete() throws Exception {
        SampleCollection selectedCollection = bspGroupCollectionTokenInput.getTokenObject();
        if (selectedCollection != null) {
            return createTextResolution(
                    bspShippingLocationTokenInput.getJsonString(getQ(), selectedCollection));
        }
        return null;
    }

    @HandlesEvent("groupCollectionAutocomplete")
    public Resolution groupCollectionAutocomplete() throws Exception {
        return createTextResolution(bspGroupCollectionTokenInput.getJsonString(getQ()));
    }

    @HandlesEvent("anyUsersAutocomplete")
    public Resolution anyUsersAutocomplete() throws Exception {
        return createTextResolution(notificationListTokenInput.getJsonString(getQ()));
    }

    @HandlesEvent("collectionOrganisms")
    public Resolution collectionOrganisms() throws Exception {

        SampleCollection sampleCollection = bspGroupCollectionTokenInput.getTokenObject();

        JSONObject collectionAndOrganismsList = new JSONObject();
        if (sampleCollection != null) {
            Collection<Pair<Long, String>> organisms = sampleCollection.getOrganisms();

            collectionAndOrganismsList.put("collectionName", sampleCollection.getCollectionName());

            // Create the json array of items for the chunk
            JSONArray itemList = new JSONArray();
            collectionAndOrganismsList.put("organisms", itemList);

            for (Pair<Long, String> organism : organisms) {
                JSONObject item = new JSONObject();
                item.put("id", organism.getLeft());
                item.put("name", organism.getRight());

                itemList.put(item);
            }
        }

        return new StreamingResolution("text", new StringReader(collectionAndOrganismsList.toString()));
    }

    public List<String> getAddOnKeys() {
        return addOnKeys;
    }

    public void setAddOnKeys(List<String> addOnKeys) {
        this.addOnKeys = addOnKeys;
    }

    public List<String> getPostReceiveOptionKeys() {
        return postReceiveOptionKeys;
    }

    public void setPostReceiveOptionKeys(List<String> postReceiveOptionKeys) {
        this.postReceiveOptionKeys = postReceiveOptionKeys;
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
        // Unless we're in draft mode, or CREATING a new order, user must be logged into JIRA to
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
        List<ProductOrderSample> samples = new ArrayList<>();
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

    public BspShippingLocationTokenInput getBspShippingLocationTokenInput() {
        return bspShippingLocationTokenInput;
    }

    public BspGroupCollectionTokenInput getBspGroupCollectionTokenInput() {
        return bspGroupCollectionTokenInput;
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

    public String getRiskComment() {
        return riskComment;
    }

    public void setRiskComment(String riskComment) {
        this.riskComment = riskComment;
    }

    public String getAbandonComment() {
        return abandonComment;
    }

    public void setAbandonComment(String abandonComment) {
        this.abandonComment = abandonComment;
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
        return (productOrderListEntry != null) && productOrderListEntry.isReadyForBilling();
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
     * Convenience method to determine whether or not the current PDO is for sample initiation.
     *
     * @return true if this is a sample initiation PDO; false otherwise
     */
    public boolean isSampleInitiation() {
        return editOrder.getProduct() != null && editOrder.getProduct().isSampleInitiationProduct();
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
        }
        if (productOrderSampleDao.countSamplesWithLedgerEntries(editOrder) > 0) {
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
     * Convenience method for PDO view page to show number of samples abandoned for the current PDO.
     *
     * @return Number of sample abandoned.
     */
    public int getNumberAbandoned() {
        return progressFetcher.getNumberAbandoned(editOrder.getBusinessKey());
    }


    /**
     * Convenience method for PDO view page to show number of samples completed for the current PDO.
     *
     * @return Number of sample completed.
     */
    public int getNumberCompleted() {
        return progressFetcher.getNumberCompleted(editOrder.getBusinessKey());
    }


    /**
     * Convenience method for PDO view page to show number of samples in progress for the current PDO.
     *
     * @return Number of sample in progress.
     */
    public int getNumberInProgress() {
        return progressFetcher.getNumberInProgress(editOrder.getBusinessKey());
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
        } else {
            List<String> progressPieces = new ArrayList<>();
            if (getPercentAbandoned() != 0) {
                progressPieces.add(formatProgress(getNumberAbandoned(), getPercentAbandoned(), "Abandoned"));
            }
            if (getPercentCompleted() != 0) {
                progressPieces.add(formatProgress(getNumberCompleted(), getPercentCompleted(), "Completed"));
            }
            if (getPercentInProgress() != 0) {
                progressPieces.add(formatProgress(getNumberInProgress(), getPercentInProgress(), "In Progress"));
            }

            return StringUtils.join(progressPieces, ", ");
        }
    }

    /**
     * Format progress String
     *
     * @return Formatted progress string for display.
     */
    private String formatProgress(int number, int percentage, String identifier) {
        return String.format("%d %s (%d%%)", number, identifier, percentage);
    }

    public Map<String, Date> getProductOrderSampleReceiptDates() {
        return productOrderSampleReceiptDates;
    }

    public void setProductOrderSampleReceiptDates(Map<String, Date> productOrderSampleReceipts) {
        this.productOrderSampleReceiptDates = productOrderSampleReceipts;
    }

    /**
     * @return Reason that abandon button is disabled
     */
    public String getAbandonDisabledReason() {
        return abandonDisabledReason;
    }

    /**
     * Update reason why the abandon button is disabled.
     *
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
     *
     * @param abandonWarning Boolean flag used to determine whether we need to use special message confirmation
     */
    public void setAbandonWarning(boolean abandonWarning) {
        this.abandonWarning = abandonWarning;
    }

    public boolean isSupportsPico() {
        return (editOrder != null) && (editOrder.getProduct() != null) && editOrder.getProduct().isSupportsPico();
    }

    public boolean isSupportsRin() {
        return (editOrder != null) && (editOrder.getProduct() != null) && editOrder.getProduct().isSupportsRin();
    }

    public void setProductFamilies(List<ProductFamily> productFamilies) {
        this.productFamilies = productFamilies;
    }

    public Object getProductFamilies() {
        return productFamilies;
    }

    public Long getProductFamilyId() {
        return productFamilyId;
    }

    public void setProductFamilyId(Long productFamilyId) {
        this.productFamilyId = productFamilyId;
    }

    public List<ProductOrder.OrderStatus> getSelectedStatuses() {
        return selectedStatuses;
    }

    public void setSelectedStatuses(List<ProductOrder.OrderStatus> selectedStatuses) {
        this.selectedStatuses = selectedStatuses;
    }

    public List<ProductOrder.LedgerStatus> getSelectedLedgerStatuses() {
        return selectedLedgerStatuses;
    }

    public void setSelectedLedgerStatuses(List<ProductOrder.LedgerStatus> selectedLedgerStatuses) {
        this.selectedLedgerStatuses = selectedLedgerStatuses;
    }

    /**
     * @return Show the create title if this is a developer or PDM.
     */
    @Override
    public boolean isCreateAllowed() {
        return getUserBean().isDeveloperUser() || getUserBean().isPMUser() || getUserBean().isPDMUser();
    }

    /**
     * @return Show the edit title if this is not a draft (Drafts use a button per Product Owner workflow request) and
     *         the user is appropriate for editing.
     */
    @Override
    public boolean isEditAllowed() {
        return !editOrder.isDraft() && isCreateAllowed();
    }

    public ProductOrder.LedgerStatus[] getLedgerStatuses() {
        return ProductOrder.LedgerStatus.values();
    }

    public ProductOrder.OrderStatus[] getOrderStatuses() {
        return ProductOrder.OrderStatus.values();
    }

    public UserTokenInput getNotificationListTokenInput() {
        return notificationListTokenInput;
    }

    public void setNotificationListTokenInput(UserTokenInput notificationListTokenInput) {
        this.notificationListTokenInput = notificationListTokenInput;
    }

    public List<MaterialInfo> getDnaMatrixMaterialTypes() {
        return dnaMatrixMaterialTypes;
    }

    public String getWorkRequestUrl() {
        return bspConfig.getWorkRequestLink(editOrder.getProductOrderKit().getWorkRequestId());
    }

    /**
     * Only call this from a test!
     */
    void setProductDao(ProductDao productDao) {
        this.productDao = productDao;
    }

    private void validateSkipQuoteOptions() {
        if (skipQuote) {
            if (StringUtils.isEmpty(editOrder.getSkipQuoteReason())) {
                addValidationError("skipQuoteReason","When skipping a quote, please provide a quick explanation for why a quote cannot be entered.");
            }
            if (!StringUtils.isEmpty(editOrder.getQuoteId())) {
                // the JSP should make this situation impossible
                addValidationError("skipQuote","You have opted out of providing a quote, but you have also selected a quote.  Please un-check the quote opt out checkbox or clear the quote field.");
            }
        }
    }

    public void validateQuoteOptions(String action) {
        validateSkipQuoteOptions();
        if (!skipQuote) {
            requireField(editOrder.getQuoteId() != null, "a quote specified", action);
        }
    }

    public boolean isSkipQuote() {
        return skipQuote;
    }

    public void setSkipQuote(boolean skipQuote) {
        this.skipQuote = skipQuote;
    }

    public String getPostReceivedOptionsAsString() {
        return editOrder.getProductOrderKit().getPostReceivedOptionsAsString("<br/>");
    }
}
