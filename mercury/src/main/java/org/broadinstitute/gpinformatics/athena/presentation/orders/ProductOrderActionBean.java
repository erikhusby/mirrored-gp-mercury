package org.broadinstitute.gpinformatics.athena.presentation.orders;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import edu.mit.broad.bsp.core.datavo.workrequest.items.kit.MaterialInfo;
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
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.bsp.client.workrequest.SampleKitWorkRequest;
import org.broadinstitute.bsp.client.workrequest.kit.KitTypeAllowanceSpecification;
import org.broadinstitute.gpinformatics.athena.boundary.orders.CompletionStatusFetcher;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporter;
import org.broadinstitute.gpinformatics.athena.boundary.orders.SampleLedgerExporterFactory;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderListEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductFamilyDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.RegulatoryInfoDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.infrastructure.AccessItem;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.preference.NameValueDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingTrackerResolution;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SquidLink;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspGroupCollectionTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspShippingLocationTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataSourceResolver;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPKitRequestService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.cognos.OrspProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.OrspProject;
import org.broadinstitute.gpinformatics.infrastructure.cognos.entity.OrspProjectConsent;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryEnumUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.BucketException;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.BSPLookupException;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.DatatablesStateSaver;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.hibernate.Hibernate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jvnet.inflector.Noun;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.mercury.presentation.datatables.DatatablesStateSaver.SAVE_SEARCH_DATA;

/**
 * This handles all the needed interface processing elements.
 */
@SuppressWarnings("unused")
@UrlBinding(ProductOrderActionBean.ACTIONBEAN_URL_BINDING)
public class ProductOrderActionBean extends CoreActionBean {

    private static final Log logger = LogFactory.getLog(ProductOrderActionBean.class);

    public static final String ACTIONBEAN_URL_BINDING = "/orders/order.action";
    public static final String PRODUCT_ORDER_PARAMETER = "productOrder";
    public static final String REGULATORY_ID_PARAMETER = "selectedRegulatoryIds";

    private static final String PRODUCT_ORDER = "Product Order";
    public static final String CREATE_ORDER = CoreActionBean.CREATE + PRODUCT_ORDER;
    public static final String EDIT_ORDER = CoreActionBean.EDIT + PRODUCT_ORDER;

    private static final String ORDER_CREATE_PAGE = "/orders/create.jsp";
    private static final String ORDER_LIST_PAGE = "/orders/list.jsp";
    public static final String ORDER_VIEW_PAGE = "/orders/view.jsp";

    private static final String ADD_SAMPLES_ACTION = "addSamples";
    private static final String ABANDON_SAMPLES_ACTION = "abandonSamples";
    private static final String UNABANDON_SAMPLES_ACTION = "unAbandonSamples";
    private static final String DELETE_SAMPLES_ACTION = "deleteSamples";
    public static final String SQUID_COMPONENTS_ACTION = "createSquidComponents";
    private static final String SET_RISK = "setRisk";
    private static final String SET_PROCEED_OOS = "setProceedOos";
    private static final String RECALCULATE_RISK = "recalculateRisk";
    private static final String REPLACE_SAMPLES = "replaceSamples";
    protected static final String PLACE_ORDER_ACTION = "placeOrder";
    protected static final String VALIDATE_ORDER = "validate";
    private static final String PUBLISH_PDO_TO_SAP = "publishProductOrderToSAP";
    // Search field constants
    private static final String FAMILY = "productFamily";
    private static final String PRODUCT = "product";
    private static final String STATUS = "status";
    private static final String LEDGER_STATUS = "ledgerStatus";
    private static final String DATE = "date";
    private static final String OWNER = "owner";
    private static final String ADD_SAMPLES_TO_BUCKET = "addSamplesToBucket";
    private static final String CHOSEN_ORGANISM = "chosenOrganism";

    private static final String KIT_DEFINITION_INDEX = "kitDefinitionQueryIndex";
    private static final String COULD_NOT_LOAD_SAMPLE_DATA = "Could not load sample data";
    private String sampleSummary;
    private List<String> sampleColumns = Arrays.asList(
            BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID.columnName(),
            BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID.columnName(),
            BSPSampleSearchColumn.PARTICIPANT_ID.columnName(),
            BSPSampleSearchColumn.VOLUME.columnName(),
            BSPSampleSearchColumn.RECEIPT_DATE.columnName(),
            BSPSampleSearchColumn.PICO_RUN_DATE.columnName(),
            BSPSampleSearchColumn.TOTAL_DNA.columnName(),
            BSPSampleSearchColumn.CONCENTRATION.columnName(),
            BSPSampleSearchColumn.MATERIAL_TYPE.columnName(), BSPSampleSearchColumn.RACKSCAN_MISMATCH.columnName(),
            "On Risk",
            "Proceed OOS",
            "Yield Amount");

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
    private BillingSessionDao billingSessionDao;

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private PreferenceEjb preferenceEjb;

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private ProductOrderListEntryDao orderListEntryDao;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private SquidLink squidLink;

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

    @Inject
    private SampleLedgerExporterFactory sampleLedgerExporterFactory;

    @Inject
    private SampleDataSourceResolver sampleDataSourceResolver;

    @Inject
    private QuoteService quoteService;

    private PriceListCache priceListCache;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private OrspProjectDao orspProjectDao;

    @Inject
    private SapIntegrationService sapService;

    private List<ProductOrderListEntry> displayedProductOrderListEntries;

    private String sampleList;
    private String replacementSampleList;

    @Validate(required = true, on = {EDIT_ACTION})
    private String productOrder;

    private List<Long> sampleIdsForGetBspData;

    private CompletionStatusFetcher progressFetcher;

    private boolean skipRegulatoryInfo;

    private GenotypingChip genotypingChip;

    private GenotypingProductOrderMapping genotypingProductOrderMapping;

    private Map<String, AttributeDefinition> pdoSpecificDefinitions = null;

    private Map<String, String> attributes = new HashMap<>();
    private HashMap<String, String> chipDefaults = new HashMap<>();


    /*
     * Due to certain items (namely as a result of token input fields) not properly being bound during the validation
     * phase, we are moving annotation based validations to be specifically called out in the code after updating the
     * token input fields
     */
    private ProductOrder editOrder;

    private ProductOrder replacementSampleOrder;

    // For create, we can also have a research project key to default.
    private String researchProjectKey;

    private List<String> selectedProductOrderBusinessKeys;
    private List<ProductOrder> selectedProductOrders;

    @Validate(required = true,
            on = {ABANDON_SAMPLES_ACTION, DELETE_SAMPLES_ACTION, ADD_SAMPLES_TO_BUCKET, UNABANDON_SAMPLES_ACTION})
    private List<Long> selectedProductOrderSampleIds;
    private List<ProductOrderSample> selectedProductOrderSamples;

    // used only as part of ajax call to get funds remaining.  Quote field is bound to editOrder.
    private String quoteIdentifier;

    private String product;
    private String selectedAddOns;

    /*
     * Used for Ajax call to retrieve post receive Options for a particular material info
     */
    private String materialInfo;

    private List<String> addOnKeys = new ArrayList<>();
    private Map<Integer, List<String>> postReceiveOptionKeys = new HashMap<>();

    @Validate(required = true, on = ADD_SAMPLES_ACTION)
    private String addSamplesText;

    @Validate(required = true, on = SET_RISK)
    private boolean riskStatus = true;

    @Validate(required = true, on = SET_RISK)
    private String riskComment;

    @Validate(required = true, on = SET_PROCEED_OOS)
    private ProductOrderSample.ProceedIfOutOfSpec proceedOos;

    private String abandonComment;
    private String unAbandonComment;

    // This is used for prompting why the abandon button is disabled.
    private String abandonDisabledReason;

    // This is used to determine whether a special warning message needs to be confirmed before normal abandon.
    private boolean abandonWarning;

    // Single {@link ProductOrderListEntry} for the view page, gives us billing session information.
    private ProductOrderListEntry productOrderListEntry;

    private Long productFamilyId;

    private Set<ProductOrder.OrderStatus> selectedStatuses = Collections.emptySet();

    private List<ProductOrderListEntry.LedgerStatus> selectedLedgerStatuses = Collections.emptyList();

    private static final List<MaterialInfo> dnaMatrixMaterialTypes =
            Arrays.asList(KitTypeAllowanceSpecification.DNA_MATRIX_KIT.getMaterialInfo());

    private KitType chosenKitType = KitType.DNA_MATRIX;

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
    DatatablesStateSaver preferenceSaver;
    String tableState="";

    @Inject
    private LabVesselDao labVesselDao;

    private RegulatoryInfoDao regulatoryInfoDao;

    private Map<String, Date> productOrderSampleReceiptDates;

    private List<ProductOrderKitDetail> kitDetails = new ArrayList<>();

    private String[] deletedKits = new String[0];

    /**
     * For use with the Ajax to indicate and pass back which kit definition is being searched for.
     */
    private String kitDefinitionQueryIndex;
    private String prePopulatedOrganismId;
    private String prepopulatePostReceiveOptions;

    /**
     * @return the required confirmation message for IRB attestation.
     */
    // Keep non-static so it can be easily called from JSP.
    public String getAttestationMessage() {
        return "By checking this box, I attest that I am fully aware of the regulatory requirements for this project, "
               + "that these requirements have been met (e.g. review by an IRB or the Broad's Office of Research "
               + "Subject Protection), and that the information provided above is accurate. Disregard of relevant "
               + "regulatory requirements and/or falsification of information may lead to quarantining of data. "
               + "If you have any questions regarding the federal regulations associated with your project, "
               + "please contact orsp@broadinstitute.org.";
    }

    public String getComplianceStatement() {
        return String.format(ResearchProject.REGULATORY_COMPLIANCE_STATEMENT, "this order involves");
    }

    /**
     * @return the list of role names that can modify the order being edited.
     */
    public String[] getModifyOrderRoles() {
        if (editOrder.isPending()) {
            // Allow PMs to modify Pending orders.
            return Role.roles(Role.Developer, Role.PDM, Role.PM);
        }
        return Role.roles(Role.Developer, Role.PDM);
    }

    /**
     * Given a product order, create an external link back to the application's View Details page for that order.
     */
    public static String getProductOrderLink(ProductOrder productOrder, AppConfig appConfig) {
        List<NameValuePair> parameters = new ArrayList<>();
        parameters.add(new BasicNameValuePair(CoreActionBean.VIEW_ACTION, ""));
        parameters.add(new BasicNameValuePair(PRODUCT_ORDER_PARAMETER, productOrder.getBusinessKey()));
        return appConfig.getUrl() + ACTIONBEAN_URL_BINDING + "?" + URLEncodedUtils
                .format(parameters, CharEncoding.UTF_8);
    }

    private List<Long> selectedRegulatoryIds = new ArrayList<>();

    public List<Long> getSelectedRegulatoryIds() {
        return selectedRegulatoryIds;
    }

    public void setSelectedRegulatoryIds(List<Long> selectedRegulatoryIds) {
        this.selectedRegulatoryIds = selectedRegulatoryIds;
    }

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
                progressFetcher = new CompletionStatusFetcher(
                        productOrderDao.getProgress(Collections.singleton(editOrder.getProductOrderId())));
            }
        } else {
            // If this was a create with research project specified, find that.
            // This is only used for save, when creating a new product order.
            editOrder = new ProductOrder();
        }
    }

    protected Map<String, Collection<RegulatoryInfo>> setupRegulatoryInformation(ResearchProject researchProject) {
        Map<String, Collection<RegulatoryInfo>> projectRegulatoryMap = new HashMap<>();
        projectRegulatoryMap.put(researchProject.getTitle(), researchProject.getRegulatoryInfos());
        for (ResearchProject project : researchProject.getAllParents()) {
            projectRegulatoryMap.put(project.getTitle(), project.getRegulatoryInfos());
        }
        return projectRegulatoryMap;
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, SAVE_SEARCH_DATA})
    public void initPreferenceSaver(){
        preferenceSaver.setPreferenceType(PreferenceType.PRODUCT_ORDER_PREFERENCES);
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
                List<Long> productOrderIds = new ArrayList<>();
                productOrderIds.add(editOrder.getProductOrderId());
                if (CollectionUtils.isNotEmpty(editOrder.getChildOrders())) {
                    for (ProductOrder childOrder : editOrder.getChildOrders()) {
                        productOrderIds.add(childOrder.getProductOrderId());
                    }
                }
                progressFetcher = new CompletionStatusFetcher(productOrderDao.getProgress(productOrderIds));
            }
        }
    }

    @Before(stages = LifecycleStage.EventHandling, on = SAVE_ACTION)
    public void initRegulatoryParameter() {

        if (editOrder.isRegulatoryInfoEditAllowed()) {
            if (selectedRegulatoryIds != null) {
                List<RegulatoryInfo> selectedRegulatoryInfos = regulatoryInfoDao
                        .findListByList(RegulatoryInfo.class, RegulatoryInfo_.regulatoryInfoId, selectedRegulatoryIds);

                editOrder.setRegulatoryInfos(selectedRegulatoryInfos);
            } else {
                editOrder.getRegulatoryInfos().clear();
            }
        }
    }

    /**
     * place the kit details into an array list to allow stripes to update the kit details
     */
    private void initializeKitDetails() {
        kitDetails.addAll(editOrder.getProductOrderKit().getKitOrderDetails());
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

        /*
         * update or add to list of kit details
         * Due to the kit Details being stored as a set in the ProductOrderKit entity, it was currently not possible
         * to have Stripes directly update the collection of kit details.  Therefore, a temporary list of kit details
         * are initialized when the page is loaded, and parsed when the page is submitted.
         */
        for (int kitDetailIndex = 0; kitDetailIndex < kitDetails.size(); kitDetailIndex++) {

            if (kitDetails.get(kitDetailIndex) != null &&
                postReceiveOptionKeys.get(kitDetailIndex) != null) {

                Set<PostReceiveOption> selectedOptions = new HashSet<>();

                for (String selectedPostReceiveOption : postReceiveOptionKeys.get(kitDetailIndex)) {
                    selectedOptions.add(PostReceiveOption.valueOf(selectedPostReceiveOption));
                }

                kitDetails.get(kitDetailIndex)
                        .setPostReceiveOptions(selectedOptions);
            }
        }

        if (StringUtils.isNotBlank(editOrder.getComments()) && editOrder.getComments().length() > 2000) {
            addValidationError("comments", "Description field cannot exceed 2000 characters");
        }
        if (StringUtils.isBlank(editOrder.getName()) || editOrder.getName().length() > 255) {
            addValidationError("title", "Name is required and cannot exceed 255 characters");
        }
        if (editOrder.canSkipQuote() && editOrder.getSkipQuoteReason().length() > 255) {
            addValidationError("skipQuoteReason", "Reason for Quote cannot exceed 255 characters");
        }
        if (editOrder.getProductOrderKit() != null &&
            StringUtils.isNotBlank(editOrder.getProductOrderKit().getComments()) &&
            editOrder.getProductOrderKit().getComments().length() > 255) {
            addValidationError("productOrderKit.comments", "Product order kit comments cannot exceed 255 characters");
        }

        // If this is not a draft, some fields are required.
        if (!editOrder.isDraft()) {

            doValidation(SAVE_ACTION);
        } else {
            // Even in draft, created by must be set. This can't be checked using @Validate (yet),
            // since its value isn't set until updateTokenInputFields() has been called.
            requireField(editOrder.getCreatedBy(), "an owner", "save");
            validateQuoteOptions(SAVE_ACTION);
            validateRegulatoryInformation(SAVE_ACTION);
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
            addGlobalValidationError("Cannot {2} ''{3}'' because it does not have {4}.",
                    MercuryStringUtils.splitCamelCase(action), editOrder.getName(), name);
        }
    }

    /**
     * Validates the rin scores for every
     * sample in the pdo.
     *
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
            try {
                if (pdoSample.isInBspFormat() && !pdoSample.canRinScoreBeUsedForOnRiskCalculation()) {
                    addGlobalValidationError(
                            "RIN '" + pdoSample.getSampleData().getRawRin() + "' for " + pdoSample.getName() +
                            " isn't a number." + "  Please correct this by going to BSP -> Utilities -> Upload Sample " +
                            "Annotation and updating the 'RIN Number' annotation for this sample.");
                }
            } catch (BSPLookupException e) {
                addGlobalValidationError(
                        pdoSample.getName() + " isn't a sample that is found in BSP.  For this reason the RIN score "
                        + "cannot be validated at this time");
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
        if (!editOrder.isSampleInitiation()) {
            requireField(!editOrder.getSamples().isEmpty(), "any samples", action);
        } else if (action.equals(SAVE_ACTION)) {
            doSaveValidation(action, researchProject);
        }

        requireField(researchProject, "a Research Project", action);

        if (editOrder.isRegulatoryInfoEditAllowed()) {
            validateRegulatoryInformation(action);
        }

        validateQuoteOptions(action);

        requireField(editOrder.getProduct(), "a product", action);

        if (editOrder.getProduct() != null && editOrder.getProduct().getSupportsNumberOfLanes()) {
            requireField(editOrder.getLaneCount() > 0, "a specified number of lanes", action);
        }

        String quoteId = editOrder.getQuoteId();
        Quote quote = validateQuoteId(quoteId);
        try {
            ProductOrder.checkQuoteValidity(editOrder, quote);
            for (FundingLevel fundingLevel : quote.getQuoteFunding().getFundingLevel()) {
                final Funding funding = fundingLevel.getFunding();
                if(funding.getFundingType().equals(Funding.FUNDS_RESERVATION)) {
                    final int numDaysBetween =
                            DateUtils.getNumDaysBetween(new Date(), funding.getGrantEndDate());
                    if(numDaysBetween > 0 && numDaysBetween < 45) {
                        addMessage("The Funding Source "+funding.getDisplayName()+" on " +
                                   quote.getAlphanumericId() + "  Quote expires in " + numDaysBetween +
                                   " days. If it is likely this work will not be completed by then, please work on "
                                   + "updating the Funding Source so Billing Errors can be avoided.");
                    }
                }
            }

            if(productOrderEjb.isOrderEligibleForSAP(editOrder)) {
                validateQuoteDetails(quote, ErrorLevel.ERROR, !editOrder.hasJiraTicketKey(), 0);
            }
        } catch (QuoteServerException e) {
            addGlobalValidationError("The quote ''{2}'' is not valid: {3}", quoteId, e.getMessage());
        } catch (QuoteNotFoundException e) {
            addGlobalValidationError("The quote ''{2}'' was not found ", quoteId);
        } catch (InvalidProductException e) {
            addGlobalValidationError("Unable to determine the existing value of open orders for " +
                                     quote.getAlphanumericId() +": " +e.getMessage());
        }

        if (editOrder != null) {
            validateRinScores(editOrder);
        }
    }

    /**
     * Determines if there is enough funds available on a quote to do any more work based on unbilled samples and funds
     * remaining on the quote
     *
     * @param quoteId   Identifier for The quote which the user intends to use.  From this we can determine the
     *                  collection of orders to include in evaluating and the funds remaining
     * @param errorLevel indicator for if the user should see a validation error or just a warning
     * @param countOpenOrders indicator for if the current order should be added.  Typically used if it is Draft or
     *                        Pending
     */
    private void validateQuoteDetails(String quoteId, final ErrorLevel errorLevel, boolean countOpenOrders)
            throws InvalidProductException {
        Quote quote = validateQuoteId(quoteId);

        if (quote != null) {
            validateQuoteDetails(quote, errorLevel, countOpenOrders, 0);
        }
    }

    /**
     * Determines if there is enough funds available on a quote to do any more work based on unbilled samples and funds
     * remaining on the quote.  This particular method will also consider Samples added on the page but not yet
     * refelcted on the order.
     *
     * The scenario for this is when the user clicks on the "Add Samples" button of a placed order
     *
     * @param quoteId   Identifier for The quote which the user intends to use.  From this we can determine the
     *                  collection of orders to include in evaluating and the funds remaining
     * @param errorLevel indicator for if the user should see a validation error or just a warning
     * @param countOpenOrders indicator for if the current order should be added.  Typically used if it is Draft or
     *                        Pending
     * @param additionalSamplesCount Number of extra samples to be considered which are not currently
     */
    private void validateQuoteDetailsWithAddedSamples(String quoteId, final ErrorLevel errorLevel,
                                                      boolean countOpenOrders, int additionalSamplesCount)
            throws InvalidProductException, QuoteServerException {
        Quote quote = validateQuoteId(quoteId);
        ProductOrder.checkQuoteValidity(editOrder, quote);
        if (quote != null) {
            validateQuoteDetails(quote, errorLevel, countOpenOrders, additionalSamplesCount);
        }
    }

    /**
     * Determines if there is enough funds available on a quote to do any more work based on unbilled samples and funds
     * remaining on the quote
     *
     * @param quote  The quote which the user intends to use.  From this we can determine the collection of orders to
     *               include in evaluating and the funds remaining
     * @param errorLevel indicator for if the user should see a validation error or just a warning
     * @param countCurrentUnPlacedOrder indicator for if the current order should be added.  Typically used if it is Draft or
     *                        Pending
     * @param additionalSampleCount
     */
    private void validateQuoteDetails(Quote quote, ErrorLevel errorLevel, boolean countCurrentUnPlacedOrder,
                                      int additionalSampleCount) throws InvalidProductException {
        if (!quote.getApprovalStatus().equals(ApprovalStatus.FUNDED)) {
            String unFundedMessage = "A quote should be funded in order to be used for a product order.";
            addMessageBasedOnErrorLevel(errorLevel, unFundedMessage);
        }

        double fundsRemaining = Double.parseDouble(quote.getQuoteFunding().getFundsRemaining());
        double outstandingEstimate = estimateOutstandingOrders(quote.getAlphanumericId(), quote);
        double valueOfCurrentOrder = 0;
        if(countCurrentUnPlacedOrder) {
            valueOfCurrentOrder = getValueOfOpenOrders(Collections.singletonList((editOrder.isChildOrder())?editOrder.getParentOrder():editOrder), quote);
        } else if(additionalSampleCount > 0) {
            valueOfCurrentOrder = getOrderValue((editOrder.isChildOrder())?editOrder.getParentOrder():editOrder, additionalSampleCount, quote);
        }

        if (fundsRemaining <= 0d ||
            (fundsRemaining < (outstandingEstimate+valueOfCurrentOrder))) {
            String inssuficientFundsMessage = "Insufficient funds are available on " + quote.getName() + " to place a new Product order";
            addMessageBasedOnErrorLevel(errorLevel, inssuficientFundsMessage);
        }
    }

    /**
     * Helper to determine if the returned message will be considered an full on validation error or simply a warning
     * @param errorLevel Enum to trigger the logic for what message level is desired
     * @param multiFundingLevelMessage Message to display to the user
     */
    private void addMessageBasedOnErrorLevel(ErrorLevel errorLevel, String multiFundingLevelMessage) {
        switch (errorLevel) {
        case ERROR:
            addGlobalValidationError(multiFundingLevelMessage);
            break;
        case WARNING:
            addMessage("WARNING: " +multiFundingLevelMessage);
            break;
        }
    }

    /**
     * Retrieves and determines the monitary value of a subset of Open Orders within Mercury
     * @param quoteId Common quote id to be used to determine which open orders will be found
     * @return total dollar amount of the monitary value of orders associated with the given quote
     */
    double estimateOutstandingOrders(String quoteId, Quote foundQuote) throws InvalidProductException {

        List<ProductOrder> ordersWithCommonQuote = productOrderDao.findOrdersWithCommonQuote(quoteId);

        return getValueOfOpenOrders(ordersWithCommonQuote, foundQuote);
    }

    /**
     * Determines the total monitary value of all unbilled samples on a given list of orders
     *
     * @param ordersWithCommonQuote Subset of orders for which the monitary value is to be determined
     * @param quote
     * @return Total dollar amount which equates to the monitary value of all orders given
     */
    double getValueOfOpenOrders(List<ProductOrder> ordersWithCommonQuote, Quote quote) throws InvalidProductException {
        double value = 0d;

        Set<ProductOrder> justParents = new HashSet<>();
        for (ProductOrder order : ordersWithCommonQuote) {
            if(order.isChildOrder()) {
                justParents.add(order.getParentOrder());
            } else {
                justParents.add(order);
            }
        }

        for (ProductOrder testOrder : justParents) {
            value += getOrderValue(testOrder, testOrder.getUnbilledSampleCount(), quote);
        }
        return value;
    }

    /**
     * Helper method to consolidate the code for evaluating the monitary value of an order based on the price associated
     * with its product(s) and the count of the unbilled samples
     * @param testOrder Product order for which we wish to determine the monitary value
     * @param sampleCount unbilled sample count to use for determining the order value.  Passed in separately to account
     *                    for the scenario when we do not want to use the sample count on the order but a sample count
     *                    that will potentially be on the order.
     * @param quote
     * @return Total monitary value of the order
     */
    double getOrderValue(ProductOrder testOrder, int sampleCount, Quote quote) throws InvalidProductException {
        double value = 0d;
        if(testOrder.getProduct() != null) {
            try {
                final Product product = testOrder.getProduct();
                double productValue =
                        getProductValue((product.getSupportsNumberOfLanes())?testOrder.getLaneCount():sampleCount, product,
                                quote);
                value += productValue;
                for (ProductOrderAddOn testOrderAddon : testOrder.getAddOns()) {
                    final Product addOn = testOrderAddon.getAddOn();
                    double addOnValue =
                            getProductValue((addOn.getSupportsNumberOfLanes())?testOrder.getLaneCount():sampleCount, addOn,
                                    quote);
                    value += addOnValue;
                }
            } catch (InvalidProductException e) {
                throw new InvalidProductException("For " + testOrder.getBusinessKey() + ": " + testOrder.getName() +
                " " + e.getMessage(), e);
            }
        }
        return value;
    }

    /**
     * Based on a product and a sample count, this method will determine the monitary value for the sake of evaluating
     * the monitary value of an order
     *
     * @param unbilledCount count of samples that have not yet been billed
     * @param product Product from which the price can be determined
     * @param quote
     * @return Derived value of the Product price multiplied by the number of unbilled samples
     */
    double getProductValue(int unbilledCount, Product product, Quote quote) throws InvalidProductException {
        double productValue = 0d;
        String foundPrice;
        try {
            foundPrice = priceListCache.getEffectivePrice(product.getPrimaryPriceItem(), quote);
        } catch (InvalidProductException e) {
            throw new InvalidProductException("For '" + product.getPartNumber() + "' " + e.getMessage(), e);
        }

        if (StringUtils.isNotBlank(foundPrice)) {
            Double productPrice = Double.valueOf(foundPrice);

            productValue = productPrice * (unbilledCount);
        } else {
            throw new InvalidProductException("Price for " + product.getPrimaryPriceItem().getDisplayName() + " for product "+
                                              product.getPartNumber()+" was not found.");
        }
        return productValue;
    }

    private void doSaveValidation(String action, ResearchProject researchProject) {
        // If this is not a draft and a sample initiation order, reset the kit details to the properly selected details
        if (!editOrder.isDraft()) {
            kitDetails.clear();
            initializeKitDetails();
        }

        requireField(!kitDetails.isEmpty(), "kit details", action);
        for (ProductOrderKitDetail kitDetail : kitDetails) {
            Long numberOfSamples = kitDetail.getNumberOfSamples();
            requireField(numberOfSamples != null && numberOfSamples > 0, "a specified number of samples",
                    action);
            requireField(kitDetail.getKitType().getKitName(), "a kit type", action);
            requireField(kitDetail.getBspMaterialName(), "a material information", action);
            requireField(kitDetail.getOrganismId(), "an organism", action);
        }
        requireField(editOrder.getProductOrderKit().getSiteId(), "a shipping location", action);
        requireField(editOrder.getProductOrderKit().getSampleCollectionId(), "a collection", action);

        // Avoid NPE if Research Project isn't set yet.
        if (researchProject != null) {
            requireField(researchProject.getBroadPIs().length > 0,
                    "a Research Project with a primary investigator", action);
            requireField(researchProject.getExternalCollaborators().length > 0,
                    "a Research Project with an external collaborator", action);
        }

        validateTransferMethod(editOrder);
    }

    public void validateTransferMethod(@Nonnull ProductOrder pdo) {
        if (pdo.getProductOrderKit().getTransferMethod() == SampleKitWorkRequest.TransferMethod.PICK_UP) {
            String validationMessage = String.format("a notification list, which required when \"%s\" is selected.",
                    SampleKitWorkRequest.TransferMethod.PICK_UP.getValue());
            requireField(pdo.getProductOrderKit().getNotificationIds().length > 0, validationMessage, SAVE_ACTION);
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

    @ValidationMethod(on = PLACE_ORDER_ACTION)
    public void validatePlacedOrder() {
        Hibernate.initialize(editOrder.getChildOrders());
        Hibernate.initialize(editOrder.getSapReferenceOrders());
        Hibernate.initialize(editOrder.getSamples());
        Hibernate.initialize(editOrder.getParentOrder());

        validatePlacedOrder(PLACE_ORDER_ACTION);
    }

    public void validatePlacedOrder(String action) {
        doValidation(action);
        if (!hasErrors()) {
            doOnRiskUpdate();

            // doOnRiskUpdate() can add errors, so check again.
            if (!hasErrors()) {
                updateFromInitiationTokenInputs();
            }
        }
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
            selectedProductOrders = productOrderDao.findListForBilling(selectedProductOrderBusinessKeys);

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
                preferenceDao.getPreferences(getUserBean().getBspUser().getUserId(), PreferenceType.PDO_SEARCH);

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
                logger.error("Could not read user preference on search for product orders.", t);
                addMessage("Could not read search preference");
            }
        }
    }

    private void populateSearchDefaults() {
        selectedStatuses = EnumSet.noneOf(ProductOrder.OrderStatus.class);
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
        selectedLedgerStatuses = ProductOrderListEntry.LedgerStatus.getFromNames(preferenceData.get(LEDGER_STATUS));
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

        progressFetcher = new CompletionStatusFetcher(productOrderDao
                .getProgress(ProductOrderListEntry.getProductOrderIDs(displayedProductOrderListEntries)));

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

        definitionValue.put(STATUS, MercuryEnumUtils.convertToStrings(selectedStatuses));

        definitionValue.put(LEDGER_STATUS, MercuryEnumUtils.convertToStrings(selectedLedgerStatuses));

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
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = EDIT_ACTION)
    public void initSampleKitInfo() {
        postReceiveOptionKeys.clear();
        int detailIndex = 0;
        initializeKitDetails();
        for (ProductOrderKitDetail kitDetail : kitDetails) {
            postReceiveOptionKeys.put(detailIndex++,
                    MercuryEnumUtils.convertToStrings(kitDetail.getPostReceiveOptions()));
        }
    }

    // All actions that can result in the view page loading (either by a validation error or view itself)
    @After(stages = LifecycleStage.BindingAndValidation,
            on = {EDIT_ACTION, VIEW_ACTION, ADD_SAMPLES_ACTION, SET_RISK, RECALCULATE_RISK, ABANDON_SAMPLES_ACTION,
                    DELETE_SAMPLES_ACTION, PLACE_ORDER_ACTION, VALIDATE_ORDER, UNABANDON_SAMPLES_ACTION, REPLACE_SAMPLES})
    public void entryInit() {
        if (editOrder != null) {
            editOrder.loadSampleData();
            productOrderListEntry = editOrder.isDraft() ? ProductOrderListEntry.createDummy() :
                    orderListEntryDao.findSingle(editOrder.getJiraTicketKey());

            ProductOrder.loadLabEventSampleData(editOrder.getSamples());

            sampleDataSourceResolver.populateSampleDataSources(editOrder);
            populateAttributes(editOrder.getJiraTicketKey());
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
                Quote quote = quoteService.getQuoteByAlphaId(quoteIdentifier);
                double fundsRemaining = Double.parseDouble(quote.getQuoteFunding().getFundsRemaining());
                item.put("fundsRemaining", NumberFormat.getCurrencyInstance().format(fundsRemaining));
                item.put("status", quote.getApprovalStatus().getValue());

                double outstandingOrdersValue = estimateOutstandingOrders(quoteIdentifier, quote);
                item.put("outstandingEstimate",  NumberFormat.getCurrencyInstance().format(
                        outstandingOrdersValue));
                JSONArray fundingDetails = new JSONArray();

                for (FundingLevel fundingLevel : quote.getQuoteFunding().getFundingLevel()) {
                    if(fundingLevel.getFunding().getFundingType().equals(Funding.FUNDS_RESERVATION)) {
                        JSONObject fundingInfo = new JSONObject();
                        fundingInfo.put("grantTitle", fundingLevel.getFunding().getDisplayName());
                        fundingInfo.put("grantEndDate",
                                DateUtils.getDate(fundingLevel.getFunding().getGrantEndDate()));
                        fundingInfo.put("grantNumber", fundingLevel.getFunding().getGrantNumber());
                        fundingInfo.put("grantStatus", fundingLevel.getFunding().getGrantStatus());

                        final Date today = new Date();
                        fundingInfo.put("activeGrant", (fundingLevel.getFunding().getGrantEndDate() != null &&
                                                        fundingLevel.getFunding().getGrantEndDate().after(today)));
                        fundingInfo.put("daysTillExpire",
                                DateUtils.getNumDaysBetween(today, fundingLevel.getFunding().getGrantEndDate()));
                        fundingDetails.put(fundingInfo);
                    }
                }
                item.put("fundingDetails", fundingDetails);
            }

        } catch (Exception ex) {
            try {
                item.put("error", "Unable to complete evaluating order values:  " + ex.getMessage());
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

                // FIXME?  This seems to cause the search preferences to not get loaded on the resulting list page.
                // Because of this, the user is presented witha  list page with no search results.  If they click
                // 'search' from that blank page, it would inadvertantly wipe out their previously defined search
                // preferences
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

        for (ProductOrderKitDetail kitDetail : productOrderKit.getKitOrderDetails()) {
            kitDetail.setOrganismName(null);
        }

        SampleCollection sampleCollection = bspGroupCollectionTokenInput.getTokenObject();
        if (sampleCollection != null) {
            for (ProductOrderKitDetail kitDetail : productOrderKit.getKitOrderDetails()) {
                if (kitDetail.getOrganismId() != null) {
                    for (Pair<Long, String> organism : sampleCollection.getOrganisms()) {
                        if (kitDetail.getOrganismId().equals(organism.getLeft())) {
                            kitDetail.setOrganismName(organism.getRight());
                            break;
                        }
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
            String siteKey = editOrder.getProductOrderKit().getSiteId() != null ?
                    String.valueOf(editOrder.getProductOrderKit().getSiteId()) : null;
            bspShippingLocationTokenInput.setup(!StringUtils.isBlank(siteKey) ? new String[]{siteKey} : new String[0]);
        }

        notificationListTokenInput.setup(editOrder.getProductOrderKit().getNotificationIds());
    }

    @HandlesEvent(PUBLISH_PDO_TO_SAP)
    public Resolution publishProductOrderToSAP() throws SAPInterfaceException {
        MessageCollection placeOrderMessageCollection = new MessageCollection();

        productOrderEjb.publishProductOrderToSAP(editOrder,placeOrderMessageCollection, true);

        addMessages(placeOrderMessageCollection);
        return createViewResolution(editOrder.getBusinessKey());
    }

    @HandlesEvent(PLACE_ORDER_ACTION)
    public Resolution placeOrder() {
        String originalBusinessKey = editOrder.getBusinessKey();

        MessageCollection placeOrderMessageCollection = new MessageCollection();
        try {
            if (editOrder.isPending()) {
                // For a Pending order, we need to remove samples that haven't been received.
                productOrderEjb.removeNonReceivedSamples(editOrder, this);
            }

            productOrderEjb.placeProductOrder(editOrder.getProductOrderId(), originalBusinessKey,
                    placeOrderMessageCollection);

            addMessage("Product Order \"{0}\" has been placed", editOrder.getTitle());
            originalBusinessKey = null;

            productOrderEjb.publishProductOrderToSAP(editOrder, placeOrderMessageCollection, true);
            addMessages(placeOrderMessageCollection);

            /*
             While fixing GPLIM-4481 we came across a scenario that left certain collections on the product order
             uninitialized which caused a lazy initialization exception when they were access accessed on the
             orders/view.jsp page. For this reason the calls to Hibernate.initialize were added below.

             */
            Hibernate.initialize(editOrder.getChildOrders());
            Hibernate.initialize(editOrder.getSapReferenceOrders());

            productOrderEjb.handleSamplesAdded(editOrder.getBusinessKey(), editOrder.getSamples(), this);
            productOrderDao.persist(editOrder);

        } catch (Exception e) {

            // If we get here with an original business key, then clear out the session and refetch the order.
            if (originalBusinessKey != null) {
                productOrderDao.clear();
                editOrder = productOrderDao.findByBusinessKey(originalBusinessKey);
            }

            addGlobalValidationError(e.getMessage());

            // If the edit order is null for any reason, this is a mess, so just go back to the list page. This was
            // seen with a bad jira refresh, so it is a really rare case.
            if (editOrder == null) {
                // flash seems to only work for messages and it seems better to have the message appear than
                // have it silently jump to the list page.
                addMessage(e.getMessage());
                return new RedirectResolution(ProductOrderActionBean.class, LIST_ACTION).flash(this);
            }

            updateFromInitiationTokenInputs();

            logger.error("Error while placing an order for " + editOrder.getBusinessKey(), e);
            // Make sure ProductOrderListEntry is initialized if returning source page resolution.
            entryInit();
            return getSourcePageResolution();
        }

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

    @HandlesEvent(SQUID_COMPONENTS_ACTION)
    public Resolution createSquidComponents() {
        return new ForwardResolution(SquidComponentActionBean.class, SquidComponentActionBean.ENTER_COMPONENTS_ACTION);
    }

    @HandlesEvent(VALIDATE_ORDER)
    public Resolution validate() {
        validatePlacedOrder("validate");
        if (hasErrors()) {
            return new ForwardResolution(this.getClass(), EDIT_ACTION);
        }
        addMessage("Draft Order is valid and ready to be placed");

        // entryInit() must be called explicitly here since it does not run automatically with source page resolution
        // and the ProductOrderListEntry that provides billing data would otherwise be null.
        entryInit();

        return getSourcePageResolution();
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution save() throws Exception {

        MessageCollection saveOrderMessageCollection = new MessageCollection();

        // Update the modified by and created by, if necessary.
        ProductOrder.SaveType saveType = ProductOrder.SaveType.UPDATING;
        if (isCreating()) {
            saveType = ProductOrder.SaveType.CREATING;
        }

        if (editOrder.isRegulatoryInfoEditAllowed()) {
            updateRegulatoryInformation();
        }
        Set<String> deletedIdsConverted = new HashSet<>(Arrays.asList(deletedKits));
        try {
            productOrderEjb.persistProductOrder(saveType, editOrder, deletedIdsConverted, kitDetails,
                    saveOrderMessageCollection);
            if (isInfinium() && editOrder.getPipelineLocation() == null) {
                editOrder.setPipelineLocation(ProductOrder.PipelineLocation.US_CLOUD);
                productOrderDao.persist(editOrder);
            }
            addMessages(saveOrderMessageCollection);
            addMessage("Product Order \"{0}\" has been saved.", editOrder.getTitle());
        } catch (SAPInterfaceException e) {
            addGlobalValidationError(e.getMessage());
            getSourcePageResolution();
        }
        try {
            if(!productOrderEjb.isOrderEligibleForSAP(editOrder)) {
                validateQuoteDetails(editOrder.getQuoteId(), ErrorLevel.WARNING, !editOrder.hasJiraTicketKey());
            }
        } catch (QuoteServerException e) {
            addGlobalValidationError("The quote ''{2}'' is not valid: {3}", editOrder.getQuoteId(), e.getMessage());
        } catch (QuoteNotFoundException e) {
            addGlobalValidationError("The quote ''{2}'' was not found ", editOrder.getQuoteId());
        } catch (InvalidProductException ipe) {
            addGlobalValidationError("Unable to determine the existing value of open orders for " + editOrder.getQuoteId() +": " +ipe.getMessage());
        }
        if (chipDefaults != null && attributes != null) {
            if (!chipDefaults.equals(attributes)) {
                genotypingProductOrderMapping =
                        productOrderEjb.findOrCreateGenotypingChipProductOrderMapping(editOrder.getJiraTicketKey());
                if (genotypingProductOrderMapping != null) {
                    boolean foundChange = false;
                    for (ArchetypeAttribute existingAttribute : genotypingProductOrderMapping.getAttributes()) {
                        String newValue = attributes.get(existingAttribute.getAttributeName());
                        String oldValue = existingAttribute.getAttributeValue();
                        if (oldValue == null && newValue != null || oldValue != null && !oldValue.equals(newValue)) {
                            foundChange = true;
                            existingAttribute.setAttributeValue(newValue);
                        }
                    }
                    if (foundChange) {
                        attributeArchetypeDao.persist(genotypingProductOrderMapping);
                    }
                }
            }
        }
        return createViewResolution(editOrder.getBusinessKey());
    }

    @ValidationMethod(on = REPLACE_SAMPLES)
    public void validateReplacementSample() {

        final List<ProductOrderSample> replacementSamples = stringToSampleList(replacementSampleList);

        if(editOrder.getNumberForReplacement() <= 0 ) {
            addGlobalValidationError("There are no samples to replace.  If you must process samples, please open a new Product Order");
        } else if (replacementSamples.size() > editOrder.getNumberForReplacement()) {
            addGlobalValidationError("You are attempting to replace more samples than you are able to.  Please reduce the number samples entered");
        }
    }

    @HandlesEvent(REPLACE_SAMPLES)
    public Resolution replaceSamples() throws Exception {

        boolean shareSapOrder = false;
        MessageCollection saveOrderMessageCollection = new MessageCollection();

        Hibernate.initialize(editOrder.getChildOrders());
        Hibernate.initialize(editOrder.getSapReferenceOrders());
        Hibernate.initialize(editOrder.getSamples());

        final List<ProductOrderSample> replacementSamples = stringToSampleList(replacementSampleList);

        if(editOrder.isSavedInSAP() && editOrder.hasAtLeastOneBilledLedgerEntry() &&
           (editOrder.getTotalNonAbandonedCount(ProductOrder.CountAggregation.ALL) < editOrder.latestSapOrderDetail().getPrimaryQuantity()) ) {
            shareSapOrder = true;
        }
        ProductOrder.SaveType saveType = ProductOrder.SaveType.CREATING;
        replacementSampleOrder = ProductOrder.cloneProductOrder(editOrder, shareSapOrder);
        replacementSampleOrder.setSamples(replacementSamples);
        Set<String> deletedIdsConverted = new HashSet<>(Arrays.asList(deletedKits));
        try {
            productOrderEjb.persistClonedProductOrder(saveType, replacementSampleOrder, saveOrderMessageCollection);
            addMessages(saveOrderMessageCollection);
            addMessage("Product Order \"{0}\" has been saved.", replacementSampleOrder.getTitle());
        } catch (SAPInterfaceException e) {
            addGlobalValidationError(e.getMessage());
            return getSourcePageResolution();
        }

        return createViewResolution(replacementSampleOrder==null?editOrder.getBusinessKey():replacementSampleOrder.getBusinessKey());
    }

    void updateRegulatoryInformation() {
        if (!editOrder.getRegulatoryInfos().isEmpty() && !isSkipRegulatoryInfo()) {
            editOrder.setSkipRegulatoryReason(null);
        }
        if (isSkipRegulatoryInfo() && !StringUtils.isBlank(editOrder.getSkipRegulatoryReason())) {
            editOrder.getRegulatoryInfos().clear();
        }
    }

    private void updateTokenInputFields() {
        // Set the project, product and addOns for the order.
        ResearchProject tokenProject = projectTokenInput.getTokenObject();
        ResearchProject project =
                tokenProject != null ? researchProjectDao.findByBusinessKey(tokenProject.getBusinessKey()) : null;
        Product tokenProduct = productTokenInput.getTokenObject();
        Product product = tokenProduct != null ? productDao.findByPartNumber(tokenProduct.getPartNumber()) : null;

        if(editOrder.isSavedInSAP() && !editOrder.latestSapOrderDetail().getCompanyCode().equals(
                SapIntegrationServiceImpl.getSapCompanyConfigurationForProduct(product).getCompanyCode())) {
            addGlobalValidationError("Unable to update the order in SAP.  This combination of Product and Order is "
                                     + "attempting to change the company code to which this order will be associated.");
        }

        List<Product> addOnProducts = productDao.findByPartNumbers(addOnKeys);
        editOrder.updateData(project, product, addOnProducts, stringToSampleListExisting(sampleList));
        BspUser tokenOwner = owner.getTokenObject();
        editOrder.setCreatedBy(tokenOwner != null ? tokenOwner.getUserId() : null);

        // For sample initiation fields we will set token input fields.
        if (editOrder.isSampleInitiation()) {
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

        SampleLedgerExporter exporter = sampleLedgerExporterFactory.makeExporter(selectedProductOrders);

        try {
            return new BillingTrackerResolution(exporter);
        } catch (Exception e) {
            String message = "Error generating billing tracker for download";
            addGlobalValidationError("{2}: {3}", message, e.getMessage());
            logger.error(message, e);
            setupListDisplay();
            return getSourcePageResolution();
        }
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
            } catch (NoJiraTransitionException | ProductOrderEjb.NoSuchPDOException |
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
            for (Product addOn : product.getAddOns(userBean)) {
                    JSONObject item = new JSONObject();
                    item.put("key", addOn.getBusinessKey());
                    item.put("value", addOn.getProductName());

                    itemList.put(item);
                }
            }
        return createTextResolution(itemList.toString());
    }

    @HandlesEvent("getRegulatoryInfo")
    public Resolution getRegulatoryInfo() throws Exception {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
        String pdoId = getContext().getRequest().getParameter("pdoId");
        if (!StringUtils.isBlank(pdoId)) {
            editOrder = productOrderDao.findById(Long.parseLong(pdoId));
        }
        JSONArray itemList = new JSONArray();
        if (researchProject != null) {
            Map<String, Collection<RegulatoryInfo>>
                    regulatoryInfoByProject = setupRegulatoryInformation(researchProject);
            for (Map.Entry<String, Collection<RegulatoryInfo>> regulatoryEntries : regulatoryInfoByProject.entrySet()) {
                if (!regulatoryEntries.getValue().isEmpty()) {
                    JSONObject item = new JSONObject();
                    item.put("group", regulatoryEntries.getKey());
                    JSONArray values = new JSONArray();
                    for (RegulatoryInfo regulatoryInfo : regulatoryEntries.getValue()) {
                        JSONObject regulatoryInfoJson = new JSONObject();
                        regulatoryInfoJson.put("key", regulatoryInfo.getBusinessKey());
                        regulatoryInfoJson.put("value", regulatoryInfo.getDisplayText());
                        if (editOrder != null && editOrder.getRegulatoryInfos().contains(regulatoryInfo)) {
                            regulatoryInfoJson.put("selected", true);
                        }
                        values.put(regulatoryInfoJson);
                    }
                    item.put("value", values);
                    itemList.put(item);
                }
            }

        }
        return createTextResolution(itemList.toString());
    }

    /**
     * Suggest ORSP numbers based on the currently entered sample IDs. Results can include ORSP numbers that Mercury
     * does not yet have stored in its database.
     *
     * First, fetch the samples' collection IDs from BSP. Then search ORSP for projects referencing consents for those
     * collections. If there is an existing RegulatoryInfo record in Mercury, its primary key is included in the result.
     *
     * The JSON result is an array of objects containing the attributes "identifier", "name", and optionally
     * "regulatoryInfoId".
     *
     * @return a resolution for the JSON results
     */
    @HandlesEvent("suggestRegulatoryInfo")
    public Resolution suggestRegulatoryInfo() throws Exception {
        JSONObject results = new JSONObject();
        JSONArray jsonResults = new JSONArray();

        // Access sample list directly in order to suggest based on possibly not-yet-saved sample IDs.
        if (!getSampleList().isEmpty() && !editOrder.isChildOrder()) {
            List<ProductOrderSample> productOrderSamples = stringToSampleListExisting(getSampleList());
            // Bulk-fetch collection IDs for all samples to avoid having them fetched individually on demand.
            ProductOrder.loadSampleData(productOrderSamples, BSPSampleSearchColumn.BSP_COLLECTION_BARCODE,
                    BSPSampleSearchColumn.COLLECTION);

            Multimap<String, ProductOrderSample> samplesByCollection = HashMultimap.create();
            for (ProductOrderSample productOrderSample : productOrderSamples) {
                String collectionId = productOrderSample.getSampleData().getCollectionId();
                if (!collectionId.isEmpty()) {
                    samplesByCollection.put(collectionId, productOrderSample);
                }
            }

            List<OrspProject> orspProjects = orspProjectDao.findBySamples(productOrderSamples);
            for (OrspProject orspProject : orspProjects) {
                for (OrspProjectConsent consent : orspProject.getConsents()) {
                    Collection<ProductOrderSample> samples =
                            samplesByCollection.get(consent.getKey().getSampleCollection());
                    for (ProductOrderSample sample : samples) {
                        sample.addOrspProject(orspProject);
                    }
                }
            }
            // Fetching RP here instead of a @Before method to avoid the fetch when it won't be used.
            ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
            Map<String, RegulatoryInfo> regulatoryInfoByIdentifier = researchProject.getRegulatoryByIdentifier();
            jsonResults = orspProjectToJson(productOrderSamples, regulatoryInfoByIdentifier);
        }
        results.put("data", jsonResults);
        results.put("draw", 1);
        results.put("recordsTotal", jsonResults.length());

        return createTextResolution(results.toString());
    }

    /**
     * Create a JSON data structure linking all the input ProductOrderSamples to possible OSRP Projects and Sample
     * Collections. If regulatoryInfo is non-null, it is assumed to be an existing record with the same identifier
     * associated with the Research Project in which case its primary key is also placed in the result. This allows
     * client-side scripts to match these results with entries in a regulatory info selection widget.
     *
     * @param productOrderSamples input samples to find possible regulatory information about.
     * @param regulatoryInfoById the matching RegulatoryInfo record (or null)
     * @return A JSONArray of JSONObjects which link the ProductOrderSamples to ORSP projects and Sample Collections.
     */
    private JSONArray orspProjectToJson(List<ProductOrderSample> productOrderSamples, Map<String, RegulatoryInfo> regulatoryInfoById) {
        JSONArray resultList = new JSONArray();
        try {
            ListMultimap<OrspProject, ProductOrderSample> orspProjectSamples = ArrayListMultimap.create();
            Set<String> samplesWithNoOrsp = new HashSet<>();
            for (ProductOrderSample productOrderSample : productOrderSamples) {
                if (productOrderSample.getOrspProjects().isEmpty()) {
                    samplesWithNoOrsp.add(productOrderSample.getSampleKey());
                }
                for (OrspProject orspProject : productOrderSample.getOrspProjects()) {
                    orspProjectSamples.put(orspProject, productOrderSample);
                }
            }

            for (Map.Entry<OrspProject, Collection<ProductOrderSample>> pdoSamplesEntry : orspProjectSamples.asMap()
                    .entrySet()) {
                OrspProject orspProject = pdoSamplesEntry.getKey();
                JSONObject orspObject = new JSONObject();
                orspObject.put("identifier", orspProject.getProjectKey());
                orspObject.put("name", orspProject.getName());

                RegulatoryInfo regulatoryInfo = regulatoryInfoById.get(orspProject.getProjectKey());
                if (regulatoryInfo != null) {
                    orspObject.put("regulatoryInfoId", regulatoryInfo.getRegulatoryInfoId());
                }

                Set<String> samples = new HashSet<>();
                Set<String> collections = new HashSet<>();
                for (ProductOrderSample productOrderSample : pdoSamplesEntry.getValue()) {
                    samples.add(productOrderSample.getSampleKey());
                    collections.add(productOrderSample.getSampleData().getCollection());
                }

                resultList.put(buildOrspJsonObject(orspObject, samples, collections));
            }
            if (!samplesWithNoOrsp.isEmpty()) {
                resultList.put(buildOrspJsonObject(new JSONObject(), samplesWithNoOrsp, Collections.<String>emptySet()));
            }
        } catch (JSONException e) {
            throw new RuntimeException("No, I didn't pass a null key to JSONObject.put()");
        }
        return resultList;
    }

    @HandlesEvent("getPostReceiveOptions")
    public Resolution getPostReceiveOptions() throws Exception {
        JSONArray itemList = new JSONArray();
        boolean savedOrder = !StringUtils.isBlank(this.productOrder);
        MaterialInfo materialInfo = MaterialInfo.fromText(this.materialInfo);
        for (PostReceiveOption postReceiveOption : materialInfo.getPostReceiveOptions()) {
            if (!postReceiveOption.getArchived()) {
                JSONObject item = new JSONObject();
                item.put("key", postReceiveOption.name());
                item.put("label", postReceiveOption.getText());
                if (!savedOrder) {
                    item.put("checked", postReceiveOption.getDefaultToChecked());
                } else {
                    item.put("checked", false);
                }
                itemList.put(item);
            }
        }

        JSONObject kitIndexObject = new JSONObject();

        kitIndexObject.put(KIT_DEFINITION_INDEX, this.kitDefinitionQueryIndex);
        kitIndexObject.put("dataList", itemList);
        if (StringUtils.isNotBlank(prepopulatePostReceiveOptions)) {
            kitIndexObject.put("prepopulatePostReceiveOptions", prepopulatePostReceiveOptions);
        }

        return createTextResolution(kitIndexObject.toString());
    }

    public String getSummary() throws Exception {
        if (editOrder != null && StringUtils.isBlank(sampleSummary)) {
            JSONArray sampleSummaryJson = new JSONArray();
            try {
                List<String> comments = editOrder.getSampleSummaryComments();
                for (String comment : comments) {
                    JSONObject item = new JSONObject();
                    item.put("comment", comment);
                    sampleSummaryJson.put(item);
                }
                sampleSummary = sampleSummaryJson.toString();
            } catch (BSPLookupException e) {
                handleBspLookupFailed(e);
            }
        }
        return sampleSummary;
    }

    /**
     * This convenience method logs exceptions from bsp and adds a global validation error.
     *
     * @param bspLookupException the exception thrown when a call to BspSampleDataFetcher fails.
     */
    private void handleBspLookupFailed(BSPLookupException bspLookupException) {
        String errorMessage = String.format("%s: %s", COULD_NOT_LOAD_SAMPLE_DATA, bspLookupException.getMessage());
        addGlobalValidationError(errorMessage);
        logger.error(errorMessage);
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
        List<String> selectedOrderAddons = null;
        if(selectedAddOns != null) {
            selectedOrderAddons = Arrays.asList(StringUtils.split(selectedAddOns, "|@|"));
        }
        JSONObject item = new JSONObject();

        if (product != null) {
            final Product productToFind = productDao.findByBusinessKey(product);
            supportsNumberOfLanes = productToFind.getSupportsNumberOfLanes();
            if(selectedOrderAddons != null)
            for (String selectedOrderAddon : selectedOrderAddons) {
                if(!supportsNumberOfLanes) {
                    supportsNumberOfLanes = productDao.findByBusinessKey(selectedOrderAddon).getSupportsNumberOfLanes();
                } else {
                    break;
                }
            }
        }

        item.put(BspSampleData.SUPPORTS_NUMBER_OF_LANES, supportsNumberOfLanes);

        return createTextResolution(item.toString());
    }

    @ValidationMethod(on = "deleteOrder")
    public void validateDeleteOrder() {
        if (!editOrder.isDraft()) {
            addGlobalValidationError("Orders can only be deleted in draft mode");
        }
    }

    @ValidationMethod(
            on = {DELETE_SAMPLES_ACTION, ABANDON_SAMPLES_ACTION, SET_RISK, RECALCULATE_RISK, ADD_SAMPLES_TO_BUCKET,
                    UNABANDON_SAMPLES_ACTION, SET_PROCEED_OOS},
            priority = 0)
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

    @ValidationMethod(on = {DELETE_SAMPLES_ACTION, ABANDON_SAMPLES_ACTION, UNABANDON_SAMPLES_ACTION}, priority = 1)
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
        try {
            productOrderEjb.removeSamples(editOrder.getBusinessKey(), selectedProductOrderSamples, this);
        } catch (SAPInterfaceException e) {
            logger.error("SAP error when attempting to delete samples", e);
            addGlobalValidationError(e.getMessage());
        }
        return createViewResolution(editOrder.getBusinessKey());
    }


    @HandlesEvent(ADD_SAMPLES_TO_BUCKET)
    public Resolution addSamplesToBucket() {
        try {
            productOrderEjb.handleSamplesAdded(editOrder.getBusinessKey(), editOrder.getSamples(), this);
        } catch (BucketException e) {
            addGlobalValidationError(e.getMessage());
        }
        return new ForwardResolution(ORDER_VIEW_PAGE);
    }

    @HandlesEvent(SET_RISK)
    public Resolution setRisk() throws Exception {

        productOrderEjb.addManualOnRisk(
                getUserBean().getBspUser(), editOrder.getBusinessKey(), selectedProductOrderSamples, riskStatus,
                riskComment);

        addMessage("Set manual on risk to {0} for {1} samples.", riskStatus, selectedProductOrderSampleIds.size());

        return createViewResolution(editOrder.getBusinessKey());
    }

    @HandlesEvent(SET_PROCEED_OOS)
    public Resolution proceedOos() {
        productOrderEjb.proceedOos(userBean.getBspUser(), selectedProductOrderSamples, editOrder, proceedOos);
        return createViewResolution(editOrder.getBusinessKey());
    }

    /**
     * Builds a List of ProductOrderSamples by finding
     * PDOSamples in the given pdo that have an id
     * that matches an id in the pdoIds list.
     */
    private List<ProductOrderSample> getSelectedProductOrderSamplesFor(ProductOrder pdo,
                                                                       Collection<Long> pdoIds) {
        List<ProductOrderSample> selectedPdoSamples = new ArrayList<>(getSelectedProductOrderSampleIds().size());
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
            validateRinScores(getSelectedProductOrderSamplesFor(editOrder, getSelectedProductOrderSampleIds()));
        }
    }

    @HandlesEvent(RECALCULATE_RISK)
    public Resolution recalculateRisk() throws Exception {
        int originalOnRiskCount = editOrder.countItemsOnRisk();

        try {

            //  FIXME SGM Must try this with setting the method to TransactionAttribute(TransactionAttributeType.REQUIRED)
            // Currently it seems to cause an unintended Transaction to be started which will save the product order.
            //  THere needs to be some persist action specifically called after the changes to risk in order to save
            // The order.  it should not depend on what should be essentially a DBFree non transactional method.

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

            MessageCollection abandonSamplesMessageCollection = new MessageCollection();

            try {
                productOrderEjb.publishProductOrderToSAP(editOrder, abandonSamplesMessageCollection, false);
            } catch (SAPInterfaceException e) {
                logger.error("SAP Error when attempting to abandon samples", e);
                addGlobalValidationError(e.getMessage());
            }
            addMessages(abandonSamplesMessageCollection);
        }
        return createViewResolution(editOrder.getBusinessKey());
    }

    @HandlesEvent(UNABANDON_SAMPLES_ACTION)
    public Resolution unAbandonSamples() throws Exception {

        if (CollectionUtils.isNotEmpty(selectedProductOrderSampleIds)) {
            try {
                productOrderEjb.unAbandonSamples(editOrder.getJiraTicketKey(), selectedProductOrderSampleIds,
                        unAbandonComment, this);
            } catch (ProductOrderEjb.SampleDeliveryStatusChangeException e) {
                addGlobalValidationError(e.getMessage());
                return new ForwardResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(
                        PRODUCT_ORDER_PARAMETER,
                        editOrder.getBusinessKey());
            }
            productOrderEjb.updateOrderStatus(editOrder.getJiraTicketKey(), this);

            MessageCollection abandonSamplesMessageCollection = new MessageCollection();
            try {
                productOrderEjb.publishProductOrderToSAP(editOrder, abandonSamplesMessageCollection, false);
            } catch (SAPInterfaceException e) {
                logger.error("SAP Error when attempting to abandon samples", e);
                addGlobalValidationError(e.getMessage());
            }
            addMessages(abandonSamplesMessageCollection);
        }
        return createViewResolution(editOrder.getBusinessKey());
    }
    @ValidationMethod(on = ADD_SAMPLES_ACTION)
    public void addSampleExtraValidations() throws Exception {
        try {
            if (productOrderEjb.isOrderEligibleForSAP(editOrder)) {
                validateQuoteDetailsWithAddedSamples(editOrder.getQuoteId(), ErrorLevel.ERROR,
                        !editOrder.hasJiraTicketKey(), stringToSampleList(addSamplesText).size());
            }
        } catch (QuoteServerException e) {
            addGlobalValidationError("The quote ''{2}'' is not valid: {3}", editOrder.getQuoteId(), e.getMessage());
        } catch (QuoteNotFoundException e) {
            addGlobalValidationError("The quote ''{2}'' was not found ", editOrder.getQuoteId());
        } catch (InvalidProductException e) {
            addGlobalValidationError("Unable to determine the existing value of open orders for " + editOrder.getQuoteId() +": " +e.getMessage());
        }
    }

    private void testForPriceItemValidity(ProductOrder editOrder) {
        if(productOrderEjb.arePriceItemsValid(editOrder, new HashSet<AccessItem>())) {
            final String errorMessage = "One of the price items on this orders products is invalid";
            if(editOrder.isSavedInSAP()) {
                addGlobalValidationError(errorMessage);
            } else {
                addMessage(errorMessage);
            }
        }
    }

    @HandlesEvent(ADD_SAMPLES_ACTION)
    public Resolution addSamples() throws Exception {
        List<ProductOrderSample> samplesToAdd = stringToSampleList(addSamplesText);
        try {
            productOrderEjb.addSamples(editOrder.getJiraTicketKey(), samplesToAdd, this);
        } catch (BucketException e) {
            logger.error("Problem adding samples to bucket", e);
            addGlobalValidationError(e.getMessage());
        } catch (SAPInterfaceException sie) {
            logger.error("Error from SAP when attempting to add samples");
            addGlobalValidationError(sie.getMessage());
        }

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

    public String getSquidWorkRequestUrl(String workRequestId) {
        return squidLink.workRequestUrl(workRequestId);
    }

    public String getSquidWorkRequestUrl() {
        return getSquidWorkRequestUrl(editOrder.getSquidWorkRequest());
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

    public String getSelectedAddOns() {
        return selectedAddOns;
    }

    public void setSelectedAddOns(String selectedAddOns) {
        this.selectedAddOns = selectedAddOns;
    }

    public String getMaterialInfo() {
        return materialInfo;
    }

    public void setMaterialInfo(String materialInfo) {
        this.materialInfo = materialInfo;
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

            collectionAndOrganismsList.put(KIT_DEFINITION_INDEX, kitDefinitionQueryIndex);
            if (StringUtils.isNotBlank(prePopulatedOrganismId)) {
                collectionAndOrganismsList.put(CHOSEN_ORGANISM, prePopulatedOrganismId);
            } else if (CollectionUtils.isNotEmpty(kitDetails) && kitDetails.size() > Integer
                    .valueOf(kitDefinitionQueryIndex)) {
                collectionAndOrganismsList.put(CHOSEN_ORGANISM,
                        kitDetails.get(Integer.valueOf(kitDefinitionQueryIndex))
                                .getOrganismId());
            }

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

    public Map<Integer, List<String>> getPostReceiveOptionKeys() {
        return postReceiveOptionKeys;
    }

    public void setPostReceiveOptionKeys(Map<Integer, List<String>> postReceiveOptionKeys) {
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
        return userBean.isValidUser() && editOrder.getOrderStatus().canPlace();
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
        final List<String> sampleNames = SearchActionBean.cleanInputStringForSamples(sampleListText);
        for (String sampleName : sampleNames) {
            samples.add(new ProductOrderSample(sampleName));
        }

        return samples;
    }

    private List<ProductOrderSample> stringToSampleListExisting(String sampleListText) {
        List<ProductOrderSample> samples = new ArrayList<>();
        List<String> sampleNames = SearchActionBean.cleanInputStringForSamples(sampleListText);

        // Allow random access to existing ProductOrderSamples.  A sample can appear more than once.
        Map<String, List<ProductOrderSample>> mapIdToSampleList = new HashMap<>();
        for (ProductOrderSample productOrderSample : editOrder.getSamples()) {
            List<ProductOrderSample> productOrderSamples = mapIdToSampleList.get(productOrderSample.getSampleKey());
            if (productOrderSamples == null) {
                productOrderSamples = new ArrayList<>();
                mapIdToSampleList.put(productOrderSample.getSampleKey(), productOrderSamples);
            }
            productOrderSamples.add(productOrderSample);
        }

        // Use existing, if any, or create new.
        for (String sampleName : sampleNames) {
            ProductOrderSample productOrderSample;
            List<ProductOrderSample> productOrderSamples = mapIdToSampleList.get(sampleName);

            if (productOrderSamples == null || productOrderSamples.isEmpty()) {
                productOrderSample = new ProductOrderSample(sampleName);
            } else {
                productOrderSample = productOrderSamples.remove(0);
            }
            samples.add(productOrderSample);
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

    public ProductOrderSample.ProceedIfOutOfSpec getProceedOos() {
        return proceedOos;
    }

    public void setProceedOos(ProductOrderSample.ProceedIfOutOfSpec proceedOos) {
        this.proceedOos = proceedOos;
    }

    public String getAbandonComment() {
        return abandonComment;
    }

    public void setAbandonComment(String abandonComment) {
        this.abandonComment = abandonComment;
    }

    public String getUnAbandonComment() {
        return unAbandonComment;
    }

    public void setUnAbandonComment(String unAbandonComment) {
        this.unAbandonComment = unAbandonComment;
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
    public boolean getCanAbandonOrder() {
        ProductOrder.OrderStatus status = editOrder.getOrderStatus();
        if (!status.canAbandon()) {
            setAbandonDisabledReason("an order with status " + status + " can't be abandoned.");
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
    public double getPercentAbandoned() {
        return progressFetcher.getPercentAbandoned(editOrder.getBusinessKey());
    }

    /**
     * Convenience method for PDO view page to show percentage of samples completed for the current PDO.
     *
     * @return Percentage of sample completed.
     */
    public double getPercentCompleted() {
        return progressFetcher.getPercentCompleted(editOrder.getBusinessKey());
    }

    /**
     * Convenience method for PDO view page to show percentage of samples in progress for the current PDO.
     *
     * @return Percentage of sample in progress.
     */
    public double getPercentInProgress() {
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
        } else {
            ProductOrderCompletionStatus status = progressFetcher.getStatus(editOrder.getBusinessKey());
            List<String> progressPieces = new ArrayList<>();
            if (status.getPercentAbandoned() > 0) {
                progressPieces.add(formatProgress(status.getNumberAbandoned(), status.getPercentAbandonedDisplay(),
                        "Abandoned"));
            }
            if (status.getPercentCompleted() > 0) {
                progressPieces.add(formatProgress(status.getNumberCompleted(), status.getPercentCompletedDisplay(),
                        "Completed"));
            }
            if (status.getPercentInProgress() > 0) {
                progressPieces.add(formatProgress(status.getNumberInProgress(), status.getPercentInProgressDisplay(),
                        "In Progress"));
            }

            return StringUtils.join(progressPieces, ", ");
        }
    }

    /**
     * Format progress String
     *
     * @return Formatted progress string for display.
     */
    private String formatProgress(int number, String percentageDisplay, String identifier) {
        return String.format("%d %s (%s%%)", number, identifier, percentageDisplay);
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

    public Set<ProductOrder.OrderStatus> getSelectedStatuses() {
        return selectedStatuses;
    }

    public void setSelectedStatuses(Set<ProductOrder.OrderStatus> selectedStatuses) {
        this.selectedStatuses = selectedStatuses;
    }

    public List<ProductOrderListEntry.LedgerStatus> getSelectedLedgerStatuses() {
        return selectedLedgerStatuses;
    }

    public void setSelectedLedgerStatuses(List<ProductOrderListEntry.LedgerStatus> selectedLedgerStatuses) {
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
     * the user is appropriate for editing.
     */
    @Override
    public boolean isEditAllowed() {
        return !editOrder.isDraft() && isCreateAllowed();
    }

    public boolean isEditResearchProjectAllowed() {
        return ResearchProjectActionBean.isEditAllowed(getUserBean());
    }

    public ProductOrderListEntry.LedgerStatus[] getLedgerStatuses() {
        return ProductOrderListEntry.LedgerStatus.values();
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

    public void validateQuoteOptions(String action) {
        if (action.equals(PLACE_ORDER_ACTION) || action.equals(VALIDATE_ORDER) ||
            (action.equals(SAVE_ACTION) && editOrder.isSubmitted())) {
            boolean hasQuote = !StringUtils.isBlank(editOrder.getQuoteId());
            requireField(hasQuote || editOrder.canSkipQuote(), "a quote specified", action);
            if (!hasQuote && editOrder.allowedToSkipQuote()) {
                requireField(editOrder.getSkipQuoteReason() , "an explanation for why a quote cannot be entered", action);
            }
        }
    }

    public void validateRegulatoryInformation(String action) {

        if (action.equals(PLACE_ORDER_ACTION) || action.equals(VALIDATE_ORDER)) {
            requireField(editOrder.regulatoryRequirementsMet(),
                    "its regulatory requirements met or a reason for bypassing the regulatory requirements",
                    action);
            if (!editOrder.orderPredatesRegulatoryRequirement()) {
                requireField(editOrder.isAttestationConfirmed().booleanValue(),
                        "the checkbox checked which attests that you are aware of the regulatory requirements for this project",
                        action);
            }
        }

        if (action.equals(SAVE_ACTION)) {
            if (skipRegulatoryInfo) {
                requireField(editOrder.canSkipRegulatoryRequirements(),
                        "a reason for bypassing the regulatory requirements", action);
            }
            if (editOrder.isRegulatoryInfoEditAllowed()) {
                if (CollectionUtils.isNotEmpty(selectedRegulatoryIds)) {
                    List<RegulatoryInfo> selectedRegulatoryInfos = regulatoryInfoDao
                            .findListByList(RegulatoryInfo.class, RegulatoryInfo_.regulatoryInfoId,
                                    selectedRegulatoryIds);

                    Set<String> missingRegulatoryRequirements = new HashSet<>();
                    for (RegulatoryInfo chosenInfo : selectedRegulatoryInfos) {
                        if (!chosenInfo.getResearchProjectsIncludingChildren()
                                .contains(editOrder.getResearchProject())) {
                            missingRegulatoryRequirements.add(chosenInfo.getName());
                        }
                    }

                    if (!missingRegulatoryRequirements.isEmpty()) {
                        addGlobalValidationError("Regulatory info {2} is not associated with research project {3}",
                                StringUtils.join(missingRegulatoryRequirements, ", "),
                                editOrder.getResearchProject().getName());
                    }

                }
            }

        }
    }

    public KitType getChosenKitType() {
        return chosenKitType;
    }

    public void setChosenKitType(KitType chosenKitType) {
        this.chosenKitType = chosenKitType;
    }

    /**
     * Attempt to return the list of receptacles for a given dna kit type.
     * <p/>
     * FIXME (GPLIM-2463):  Currently, there is only one kit type and one receptacle type represented.  We will need to expand
     * this.  There for the singletonlist is just a placeholder
     *
     * @return List of receptacle types
     */
    public List<String> getKitReceptacleTypes() {
        return Collections.singletonList(KitType.valueOf(chosenKitType.name()).getDisplayName());
    }

    public String getKitDefinitionQueryIndex() {
        return kitDefinitionQueryIndex;
    }

    public void setKitDefinitionQueryIndex(String kitDefinitionQueryIndex) {
        this.kitDefinitionQueryIndex = kitDefinitionQueryIndex;
    }

    public String getKitDefinitionIndexIdentifier() {
        return KIT_DEFINITION_INDEX;
    }

    public List<ProductOrderKitDetail> getKitDetails() {
        return kitDetails;
    }

    public void setKitDetails(List<ProductOrderKitDetail> kitDetails) {
        this.kitDetails = kitDetails;
    }

    public String getPrePopulatedOrganismId() {
        return prePopulatedOrganismId;
    }

    public void setPrePopulatedOrganismId(String prePopulatedOrganismId) {
        this.prePopulatedOrganismId = prePopulatedOrganismId;
    }

    public String getPrepopulatePostReceiveOptions() {
        return prepopulatePostReceiveOptions;
    }

    public void setPrepopulatePostReceiveOptions(String prepopulatePostReceiveOptions) {
        this.prepopulatePostReceiveOptions = prepopulatePostReceiveOptions;
    }

    public void setDeletedKits(String[] deletedKits) {
        this.deletedKits = deletedKits;
    }

    public String[] getDeletedKits() {
        return deletedKits;
    }

    public boolean isSkipRegulatoryInfo() {
        return skipRegulatoryInfo;
    }

    public void setSkipRegulatoryInfo(boolean skipRegulatoryInfo) {
        this.skipRegulatoryInfo = skipRegulatoryInfo;
    }

    @Inject
    public void setRegulatoryInfoDao(RegulatoryInfoDao regulatoryInfoDao) {
        this.regulatoryInfoDao = regulatoryInfoDao;
    }

    public boolean isCollaborationKitRequest() {
        return !StringUtils.isBlank(editOrder.getProductOrderKit().getWorkRequestId()) && !editOrder
                .isSampleInitiation();
    }

    /**
     * Get count of samples not received. Return null if the samples can not be found in BSP
     */
    public Integer getNumberSamplesNotReceived() {
        Integer samplesNotReceived=null;
        try {
            samplesNotReceived = editOrder.getSampleCount() - editOrder.getReceivedSampleCount();
        } catch (BSPLookupException e) {
            handleBspLookupFailed(e);
        }
        return samplesNotReceived;
    }

    public EnumSet<ProductOrder.OrderStatus> getOrderStatusNamesWhichCantBeAbandoned() {
        return EnumSet.complementOf (ProductOrder.OrderStatus.canAbandonStatuses);
    }

    public GenotypingChip getGenotypingChip() {
        if (genotypingChip == null) {
            Date effectiveDate =  editOrder.getCreatedDate();
            Pair<String, String> chipFamilyAndName = productEjb.getGenotypingChip(editOrder,
                    effectiveDate);
            if (chipFamilyAndName.getLeft() != null && chipFamilyAndName.getRight() != null) {
                genotypingChip= attributeArchetypeDao.findGenotypingChip(chipFamilyAndName.getLeft(),
                        chipFamilyAndName.getRight());
            }
        }
        return genotypingChip;
    }

    public GenotypingProductOrderMapping getGenotypingProductOrderMapping() {
        if (genotypingProductOrderMapping == null) {
            genotypingProductOrderMapping =
                    attributeArchetypeDao.findGenotypingProductOrderMapping(editOrder.getJiraTicketKey());
        }
        return genotypingProductOrderMapping;
    }

    private GenotypingProductOrderMapping findOrCreateGenotypingProductOrderMapping() {
        if (genotypingProductOrderMapping == null) {
            genotypingProductOrderMapping =
                    productOrderEjb.findOrCreateGenotypingChipProductOrderMapping(editOrder.getJiraTicketKey());
        }
        return genotypingProductOrderMapping;
    }

    /**
     * @return Overridden value of call rate threshold found in product orders genotyping chip mapping if one exists,
     * else return the default value found on a genotyping chip.
     */
    public String getCallRateThreshold() {
        if (getGenotypingProductOrderMapping() != null &&
            getGenotypingProductOrderMapping().getCallRateThreshold() != null) {
            return getGenotypingProductOrderMapping().getCallRateThreshold();
        } else if (getGenotypingChip() != null){
            Map<String, String> chipAttributes = genotypingChip.getAttributeMap();
            return chipAttributes.get("call_rate_threshold");
        }
        return null;
    }

    /**
     * If callRateThreshold overrides default found in GenotypingChip then set in GenotypingChipProductOrderMapping
     */
    public void setCallRateThreshold(String callRateThreshold) {
        if (getGenotypingChip() != null) {
            ArchetypeAttribute zcallThresholdUnix = getGenotypingChip().getAttribute("call_rate_threshold");
            if (zcallThresholdUnix != null && zcallThresholdUnix.getAttributeValue() != null &&
                    !zcallThresholdUnix.getAttributeValue().equals(callRateThreshold)) {
                GenotypingProductOrderMapping mapping = findOrCreateGenotypingProductOrderMapping();
                if (mapping != null) {
                    mapping.setCallRateThreshold(callRateThreshold);
                }
            }
        }
    }

    /**
     * @return Overridden value of call rate threshold found in product orders genotyping chip mapping if one exists,
     * else return the default value found on a genotyping chip.
     */
    public String getClusterFile() {
        if (getGenotypingProductOrderMapping() != null &&
            getGenotypingProductOrderMapping().getClusterLocation() != null) {
            return getGenotypingProductOrderMapping().getClusterLocation();
        } else if (getGenotypingChip() != null){
            Map<String, String> chipAttributes = genotypingChip.getAttributeMap();
            return chipAttributes.get("cluster_location_unix");
        }
        return null;
    }

    /**
     * If callRateThreshold overrides default found in GenotypingChip then set in GenotypingChipProductOrderMapping
     */
    public void setClusterFile(String clusterFile) {
        if (getGenotypingChip() != null) {
            ArchetypeAttribute zcallThresholdUnix = getGenotypingChip().getAttribute("cluster_location_unix");
            if (zcallThresholdUnix != null && zcallThresholdUnix.getAttributeValue() != null &&
                !zcallThresholdUnix.getAttributeValue().equals(clusterFile)) {
                GenotypingProductOrderMapping mapping = findOrCreateGenotypingProductOrderMapping();
                if (mapping != null) {
                    mapping.setClusterLocation(clusterFile);
                }
            }
        }
    }

    /**
     * Override fields are group attributes set in AttributeDefinition
     */
    private void populateAttributes(String jiraTicketKey) {
        // Set default values first from GenotypingChip
        attributes.clear();
        Map<String, AttributeDefinition> definitionsMap = getPdoAttributeDefinitions();
        GenotypingChip chip = getGenotypingChip();
        chipDefaults.clear();
        if (chip != null && definitionsMap != null) {
            for (Map.Entry<String, AttributeDefinition> entry: definitionsMap.entrySet()) {
                AttributeDefinition pdoAttributeDefinition = entry.getValue();
                //Group attributes of PDO Attribute Definition tell what overrides GenotypingChip Attributes
                for (ArchetypeAttribute chipAttribute : chip.getAttributes()) {
                    if (chipAttribute.getAttributeName().equals(pdoAttributeDefinition.getAttributeName())) {
                        attributes.put(chipAttribute.getAttributeName(), chipAttribute.getAttributeValue());
                        chipDefaults.put(chipAttribute.getAttributeName(), chipAttribute.getAttributeValue());
                    }
                }
            }
        }

        // Check if a mapping exists for this pdo, if so override default values if not null
        genotypingProductOrderMapping =
                attributeArchetypeDao.findGenotypingProductOrderMapping(jiraTicketKey);
        if (genotypingProductOrderMapping != null) {
            for (ArchetypeAttribute attribute : genotypingProductOrderMapping.getAttributes()) {
                AttributeDefinition definition = getPdoAttributeDefinitions().get(attribute.getAttributeName());
                if (definition != null && definition.isDisplayable()) {
                    if (attribute.getAttributeValue() != null) {
                        attributes.put(attribute.getAttributeName(), attribute.getAttributeValue());
                    }
                }
            }
        }
    }

    public boolean isInfinium() {
        Date effectiveDate = editOrder.getCreatedDate();
        if (effectiveDate != null) {
            Pair<String, String> chipPair = productEjb.getGenotypingChip(editOrder, effectiveDate);
            return chipPair != null && chipPair.getLeft() != null && chipPair.getRight() != null;
        }
        return false;
    }

    private Map<String, AttributeDefinition> getPdoAttributeDefinitions() {
        if (pdoSpecificDefinitions == null) {
            pdoSpecificDefinitions = attributeArchetypeDao.findAttributeGroupByTypeAndName(
                    AttributeDefinition.DefinitionType.GENOTYPING_PRODUCT_ORDER,
                    GenotypingProductOrderMapping.ATTRIBUTES_GROUP);
        }
        return pdoSpecificDefinitions;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public HashMap<String, String> getChipDefaults() {
        return chipDefaults;
    }
    public String getPublishSAPAction() {return PUBLISH_PDO_TO_SAP;}

    public String getReplacementSampleList() {
        return replacementSampleList;
    }

    public void setReplacementSampleList(String replacementSampleList) {
        this.replacementSampleList = replacementSampleList;
    }

    public DatatablesStateSaver getPreferenceSaver() {
        return preferenceSaver;
    }

    public void setPreferenceSaver(DatatablesStateSaver preferenceSaver) {
        this.preferenceSaver = preferenceSaver;
    }

    public String getTableState() {
        return tableState;
    }

    public void setTableState(String tableState) {
        this.tableState = tableState;
    }

    public static JSONObject buildOrspJsonObject(JSONObject orspProject, Set<String> samples,
                                                 Set<String> sampleCollections) throws JSONException
    {
        JSONObject result = new JSONObject();
        result.put("orspProject", orspProject);
        result.put("samples", samples);
        result.put("collections", sampleCollections);
        return result;
    }

    @Inject
    public void setPriceListCache(PriceListCache priceListCache) {
        this.priceListCache = priceListCache;
    }


    private static final ObjectMapper objectMapper = new ObjectMapper();

    @HandlesEvent(SAVE_SEARCH_DATA)
    public Resolution saveSearchData() throws Exception {
        preferenceSaver.saveTableData(tableState);
        return new StreamingResolution("application/json", preferenceSaver.getTableStateJson());
    }

    public boolean showColumn(String columnName) {
        return preferenceSaver.showColumn(columnName);
    }

}
