package org.broadinstitute.gpinformatics.athena.presentation.orders;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.broadinstitute.gpinformatics.athena.entity.orders.PriceAdjustment;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderCompletionStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKit;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderKitDetail;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.preference.NameValueDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.athena.entity.products.GenotypingProductOrderMapping;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.ProductFamily;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.athena.presentation.billing.BillingSessionActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.links.QuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SapQuoteLink;
import org.broadinstitute.gpinformatics.athena.presentation.links.SquidLink;
import org.broadinstitute.gpinformatics.athena.presentation.projects.ResearchProjectActionBean;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspGroupCollectionTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.BspShippingLocationTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProductTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.ProjectTokenInput;
import org.broadinstitute.gpinformatics.athena.presentation.tokenimporters.UserTokenInput;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataSourceResolver;
import org.broadinstitute.gpinformatics.infrastructure.analytics.OrspProjectDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProject;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.OrspProjectConsent;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPKitRequestService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.KitType;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryEnumUtils;
import org.broadinstitute.gpinformatics.infrastructure.common.MercuryStringUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition.NoJiraTransitionException;
import org.broadinstitute.gpinformatics.infrastructure.presentation.SampleLink;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPInterfaceException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateRangeSelector;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.BucketException;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.BSPLookupException;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.CoverageTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.ArchetypeAttribute;
import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeDefinition;
import org.broadinstitute.gpinformatics.mercury.entity.run.GenotypingChip;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.DatatablesStateSaver;
import org.broadinstitute.gpinformatics.mercury.presentation.datatables.State;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;
import org.broadinstitute.sap.entity.OrderCalculatedValues;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.entity.quote.FundingStatus;
import org.broadinstitute.sap.entity.quote.QuoteHeader;
import org.broadinstitute.sap.entity.quote.QuoteStatus;
import org.broadinstitute.sap.entity.quote.SapQuote;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.hibernate.Hibernate;
import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jvnet.inflector.Noun;
import org.owasp.encoder.Encode;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.broadinstitute.gpinformatics.mercury.presentation.datatables.DatatablesStateSaver.SAVE_SEARCH_DATA;

/**
 * This handles all the needed interface processing elements.
 */
@SuppressWarnings("unused")
@UrlBinding(ProductOrderActionBean.ACTIONBEAN_URL_BINDING)
public class ProductOrderActionBean extends CoreActionBean {

    public static final String SWITCHING_QUOTES_NOT_PERMITTED =
        "Switching between Quote Server and SAP quotes is not permitted once an order has been placed.";
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
    public static final String CUSTOMIZE_PRODUCT_ASSOCIATIONS = "/orders/customize_product_associations.jsp";

    private static final String ADD_SAMPLES_ACTION = "addSamples";
    private static final String ABANDON_SAMPLES_ACTION = "abandonSamples";
    private static final String UNABANDON_SAMPLES_ACTION = "unAbandonSamples";
    private static final String DELETE_SAMPLES_ACTION = "deleteSamples";
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
    public static final String GET_SAMPLE_DATA = "getSampleData";
    public static final String GET_SAMPLE_SUMMARY = "getSampleSummary";
    public static final String OPEN_CUSTOM_VIEW_ACTION = "openCustomView";

    public static final List<String> EXCLUDED_QUOTES_FROM_VALUE = Stream.of("GP87U", "CRSPEVR", "GPSPGR7").collect(Collectors.toList());

    private String sampleSummary;
    private State state;
    private ProductOrder.QuoteSourceType quoteSource;

    public ProductOrderActionBean() {
        super(CREATE_ORDER, EDIT_ORDER, PRODUCT_ORDER_PARAMETER);
    }

    @Inject
    private AppConfig appConfig;

    @Inject
    private ProductFamilyDao productFamilyDao;

    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private ProductOrderSampleDao productOrderSampleDao;

    private ResearchProjectDao researchProjectDao;

    @Inject
    private PreferenceEjb preferenceEjb;

    @Inject
    private PreferenceDao preferenceDao;

    @Inject
    private ProductOrderListEntryDao orderListEntryDao;

    private BSPUserList bspUserList;

    @Inject
    private QuoteLink quoteLink;

    @Inject
    private SapQuoteLink sapQuoteLink;

    @Inject
    private SquidLink squidLink;

    private ProductOrderEjb productOrderEjb;

    private ProductTokenInput productTokenInput;

    private ProjectTokenInput projectTokenInput;

    private BspShippingLocationTokenInput bspShippingLocationTokenInput;

    private BspGroupCollectionTokenInput bspGroupCollectionTokenInput;

    @Inject
    private BSPManagerFactory bspManagerFactory;

    private UserTokenInput notificationListTokenInput;

    @Inject
    private BSPConfig bspConfig;

    @SuppressWarnings("CdiInjectionPointsInspection")
    private JiraService jiraService;

    @Inject
    private BSPKitRequestService bspKitRequestService;

    @Inject
    private SampleDataSourceResolver sampleDataSourceResolver;

    private PriceListCache priceListCache;

    @Inject
    private QuoteDetailsHelper quoteDetailsHelper;

    private SAPProductPriceCache productPriceCache;

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private OrspProjectDao orspProjectDao;

    @Inject
    private ReagentDesignDao reagentDesignDao;

    @Inject
    private CoverageTypeDao coverageTypeDao;

    private List<ProductOrderListEntry> displayedProductOrderListEntries;

    private String sampleList;
    private String replacementSampleList;

    @Validate(required = true, on = {EDIT_ACTION})
    private String productOrder;

    private List<Long> sampleIdsForGetBspData;

    private boolean initialLoad = false;
    private boolean includeSampleSummary = false;

    private CompletionStatusFetcher progressFetcher;

    private boolean skipRegulatoryInfo;

    private boolean notFromHumans;
    private boolean fromClinicalLine;
    private boolean sampleManipulationOnly;

    private GenotypingChip genotypingChip;

    private GenotypingProductOrderMapping genotypingProductOrderMapping;

    private Map<String, AttributeDefinition> pdoSpecificDefinitions = null;

    private Map<String, String> attributes = new HashMap<>();
    private Map<String, String> chipDefaults = new HashMap<>();


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
    private String originalQuote;

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

    private String customizationJsonString;

    private String customizableProducts;
    private String customizedProductPrices;
    private String customizedProductQuantities;
    private String customizedProductNames;

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

    public String getSlowColumns() {
        return new JSONArray(ProductOrderSampleBean.SLOW_COLUMNS).toString();
    }

    private List<ProductOrderKitDetail> kitDetails = new ArrayList<>();

    private String[] deletedKits = new String[0];

    /**
     * For use with the Ajax to indicate and pass back which kit definition is being searched for.
     */
    private String kitDefinitionQueryIndex;
    private String prePopulatedOrganismId;
    private String prepopulatePostReceiveOptions;

    private String orderType;

    private List<CustomizationValues> productCustomizations = new ArrayList();
    private String customPricePlaceholder;
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

    /**
     * @return the list of role names that can modify the order being edited.
     */
    public String[] getModifyOrderRoles() {
        if (editOrder.isPending()) {
            // Allow PMs to modify Pending orders.
            return Role.roles(Role.Developer, Role.PDM, Role.PM, Role.GPProjectManager);
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
                .format(parameters, StandardCharsets.UTF_8);
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
            on = {"!" + LIST_ACTION, "!getQuoteFunding", "!" + VIEW_ACTION, "!" + GET_SAMPLE_DATA})
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

    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, SAVE_SEARCH_DATA, GET_SAMPLE_DATA})
    public void initPreferenceSaver(){
        preferenceSaver.setPreferenceType(PreferenceType.PRODUCT_ORDER_PREFERENCES);
        state = preferenceSaver.getTableState();
    }

    /**
     * Initialize the product with the passed in key for display in the form or create it, if not specified.
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION, GET_SAMPLE_DATA})
    public void editInit() {
        productOrder = getContext().getRequest().getParameter(PRODUCT_ORDER_PARAMETER);
        // If there's no product order parameter, send an error.
        if (StringUtils.isBlank(productOrder)) {
            addGlobalValidationError("No product order was specified.");
        } else {
            editOrder = productOrderDao.findByBusinessKey(productOrder);
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

        // adding customizations in order to allow it to be validated against the quote.
        if (StringUtils.isNotBlank(customizationJsonString)) {
            buildJsonObjectFromEditOrderProductCustomizations();
            editOrder.updateCustomSettings(productCustomizations);
        }

        if(editOrder.getProduct() != null) {

            String salesOrganization=null;
            if (editOrder.hasSapQuote()) {
                SapQuote sapQuote;
                try {
                    sapQuote = editOrder.getSapQuote(sapService);
                    salesOrganization =
                        Optional.ofNullable(sapQuote.getQuoteHeader()).map(QuoteHeader::getSalesOrganization)
                            .orElseThrow(() -> new SAPIntegrationException("Error Obtaining Quote Information."));
                } catch (SAPIntegrationException e) {
                    addGlobalValidationError(e.getMessage());
                    return;
                }
                Optional<SAPMaterial> cachedProduct =
                    Optional.ofNullable(productPriceCache.findByPartNumber(editOrder.getProduct().getPartNumber(),
                        salesOrganization));
                if (!cachedProduct.isPresent()) {
                    addGlobalValidationError("The product you selected " +
                                             editOrder.getProduct().getDisplayName() + " is invalid for your quote " +
                                             sapQuote.getQuoteHeader().getQuoteNumber() +
                                             " because the product is not available for sales organization " +
                                             salesOrganization +
                                             ".  Please check either the selected product or the quote you are using.");
                }
            }
            try {
                editOrder.setOrderType(ProductOrder.determineOrderType(editOrder, salesOrganization));
            } catch (Exception e) {
                addGlobalValidationError(e.getMessage());
            }
        }
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

        Optional<String> skipRegulatoryReason = Optional.ofNullable(editOrder.getSkipRegulatoryReason());

        if (editOrder.getProduct() != null && !editOrder.getProduct().isClinicalProduct()) {
            skipRegulatoryReason.ifPresent(skipReason -> {
                if(ResearchProject.FROM_CLINICAL_CELL_LINE
                        .equals(skipReason)) {
                    addGlobalValidationError("The regulatory selection '"
                                             + ResearchProject.FROM_CLINICAL_CELL_LINE + "' is only valid for Clinical Orders");
                }
            });
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

    protected void doValidation(String action) {
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

        if(action.equals(PLACE_ORDER_ACTION) && editOrder.getProduct() != null && editOrder.getProduct().isClinicalProduct()) {
            requireField(editOrder.isClinicalAttestationConfirmed().booleanValue(),
                    "the checkbox that confirms you have completed requirements to place a clinical order",
                    action);
        }

        try {
            final Optional<Quote> quote = (editOrder.hasQuoteServerQuote())?Optional.ofNullable(validateQuote(editOrder)):Optional.ofNullable(null);
            final Optional<SapQuote> sapQuote = (editOrder.hasSapQuote())?Optional.ofNullable(validateSapQuote(editOrder)):Optional.ofNullable(null);

            if (editOrder.hasSapQuote()) {
                if (sapQuote.isPresent()) {
                    ProductOrder.checkSapQuoteValidity(sapQuote.get());
                    sapQuote.get().getFundingDetails().stream().filter(fundingDetail -> {
                        return fundingDetail.getFundingType() == SapIntegrationClientImpl.FundingType.FUNDS_RESERVATION;
                    }).forEach(fundingDetail -> {
                        if(fundingDetail.getFundingStatus() != FundingStatus.APPROVED ) {
                            addMessage("The funding source %s is considered to be expired and may likely not "
                                       + "work for billing.  Please work on updating the funding sourcd so billing "
                                       + "errors can be avoided");
                        } else {
                            validateGrantEndDate(fundingDetail.getFundingHeaderChangeDate(),
                                    fundingDetail.getItemNumber().toString(),
                                    sapQuote.get().getQuoteHeader().getQuoteNumber() + " -- " +
                                    sapQuote.get().getQuoteHeader().getProjectName());
                        }
                    });
                }
                validateSapQuoteDetails(sapQuote.orElseThrow(() -> new SAPIntegrationException("A Quote was not found for " + editOrder.getQuoteId())), 0);
            } else if (editOrder.hasQuoteServerQuote()) {
                if (quote.isPresent()) {
                    ProductOrder.checkQuoteValidity(quote.get());
                    quote.get().getFunding().stream()
                        .filter(Funding::isFundsReservation)
                        .forEach(funding -> {
                            validateGrantEndDate(funding.getGrantEndDate(),
                                        funding.getDisplayName(), quote.get().getAlphanumericId());
                        });
                }
                validateQuoteDetails(quote.orElseThrow(() -> new QuoteServerException("A quote was not found for " +
                                                                                      editOrder.getQuoteId())), 0);
            }

        } catch (QuoteServerException e) {
            addGlobalValidationError("The quote ''{2}'' is not valid: {3}", editOrder.getQuoteId(), e.getMessage());
            logger.error(e);
        } catch (InvalidProductException | SAPIntegrationException e) {
            addGlobalValidationError("Unable to determine the existing value of open orders for " +
                                     editOrder.getQuoteId() + ": " + e.getMessage());
            logger.error(e);
        }

        if (editOrder != null) {
            validateRinScores(editOrder);

            if (editOrder.getProduct() != null) {
                if (editOrder.getProduct().getBaitLocked() == null || !editOrder.getProduct().getBaitLocked()) {
                    requireField(editOrder.getReagentDesignKey(), "a reagent design", action);
                }
            }
        }
    }

    private void validateGrantEndDate(Date grantEndDate, String grantDisplayName, String quoteIdentifier) {
        if (grantEndDate != null) {
            long numDaysBetween = DateUtils.getNumDaysBetween(new Date(), grantEndDate);
            if (numDaysBetween > 0 && numDaysBetween < 45) {
                addMessage(
                    String.format(
                        "The Funding Source %s on %s  Quote expires in %d days. If it is likely "
                        + "this work will not be completed by then, please work on updating the "
                        + "funding source so billing errors can be avoided.",
                        grantDisplayName, quoteIdentifier, numDaysBetween)
                );
            }
        }
    }

    /**
     * Determines if there is enough funds available on a quote to do any more work based on unbilled samples and funds
     * remaining on the quote
     *  @param productOrder   Identifier for The quote which the user intends to use.  From this we can determine the
     *                  collection of orders to include in evaluating and the funds remaining
     */
    private void validateQuoteDetails(ProductOrder productOrder, boolean countOpenOrders)
            throws InvalidProductException, SAPIntegrationException {

        Quote quote = null;
        SapQuote sapQuote = null;

        if(productOrder.hasSapQuote()) {
            sapQuote = validateSapQuote(productOrder);
            if (sapQuote != null) {
                validateSapQuoteDetails(sapQuote, 0);
            }
        } else {
            quote = validateQuote(productOrder);
            if (quote != null) {
                validateQuoteDetails(quote, 0);
            }
        }
    }

    /**
     * Determines if there is enough funds available on a quote to do any more work based on unbilled samples and funds
     * remaining on the quote.  This particular method will also consider Samples added on the page but not yet
     * refelcted on the order.
     *
     * The scenario for this is when the user clicks on the "Add Samples" button of a placed order
     * @param productOrder   Identifier for The quote which the user intends to use.  From this we can determine the
     *                  collection of orders to include in evaluating and the funds remaining
     * @param additionalSamplesCount Number of extra samples to be considered which are not currently
     */
    private void validateQuoteDetailsWithAddedSamples(ProductOrder productOrder, int additionalSamplesCount)
            throws InvalidProductException, QuoteServerException, SAPIntegrationException {
        Quote quote = null;
        SapQuote sapQuote = null;
        if(productOrder.hasSapQuote()) {
            sapQuote = validateSapQuote(productOrder);
            ProductOrder.checkSapQuoteValidity(sapQuote);
            if(sapQuote != null) {
                validateSapQuoteDetails(sapQuote, additionalSamplesCount);
            }
        } else {
            quote = validateQuote(productOrder);
            ProductOrder.checkQuoteValidity(quote);
            if (quote != null) {
                validateQuoteDetails(quote, additionalSamplesCount);
            }
        }
    }

    /**
     * Determines if there is enough funds available on a quote to do any more work based on unbilled samples and funds
     * remaining on the quote
     *  @param quote  The quote which the user intends to use.  From this we can determine the collection of orders to
     *               include in evaluating and the funds remaining
     * @param additionalSampleCount
     */
    protected void validateQuoteDetails(Quote quote, int additionalSampleCount) throws InvalidProductException,
            SAPIntegrationException {
        if (!canChangeQuote(editOrder, originalQuote, quote.getAlphanumericId())) {
            addGlobalValidationError(SWITCHING_QUOTES_NOT_PERMITTED);
        }
        if (!quote.getApprovalStatus().equals(ApprovalStatus.FUNDED)) {
            String unFundedMessage = "A quote should be funded in order to be used for a product order.";
            addGlobalValidationError(unFundedMessage);
        }

        double fundsRemaining = Double.parseDouble(quote.getQuoteFunding().getFundsRemaining());
        double outstandingEstimate = estimateOutstandingOrders(quote, additionalSampleCount, editOrder);
        double valueOfCurrentOrder = 0;

        if (fundsRemaining <= 0d ||
            (fundsRemaining < (outstandingEstimate+valueOfCurrentOrder))) {
            String inssuficientFundsMessage = "Insufficient funds are available on " + quote.getName();
            addGlobalValidationError( inssuficientFundsMessage);
        }
    }

    /**
     * Determines if there is enough funds available on a quote to do any more work based on unbilled samples and funds
     * remaining on the quote
     *
     * @param quote  The quote which the user intends to use.  From this we can determine the collection of orders to
     *               include in evaluating and the funds remaining
     * @param additionalSampleCount
     */
    protected void validateSapQuoteDetails(SapQuote quote, int additionalSampleCount) throws InvalidProductException,
            SAPIntegrationException {
        if (!canChangeQuote(editOrder, originalQuote, quote.getQuoteHeader().getQuoteNumber())) {
            addGlobalValidationError(SWITCHING_QUOTES_NOT_PERMITTED);
        }
        if (!quote.getQuoteHeader().getQuoteStatus().equals(QuoteStatus.Z4) ||
            !quote.getQuoteHeader().getFundingHeaderStatus().equals(FundingStatus.APPROVED)) {
            String unFundedMessage = "A quote should be approved in order to be used for a product order.";
            addGlobalValidationError(unFundedMessage);
        } else if (!quote.isAllFundingApproved()) {
            String unFundedMessage = "A quote should have all funding sources defined and approved in order to be used for a product order.";
            addGlobalValidationError(unFundedMessage);
        }

        // Validate Products are on the Quote and if they are, store the references to their line items on the order
        try {
            editOrder.updateQuoteItems(quote);

            final Optional<OrderCalculatedValues> sapOrderCalculatedValues =
                    Optional.ofNullable(sapService.calculateOpenOrderValues(additionalSampleCount, quote, editOrder));
            double outstandingEstimate = 0;
            if(sapOrderCalculatedValues.isPresent()) {
                outstandingEstimate =
                        sapOrderCalculatedValues.get().calculateTotalOpenOrderValue().doubleValue();
            }
            BigDecimal fundsRemaining = quote.getQuoteHeader().fundsRemaining();
            if(sapOrderCalculatedValues.isPresent()) {
                fundsRemaining = fundsRemaining.subtract(sapOrderCalculatedValues.get().openDeliveryValues());
            }
            double valueOfCurrentOrder = 0;

            if ((fundsRemaining.compareTo(BigDecimal.ZERO) <= 0)
                || (fundsRemaining.compareTo(BigDecimal.valueOf(outstandingEstimate + valueOfCurrentOrder))<0)) {
                String insufficientFundsMessage = "Insufficient funds are available on " +
                        //todo replace the following with a helper method for quote display
                        quote.getQuoteHeader().getQuoteNumber()+" -- " + quote.getQuoteHeader().getProjectName();
                addGlobalValidationError(insufficientFundsMessage);
            }
        } catch (SAPInterfaceException e) {
            logger.error(e);
            addGlobalValidationError(e.getMessage());
        }
    }

    /**
     * Retrieves and determines the monitary value of a subset of Open Orders within Mercury
     * @return total dollar amount of the monitary value of orders associated with the given quote
     */
    double estimateOutstandingOrders(Quote foundQuote, int addedSampleCount, ProductOrder productOrder)
            throws InvalidProductException {

        //Creating a new array list to be able to remove items from it if need be
        List<ProductOrder> ordersWithCommonQuote = new ArrayList<>();
        if(!EXCLUDED_QUOTES_FROM_VALUE.contains(foundQuote.getAlphanumericId())) {
            ordersWithCommonQuote.addAll(productOrderDao.findOrdersWithCommonQuote(foundQuote.getAlphanumericId()));
        }

        double value = 0d;

        if (productOrder != null && !ordersWithCommonQuote.contains(productOrder)) {

            // This is not a SAP quote.
            ordersWithCommonQuote.add(productOrder);
        }

        return value + getValueOfOpenOrders(ordersWithCommonQuote, foundQuote);
    }

    /**
     * Determines the total monitary value of all unbilled samples on a given list of orders
     *
     * @param ordersWithCommonQuote Subset of orders for which the monitary value is to be determined
     * @param quote
     * @return Total dollar amount which equates to the monitary value of all orders given
     */
    double getValueOfOpenOrders(List<ProductOrder> ordersWithCommonQuote, Quote quote)
            throws InvalidProductException {
        double value = 0d;

        Set<ProductOrder> justParents = new HashSet<>();
        justParents.addAll(ordersWithCommonQuote);

        for (ProductOrder testOrder : justParents) {
            value += getOrderValue(testOrder, testOrder.getUnbilledSampleCount(), quote);
        }
        return value;
    }

    /**
     * Helper method to consolidate the code for evaluating the monitary value of an order based on the price associated
     * with its product(s) and the count of the unbilled samples
     * @param testOrder     Product order for which we wish to determine the monitary value
     * @param sampleCount   unbilled sample count to use for determining the order value.  Passed in separately to account
     *                      for the scenario when we do not want to use the sample count on the order but a sample count
     *                      that will potentially be on the order.
     * @param quote         Quote used in the order for which this price is being derived
     * @return Total monitary value of the order
     */
    double getOrderValue(ProductOrder testOrder, int sampleCount, Quote quote) throws InvalidProductException {
        double value = 0d;
        if(testOrder.getProduct() != null) {
            try {
                final Product product = testOrder.getProduct();
                double productValue =
                        getProductValue(getUnbilledCountForProduct(testOrder, sampleCount, product), product,
                                quote, testOrder);
                value += productValue;
                for (ProductOrderAddOn testOrderAddon : testOrder.getAddOns()) {
                    final Product addOn = testOrderAddon.getAddOn();
                    double addOnValue =
                            getProductValue(getUnbilledCountForProduct(testOrder, sampleCount, addOn), addOn,
                                    quote, testOrder);
                    value += addOnValue;
                }
            } catch (InvalidProductException e) {
                throw new InvalidProductException("For " + testOrder.getBusinessKey() + ": " + testOrder.getName() +
                " " + e.getMessage(), e);
            }
        }
        return value;
    }

    protected int getUnbilledCountForProduct(ProductOrder productOrder, int sampleCount, Product product)
            throws InvalidProductException {
        int unbilledCount = sampleCount;

        final PriceAdjustment adjustmentForProduct = productOrder.getAdjustmentForProduct(product);
        int totalAdjustmentCount = 0;
        if(adjustmentForProduct != null && adjustmentForProduct.getAdjustmentQuantity() != null) {
            totalAdjustmentCount = adjustmentForProduct.getAdjustmentQuantity();
            unbilledCount = (int) ProductOrder.getUnbilledNonSampleCount(productOrder, product, totalAdjustmentCount);
        }

        if (product.getSupportsNumberOfLanes()) {
            int totalCount = productOrder.getLaneCount();
            unbilledCount = (int) ProductOrder.getUnbilledNonSampleCount(productOrder, product, totalCount);
        }
        return unbilledCount;
    }

    /**
     * Based on a product and a sample count, this method will determine the monitary value for the sake of evaluating
     * the monitary value of an order
     *
     * @param unbilledCount count of samples that have not yet been billed
     * @param product       Product from which the price can be determined
     * @param quote         Quote used in the order for which this price is being derived
     * @param productOrder
     * @return Derived value of the Product price multiplied by the number of unbilled samples
     */
    double getProductValue(int unbilledCount, Product product, Quote quote,
                           ProductOrder productOrder) throws InvalidProductException {
        double productValue = 0d;
        String foundPrice;
        try {
            foundPrice = productOrderEjb.validateQuoteAndGetPrice(quote, product, productOrder);
        } catch (InvalidProductException e) {
            throw new InvalidProductException("For '" + product.getDisplayName() + "' " + e.getMessage(), e);
        }

        if (StringUtils.isNotBlank(foundPrice)) {
            Double productPrice = Double.valueOf(foundPrice);

            final PriceAdjustment adjustmentForProduct = productOrder.getAdjustmentForProduct(product);
            int adjustedCount = unbilledCount;
            if(adjustmentForProduct != null) {
                if(adjustmentForProduct.getAdjustmentValue() != null) {
                    productPrice = adjustmentForProduct.getAdjustmentValue().doubleValue();
                }
            }

            productValue = productPrice * (unbilledCount);
        } else {
            throw new InvalidProductException("Price for " + product.getPriceItemDisplayName() +
                                              " for product " + product.getDisplayName() + " was not found.");
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
                    String billingSessionKey = ledger.getBillingSession().getBusinessKey();
                    String billingSessionLink =
                        BillingSessionActionBean.getBillingSessionLink(ledger.getBillingSession(), appConfig);
                    lockedOutOrderStrings.add(String.format("<a href='%s' class='external' target='%s'>%s</a>",
                        billingSessionLink, billingSessionKey, billingSessionKey));
                }
                String lockedOutString = String.join(", ", lockedOutOrderStrings);
                addGlobalValidationError(
                    "The selected orders are locked out by active billing sessions: " + lockedOutString);
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
            on = {EDIT_ACTION, GET_SAMPLE_DATA, GET_SAMPLE_SUMMARY, ADD_SAMPLES_ACTION, SET_RISK, RECALCULATE_RISK, ABANDON_SAMPLES_ACTION,
                    DELETE_SAMPLES_ACTION, PLACE_ORDER_ACTION, VALIDATE_ORDER, UNABANDON_SAMPLES_ACTION, REPLACE_SAMPLES})
    public void entryInit() {
        if (editOrder != null) {
            productOrderListEntry = editOrder.isDraft() ? ProductOrderListEntry.createDummy() :
                    orderListEntryDao.findSingle(editOrder.getJiraTicketKey());

            ProductOrder.loadLabEventSampleData(editOrder.getSamples());

            Optional<String> skipRegulatoryReason = Optional.ofNullable(editOrder.getSkipRegulatoryReason());
            skipRegulatoryReason.ifPresent(reason -> {
                if (ResearchProject.FROM_CLINICAL_CELL_LINE.equals(reason)) {
                    fromClinicalLine = true;
                } else if (ResearchProject.NOT_FROM_HUMANS_REASON_FILL.equals(reason)) {
                    notFromHumans = true;
                } else if (ResearchProject.SAMPLE_MANIPULATION_ONLY.equals(reason)) {
                    sampleManipulationOnly = true;
                }
            });

            sampleDataSourceResolver.populateSampleDataSources(editOrder);

            if(editOrder.getOrderType() != null) {
                orderType = editOrder.getOrderType().getDisplayName();
            }

            populateAttributes(editOrder.getProductOrderId());

            buildCustomizationHelper();

            QuotePriceItem priceItemByKeyFields = null;
            if (editOrder.getProduct() != null) {
                if (editOrder.getProduct().getPrimaryPriceItem() != null) {
                    priceItemByKeyFields = priceListCache.findByKeyFields(editOrder.getProduct().getPrimaryPriceItem());
                    if (priceItemByKeyFields != null) {

                        //todo this will nolonger be necessary once units are added to the Product
                        editOrder.getProduct().getPrimaryPriceItem().setUnits(priceItemByKeyFields.getUnit());
                    }
                }
            }

            for (ProductOrderAddOn productOrderAddOn : editOrder.getAddOns()) {
                if (productOrderAddOn.getAddOn().getPrimaryPriceItem() != null) {
                    final QuotePriceItem addOnPriceItemByKeyFields =
                            priceListCache.findByKeyFields(productOrderAddOn.getAddOn().getPrimaryPriceItem());
                    if (addOnPriceItemByKeyFields != null ) {
                        //todo this will nolonger be necessary once units are added to the Product
                        productOrderAddOn.getAddOn().getPrimaryPriceItem().setUnits(addOnPriceItemByKeyFields.getUnit());
                    }
                }
            }
        }
    }

    @After(stages = LifecycleStage.BindingAndValidation, on = {VIEW_ACTION})
    public void viewPageInit() {
        buildCustomizationHelper();
    }

    public void buildCustomizationHelper() {
        try {
            buildJsonCustomizationsFromProductOrder(editOrder);
        } catch (JSONException e) {
            if (userBean.isGPPMUser() || userBean.isPDMUser() || userBean.isDeveloperUser()) {
                addGlobalValidationError("Unable to render the Previously Defined CustomizationValues");
            }
        }
    }

    private void buildJsonCustomizationsFromProductOrder(ProductOrder editOrder) throws JSONException {

        JSONObject customizationOutput = new JSONObject();

        if(editOrder.getSinglePriceAdjustment() != null) {

            CustomizationValues primaryCustomization =
                    new CustomizationValues(editOrder.getProduct().getPartNumber(),
                            String.valueOf((editOrder.getSinglePriceAdjustment().getAdjustmentQuantity() != null?editOrder.getSinglePriceAdjustment().getAdjustmentQuantity():"")),
                            (editOrder.getSinglePriceAdjustment().getAdjustmentValue() != null)?editOrder.getSinglePriceAdjustment().getAdjustmentValue().toString():"",
                            editOrder.getSinglePriceAdjustment().getCustomProductName());
            customizationOutput.put(editOrder.getProduct().getPartNumber(), primaryCustomization.toJson());
        }

        for (ProductOrderAddOn productOrderAddOn : editOrder.getAddOns()) {
            if(productOrderAddOn.getSingleCustomPriceAdjustment() != null) {

                CustomizationValues addonCustomization =
                        new CustomizationValues(productOrderAddOn.getAddOn().getPartNumber(),
                                String.valueOf(
                                        (productOrderAddOn.getSingleCustomPriceAdjustment().getAdjustmentQuantity() != null)?productOrderAddOn.getSingleCustomPriceAdjustment().getAdjustmentQuantity():""),
                                (productOrderAddOn.getSingleCustomPriceAdjustment().getAdjustmentValue() != null)?productOrderAddOn.getSingleCustomPriceAdjustment().getAdjustmentValue().toString():"",
                                productOrderAddOn.getSingleCustomPriceAdjustment().getCustomProductName());

                customizationOutput.put(productOrderAddOn.getAddOn().getPartNumber(),addonCustomization.toJson());
            }
        }

        if(customizationOutput.length() > 0) {
            customizationJsonString = customizationOutput.toString();
        }
    }

    private void validateUser(String validatingFor) {
        if (!userBean.ensureUserValid()) {
            addGlobalValidationError(MessageFormat.format(UserBean.LOGIN_WARNING, validatingFor + " an order"));
        }
    }

    @HandlesEvent("getQuoteFunding")
    public Resolution getQuoteFunding() throws Exception {
        productOrder = getContext().getRequest().getParameter(PRODUCT_ORDER_PARAMETER);
        if (!StringUtils.isBlank(productOrder)) {
            editOrder = productOrderDao.findByBusinessKey(productOrder);
        }
        JSONObject item = quoteDetailsHelper.getQuoteDetailsJson(this, quoteIdentifier, originalQuote);
        return new StreamingResolution("text/json", item.toString());
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(ORDER_LIST_PAGE);
    }

    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (editOrder == null) {
            addGlobalValidationError("A PDO named '" + Encode.forHtml(productOrder) +
                    "' could not be found.");
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
    protected void populateTokenListsFromObjectData() {
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

        if (editOrder.hasSapQuote()) {
            productOrderEjb.publishProductOrderToSAP(editOrder,placeOrderMessageCollection, true);
        } else {
            addGlobalValidationError("This order does not have an SAP quote so it is not eligible to be an SAP order");
        }

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

            if (editOrder.hasSapQuote()) {
                productOrderEjb.publishProductOrderToSAP(editOrder, placeOrderMessageCollection, true);
            }
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
        String originalBusinessKey = editOrder.getBusinessKey();

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
            /*
            Validate Quote order values and send a warning if there is an issue
            */
            validateQuoteDetails(editOrder, !editOrder.hasJiraTicketKey());
        } catch (InvalidProductException | SAPIntegrationException ipe) {
            addGlobalValidationError("Unable to determine the existing value of open orders for " + editOrder.getQuoteId() +": " +ipe.getMessage());
        }
        try {
            productOrderEjb.persistProductOrder(saveType, editOrder, deletedIdsConverted, kitDetails, saveOrderMessageCollection);
            originalBusinessKey = null;


            if (isInfinium() && editOrder.getPipelineLocation() == null) {
                editOrder.setPipelineLocation(ProductOrder.PipelineLocation.US_CLOUD);
                productOrderDao.persist(editOrder);
            }
            addMessages(saveOrderMessageCollection);
            addMessage("Product Order \"{0}\" has been saved.", editOrder.getTitle());
        } catch (SAPInterfaceException e) {
            if (originalBusinessKey != null) {
                productOrderDao.clear();
                editOrder = productOrderDao.findByBusinessKey(originalBusinessKey);
            }

            addGlobalValidationError(e.getMessage());
            getSourcePageResolution();
        }
        if (chipDefaults != null && attributes != null) {
            if (!chipDefaults.equals(attributes)) {
                genotypingProductOrderMapping
                        = productOrderEjb.findOrCreateGenotypingChipProductOrderMapping(editOrder.getProductOrderId());
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
            } else {
                // Values for attributes are the same as the defaults - Delete any overrides
                genotypingProductOrderMapping
                        = productOrderEjb.findOrCreateGenotypingChipProductOrderMapping(editOrder.getProductOrderId());
                if (genotypingProductOrderMapping != null) {
                    attributeArchetypeDao.remove(genotypingProductOrderMapping);
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

        if(editOrder.isPriorToSAP1_5() && editOrder.isSavedInSAP() && editOrder.hasAtLeastOneBilledLedgerEntry() &&
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
        if(product != null) {
            Product.setMaterialOnProduct(product, productPriceCache);
        }
        List<Product> addOnProducts = productDao.findByPartNumbers(addOnKeys);

        for (Product addOnProduct : addOnProducts) {
            Product.setMaterialOnProduct(addOnProduct, productPriceCache);
        }

        List<Product> allProducts = new ArrayList<>();

        addOnProducts.stream().filter(Objects::nonNull).forEach(allProducts::add);
        if(product !=null) {
            allProducts.add(product);
        }

        try {
            updateAndValidateQuoteSource(allProducts);
            editOrder.updateData(project, product, addOnProducts, stringToSampleListExisting(sampleList));
        } catch (InvalidProductException e) {
            addGlobalValidationError(e.getMessage());
        }

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
                    ProductOrderEjb.SampleDeliveryStatusChangeException | IOException | SAPInterfaceException e) {
                addGlobalValidationError(e.getMessage());
                return getSourcePageResolution();
            }
        }

        return new RedirectResolution(ProductOrderActionBean.class, LIST_ACTION);
    }

    @NotNull
    private JSONArray supportsGetAddOns(Product product, SapQuote sapQuote,
                                        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode) throws JSONException, SAPIntegrationException {
        JSONArray itemList = new JSONArray();
        if (product != null) {
            for (Product addOn : product.getAddOns(userBean)) {
                JSONObject item = new JSONObject();
                item.put("key", addOn.getBusinessKey());
                item.put("value", addOn.getProductName());
                addProductPriceToJson(addOn, sapQuote, companyCode, item);
                itemList.put(item);
            }
        }
        return itemList;
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
                        final Optional<OrspProject> orspRegInfo = Optional.ofNullable(orspProjectDao.findByKey(regulatoryInfo.getIdentifier()));

                        boolean userEdit = true;
                        if(orspRegInfo.isPresent()) {
                            final RegulatoryInfo comparisonRegInfo = new RegulatoryInfo(orspRegInfo.get().getName(),
                                    orspRegInfo.get().getType(), orspRegInfo.get().getProjectKey());

                            userEdit = !regulatoryInfo.equals(comparisonRegInfo);
                        }
                        regulatoryInfoJson.put("userEdit", userEdit);
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
        JSONArray jsonErrors = new JSONArray();

        // Access sample list directly in order to suggest based on possibly not-yet-saved sample IDs.
        List<ProductOrderSample> productOrderSamples=new ArrayList<>();
        if (!getSampleList().isEmpty()) {
            try {
                productOrderSamples = stringToSampleListExisting(getSampleList());

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
            } catch (Exception e) {
                jsonErrors.put(e.getMessage());
            }
            // Fetching RP here instead of a @Before method to avoid the fetch when it won't be used.
            ResearchProject researchProject = researchProjectDao.findByBusinessKey(researchProjectKey);
            Map<String, RegulatoryInfo> regulatoryInfoByIdentifier = researchProject.getRegulatoryByIdentifier();
            jsonResults = orspProjectToJson(productOrderSamples, regulatoryInfoByIdentifier);
        }
        if (jsonErrors.length() > 0) {
            jsonResults.put(new HashMap<String, JSONArray>() {{
                put("errors", jsonErrors);
            }});
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

    /**
     * Get JSON for PDO Samples.
     *
     * This method streams data back to the client as it is generated rather then collecting it all in an array
     * and sending it back.<br/>This method also takes into account several factors when deciding which data to return:
     * <ul>
     *     <li><b>preferenceSaver.visibleColumns()</b> <code>Collection&lt;String&gt;</code>: Return data only for visible columns</li>
     *     <li>POST/GET Parameter <b>sampleIdsForGetBspData</b> <code>String[]</code>: Which PDO sampleIds to receive data for if specified. Otherwise,
     *     all PDO Samples for this PDO are returned</li>
     *     <li>POST/GET Parameter <b>initialLoad</b> <code>Boolean</code>, <br/><ul><li>If true it is the initial call populating the DataTable. When true, sample data is returned only for the first page (which is saved in the tableState preference)</li>
     *     <li>If false, all sample data is returned</li></ul></li>
     *     <li>POST/GET Parameter <b>includeSampleSummary</b> <code>Boolean</code>, If true, return sample summary information. It is included in this JSON in order to prevent extra call to BSP for sample data</li>
     * </ul>
     *
     * The point of this method is to speed up the fetching of results by including all data only for the first page.
     * By limiting the data returned in the initial page load, the UI appears to be much snappier. Subsequent ajax calls
     * must be made to load the reamaining data.
     *
     * Note: Since Mercury does not currently do server-side sorting or filtering the subset of samples returned may
     * not necessarily coincide with the first page displayed in the UI so the page may show gaps. However, since these
     * are batch fetches, the gaps will be filled in with subsequent calls.
     */
    @HandlesEvent(GET_SAMPLE_DATA)
    public Resolution getSampleData() {
        Resolution resolution = new StreamingResolution("text/json"){
            @Override
            protected void stream(HttpServletResponse response)  throws IOException{
                List<ProductOrderSample> samples;
                if (sampleIdsForGetBspData != null) {
                    samples = productOrderSampleDao.findListByList(
                            ProductOrderSample.class, ProductOrderSample_.productOrderSampleId, sampleIdsForGetBspData);
                } else {
                    samples = editOrder.getSamples();
                }
                final Collection<String> allVisibleColumns = preferenceSaver.visibleColumns();

                Set<String> bspColumns = Sets.newHashSet(ProductOrderSampleBean.COLLABORATOR_SAMPLE_ID,
                    ProductOrderSampleBean.PARTICIPANT_ID, ProductOrderSampleBean.COLLABORATOR_PARTICIPANT_ID,
                    ProductOrderSampleBean.SAMPLE_TYPE, ProductOrderSampleBean.MATERIAL_TYPE,
                    ProductOrderSampleBean.VOLUME, ProductOrderSampleBean.SHIPPED_DATE,
                    ProductOrderSampleBean.CONCENTRATION, ProductOrderSampleBean.RIN, ProductOrderSampleBean.RQS,
                    ProductOrderSampleBean.DV2000, ProductOrderSampleBean.PICO_RUN_DATE,
                    ProductOrderSampleBean.RACKSCAN_MISMATCH, ProductOrderSampleBean.RECEIVED_DATE);

                boolean withSampleData = false;
                for (String visibleColumn : allVisibleColumns) {
                    if (bspColumns.contains(visibleColumn)) {
                        withSampleData = true;
                        break;
                    }
                }
                JsonFactory jsonFactory = new JsonFactory();
                JsonGenerator jsonGenerator = null;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    OutputStream outputStream = response.getOutputStream();
                    jsonGenerator = jsonFactory.createJsonGenerator(outputStream);
                    jsonGenerator.setCodec(objectMapper);
                    jsonGenerator.writeStartObject();
                    jsonGenerator.writeObjectField(ProductOrderSampleBean.RECORDS_TOTAL, samples.size());
                    jsonGenerator.writeArrayFieldStart(ProductOrderSampleBean.DATA_FIELD);
                    int rowsWithSampleData=0;
                    if (initialLoad){
                        List<ProductOrderSample> firstPage = getPageOneSamples(state, samples);
                        writeProductOrderSampleBean(jsonGenerator, firstPage, true, initialLoad, preferenceSaver);
                        rowsWithSampleData = firstPage.size();
                        List<ProductOrderSample> otherPages = new ArrayList<>(samples);
                        otherPages.removeAll(firstPage);
                        if (CollectionUtils.isNotEmpty(otherPages)) {
                            writeProductOrderSampleBean(jsonGenerator, otherPages, false, initialLoad, preferenceSaver);
                        }
                    } else {
                        if (withSampleData) {
                            rowsWithSampleData = samples.size();
                        }
                        writeProductOrderSampleBean(jsonGenerator, samples, withSampleData, true, preferenceSaver);
                    }
                    jsonGenerator.writeEndArray();
                    jsonGenerator.writeObjectField(ProductOrderSampleBean.SAMPLE_DATA_ROW_COUNT, rowsWithSampleData);
                    jsonGenerator.writeEndObject();
                } catch (BSPLookupException e) {
                    handleBspLookupFailed(e);
                } catch (Exception e){
                    logger.error(e);
                } finally {
                    if (jsonGenerator!=null && !jsonGenerator.isClosed()) {
                        try { jsonGenerator.close(); } catch (IOException e2) {}
                    }
                }
            }
        };
        return resolution;
    }

    @HandlesEvent(GET_SAMPLE_SUMMARY)
    public Resolution getSampleSummary() {
        Resolution resolution = new StreamingResolution("text/json"){
            @Override
            protected void stream(HttpServletResponse response)  throws IOException{
                List<ProductOrderSample> samples = editOrder.getSamples();

                JsonFactory jsonFactory = new JsonFactory();
                JsonGenerator jsonGenerator = null;
                try {
                    ObjectMapper objectMapper = new ObjectMapper();
                    OutputStream outputStream = response.getOutputStream();
                    jsonGenerator = jsonFactory.createJsonGenerator(outputStream);
                    jsonGenerator.setCodec(objectMapper);
                    jsonGenerator.writeStartObject();
                    List<String> comments = new ArrayList<>();
                    String samplesNotReceivedString = "";
                    try {
                        comments = editOrder.getSampleSummaryComments();
                        samplesNotReceivedString = getSamplesNotReceivedString();
                    } catch (Exception e) {
                        logger.error("Could not get sample summary.", e);
                    }
                    jsonGenerator.writeArrayFieldStart("summary");
                    for (String comment : comments) {
                        jsonGenerator.writeObject(comment);
                    }
                    jsonGenerator.writeEndArray();
                    jsonGenerator.writeObjectField(ProductOrderSampleBean.SAMPLES_NOT_RECEIVED, samplesNotReceivedString);
                    jsonGenerator.writeEndObject();
                } catch (BSPLookupException e) {
                    handleBspLookupFailed(e);
                } catch (Exception e){
                    logger.error(e);
                } finally {
                    if (jsonGenerator!=null) {
                        IOUtils.closeQuietly(jsonGenerator);
                    }
                }
            }
        };
        return resolution;
    }

    /**
     * Returns the first page's worth of samples based on the current page length of the datatable. This method always
     * returns  0 .. page length (or All, if that is what is selected).
     */
    static List<ProductOrderSample> getPageOneSamples(State state, List<ProductOrderSample> samples) {
        List<ProductOrderSample> results;
        int length = state.getLength();
        if (length == State.ALL_RESULTS) {
            results = samples;
        } else {
            if (length > samples.size()) {
                length = samples.size();
            }
            results = samples.subList(0, length);
        }
        return results;
    }

    private void writeProductOrderSampleBean(JsonGenerator jsonGenerator, List<ProductOrderSample> productOrderSamples,
                                             final boolean includeSampleData, boolean initialLoad,
                                             final DatatablesStateSaver preferenceSaver) throws IOException {
        if (includeSampleData) {
            ProductOrder.loadSampleData(productOrderSamples);
            if (preferenceSaver.showColumn(ProductOrderSampleBean.SHIPPED_DATE) ||
                preferenceSaver.showColumn(ProductOrderSampleBean.RECEIVED_DATE)) {
                ProductOrder.loadLabEventSampleData(productOrderSamples);
            }
        }
        for (ProductOrderSample sample : productOrderSamples) {
            SampleLink sampleLink = null;
            if (sample.isInBspFormat()) {
                try {
                    sampleLink = getSampleLink(sample);
                } catch (Exception e) {
                    logger.error("Could not get sample link", e);
                }
            }
            ProductOrderSampleBean bean =
                new ProductOrderSampleBean(sample, includeSampleData, initialLoad, preferenceSaver, sampleLink);
            jsonGenerator.writeObject(bean);

        }
    }

    @HandlesEvent("getSupportsSkippingQuote")
    public Resolution getSupportsSkippingQuote() throws Exception {
        JSONObject item = new JSONObject();
        Product productEntity = null;
        if (!StringUtils.isEmpty(this.product)) {
            productEntity = productDao.findByBusinessKey(this.product);
        }
        supportsGetSupportsSkippingQuote(item, productEntity);
        return createTextResolution(item.toString());
    }

    private void supportsGetSupportsSkippingQuote(JSONObject item, Product productEntity) throws JSONException {
        boolean supportsSkippingQuote = false;
        if (!StringUtils.isEmpty(product)) {
            supportsSkippingQuote = productEntity.getSupportsSkippingQuote();
        }
        item.put(Product.SUPPORTS_SKIPPING_QUOTE, supportsSkippingQuote);
    }

    @HandlesEvent("getProductInfo")
    public Resolution getProductInfo() throws Exception {
        Product productEntity = null;
        if(StringUtils.isNotEmpty(product)) {
            productEntity = productDao.findByBusinessKey(product);
        }

        SapQuote sapQuote = null;

        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        if(StringUtils.isNotBlank(quoteIdentifier)) {
            if(StringUtils.isNumeric(quoteIdentifier)) {
                sapQuote = sapService.findSapQuote(quoteIdentifier);
//                if(Arrays.asList(SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES,
//                        SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD).contains(
//                        SapIntegrationClientImpl.SAPCompanyConfiguration.fromSalesOrgForMaterial(sapQuote.getQuoteHeader().getSalesOrganization())
//                )) {
                   companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.fromSalesOrgForMaterial(sapQuote.getQuoteHeader().getSalesOrganization());
//                }
            }
        }

        JSONObject productInfo = new JSONObject();
        if (productEntity != null) {
            supportGetSupportsNumberOfLanes(productInfo, productEntity);
            supportsGetSupportsSkippingQuote(productInfo, productEntity);
            productInfo.put("addOns", supportsGetAddOns(productEntity, sapQuote, companyCode));

            productInfo.put("clinicalProduct", productEntity.isClinicalProduct());
            productInfo.put("externalProduct", productEntity.isExternalOnlyProduct() ||
                                               (companyCode == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES &&
                                                !productEntity.isClinicalProduct()));
            productInfo.put("productName", productEntity.getName());
            productInfo.put("baitLocked", productEntity.getBaitLocked());
            addProductPriceToJson(productEntity, sapQuote, companyCode, productInfo);
        }

        return createTextResolution(productInfo.toString());
    }

    /**
     * Helper method to support multiple layers of retrieving JSON representation of pricing for a product.  This
     * replaces code that was duplicated when getting this information for both the primary product and the addon
     * product of a product order
     * @param productEntity Product for which we wish to obtain pricing info
     * @param sapQuote      SAP quote (if valid) which is associated with the order on which the product is defined
     * @param companyCode   represents if this order will be sold as SSF or LLC
     * @param productInfo   JSON Object into which the pricing information will be stored
     * @throws JSONException
     */
    public void addProductPriceToJson(Product productEntity, SapQuote sapQuote,
                                      SapIntegrationClientImpl.SAPCompanyConfiguration companyCode,
                                      JSONObject productInfo) throws JSONException {
        String priceTitle = "researchListPrice";
        if(sapQuote != null &&
           StringUtils.equals(sapQuote.getQuoteHeader().getSalesOrganization(),
                   SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES.getSalesOrganization())) {
            priceTitle = "externalListPrice";
        }
        if (productEntity.isExternalOnlyProduct()) {
            priceTitle = "externalListPrice";
        }
        if(productEntity.isClinicalProduct()) {
            priceTitle = "clinicalPrice";
        }
        productInfo.put("productAgp", productEntity.getDefaultAggregationParticle());
        BigDecimal priceForFormat = null;
        if (sapQuote != null) {
            priceForFormat = new BigDecimal(productPriceCache.findByPartNumber(productEntity.getPartNumber(),
                    companyCode.getSalesOrganization()).getBasePrice());
        } else {
            Optional<PriceItem> primaryPriceItem = Optional.ofNullable(productEntity.getPrimaryPriceItem());
            if(primaryPriceItem.isPresent()) {
                priceForFormat =
                        new BigDecimal(priceListCache.findByKeyFields(primaryPriceItem.get()).getPrice());
            }
        }
        String formattedPrice = "";
        if (priceForFormat != null) {
            formattedPrice = NumberFormat.getCurrencyInstance().format(priceForFormat);
        }
        productInfo.put(priceTitle, formattedPrice);
    }

    @HandlesEvent("getSupportsNumberOfLanes")
    public Resolution getSupportsNumberOfLanes() throws Exception {
        JSONObject item = new JSONObject();

        if(StringUtils.isNotBlank(product)) {

            Product productEntity = productDao.findByBusinessKey(product);

            if (productEntity != null) {
                supportGetSupportsNumberOfLanes(item, productEntity);
            }
        }

        return createTextResolution(item.toString());
    }

    private void supportGetSupportsNumberOfLanes(JSONObject item, Product productToFind) throws JSONException {
        List<String> selectedOrderAddons = null;
        if(selectedAddOns != null) {
            selectedOrderAddons = Arrays.asList(StringUtils.split(selectedAddOns, "|@|"));
        }

        boolean supportsNumberOfLanes = false;
        if (product != null) {
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
    }

    @ValidationMethod(on = "deleteOrder")
    public void validateDeleteOrder() {
        if (!editOrder.isDraft()) {
            addGlobalValidationError("Orders can only be deleted in draft mode");
        }
    }

    @ValidationMethod(
            on = {DELETE_SAMPLES_ACTION, ABANDON_SAMPLES_ACTION, SET_RISK, RECALCULATE_RISK, ADD_SAMPLES_TO_BUCKET,
                    UNABANDON_SAMPLES_ACTION, SET_PROCEED_OOS}, priority = 0)
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
            MessageCollection abandonSamplesMessageCollection = new MessageCollection();
            productOrderEjb.abandonSamples(editOrder.getJiraTicketKey(), selectedProductOrderSamples, abandonComment,
                    abandonSamplesMessageCollection);
            addMessage("Abandoned samples: {0}.",
                    StringUtils.join(ProductOrderSample.getSampleNames(selectedProductOrderSamples), ", "));


            productOrderEjb.updateOrderStatus(editOrder.getJiraTicketKey(), this);

            addMessages(abandonSamplesMessageCollection);
        } else {
            addMessage("You cannot abandon samples since have not selected any samples that are eligible to abandon");
        }
        return createViewResolution(editOrder.getBusinessKey());
    }

    @HandlesEvent(UNABANDON_SAMPLES_ACTION)
    public Resolution unAbandonSamples() throws Exception {

        if (CollectionUtils.isNotEmpty(selectedProductOrderSampleIds)) {
            MessageCollection abandonSamplesMessageCollection = new MessageCollection();
            try {
                productOrderEjb.unAbandonSamples(editOrder.getJiraTicketKey(), selectedProductOrderSampleIds,
                        unAbandonComment, abandonSamplesMessageCollection);
            } catch (ProductOrderEjb.SampleDeliveryStatusChangeException e) {
                addGlobalValidationError(e.getMessage());
                return new ForwardResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter(
                        PRODUCT_ORDER_PARAMETER,
                        editOrder.getBusinessKey());
            }

            productOrderEjb.updateOrderStatus(editOrder.getJiraTicketKey(), this);

            addMessages(abandonSamplesMessageCollection);
        }
        return createViewResolution(editOrder.getBusinessKey());
    }

    @ValidationMethod(on = ADD_SAMPLES_ACTION)
    public void addSampleExtraValidations() throws Exception {
        try {
            validateQuoteDetailsWithAddedSamples(editOrder, stringToSampleList(addSamplesText).size());
        } catch (QuoteServerException e) {
            addGlobalValidationError("The quote ''{2}'' is not valid: {3}", editOrder.getQuoteId(), e.getMessage());
        } catch (InvalidProductException | SAPIntegrationException e) {
            addGlobalValidationError("Unable to determine the existing value of open orders for " + editOrder.getQuoteId() +": " +e.getMessage());
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

    public String getSapQuoteUrl(String quoteIdentifier) {
        return sapQuoteLink.sapUrl(quoteIdentifier);
    }

    public String getSapQuoteUrl() {
        return sapQuoteLink.sapUrl(editOrder.getQuoteId());
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

    @HandlesEvent(OPEN_CUSTOM_VIEW_ACTION)
    public Resolution openCustomView() throws Exception {
        buildJsonObjectFromEditOrderProductCustomizations();

        return new ForwardResolution(CUSTOMIZE_PRODUCT_ASSOCIATIONS);
    }

    private void buildJsonObjectFromEditOrderProductCustomizations() throws JSONException, SAPIntegrationException {
        SapQuote sapQuote = null;
        if(StringUtils.isNotBlank(quoteIdentifier)) {
            if (StringUtils.isNumeric(quoteIdentifier)) {
                sapQuote = sapService.findSapQuote(quoteIdentifier);
            }
        }
        JSONObject customizationJson = new JSONObject(customizationJsonString);

        final Iterator keys = customizationJson.keys();

        while(keys.hasNext()) {
            String productPartNumber = (String)keys.next();

            JSONObject currentCustomization = (JSONObject) customizationJson.get(productPartNumber);

            final Product product = productDao.findByPartNumber(productPartNumber);

            final CustomizationValues customizedProductInfo = new CustomizationValues(productPartNumber,
                    (String) ((currentCustomization.has("quantity") && currentCustomization.get("quantity") != null) ?currentCustomization.get("quantity"):""),
                    (String) ((currentCustomization.has("price") && currentCustomization.get("price") != null) ?currentCustomization.get("price"):""),
                    (String) ((currentCustomization.has("customName") && currentCustomization.get("customName") != null) ?currentCustomization.get("customName"):""));
            customizedProductInfo.setProductName(product.getProductName());

            QuotePriceItem priceListItem = null;
            BigDecimal formatedPrice = null;

            if (sapQuote != null) {
                formatedPrice = new BigDecimal(productPriceCache.findByProduct(product,
                        sapQuote.getQuoteHeader().getSalesOrganization()).getBasePrice());
            } else {
                if (product.getPrimaryPriceItem() != null) {
                    priceListItem = priceListCache.findByKeyFields(product.getPrimaryPriceItem());
                    formatedPrice = new BigDecimal(priceListItem.getPrice());
                }
            }

            customizedProductInfo.setUnits(product.getUnitsDisplay());
            customizedProductInfo.setOriginalPrice(NumberFormat.getCurrencyInstance().format(formatedPrice));
            productCustomizations.add(customizedProductInfo);
        }
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

    public String getOriginalQuote() {
        return originalQuote;
    }

    public void setOriginalQuote(String originalQuote) {
        this.originalQuote = originalQuote;
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

    public boolean isIncludeSampleSummary() {
        return includeSampleSummary;
    }

    public void setIncludeSampleSummary(boolean includeSampleSummary) {
        this.includeSampleSummary = includeSampleSummary;
    }

    public boolean isInitialLoad() {
        return initialLoad;
    }

    public void setInitialLoad(boolean initialLoad) {
        this.initialLoad = initialLoad;
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
        return getUserBean().isDeveloperUser() || getUserBean().isPMUser() || getUserBean().isPDMUser() || getUserBean().isGPPMUser();
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

    @Inject
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
            boolean hasQuote = StringUtils.isNotBlank(editOrder.getQuoteId());
            requireField(hasQuote || editOrder.canSkipQuote(), "a quote specified", action);
            if (!hasQuote && editOrder.allowedToSkipQuote()) {
                requireField(editOrder.getSkipQuoteReason() , "an explanation for why a quote cannot be entered", action);
            }
        }
    }

    /**
     * Helper method to interpret the quote ID (if entered) and set the Quote Source based on its makeup:
     * <ul><li>If the quote ID is all numeric, it must be SAP</li>
     * <li>otherwise, consider it to be Quote Server</li></ul>
     *
     * If the quote is SAP and the products are set on the order, check to determine if all of the products on the
     * order (primary and add ons) are offered in the sales org of the quote.  If not, show an error that the quote is
     * not compatible with the selected products
     */
    void updateAndValidateQuoteSource(List<Product> products) throws InvalidProductException {
        if (StringUtils.isNotBlank(editOrder.getQuoteId())) {
            if (StringUtils.isNumeric(editOrder.getQuoteId())) {
                try {
                    Optional<SapQuote> sapQuote = Optional.ofNullable(sapService.findSapQuote(editOrder.getQuoteId()));

                    if(sapQuote.isPresent()) {
                        SapQuote quote = sapQuote.get();

                        String salesOrganization = quote.getQuoteHeader().getSalesOrganization();
                        final Optional<SapIntegrationClientImpl.SAPCompanyConfiguration> sapCompanyConfiguration =
                                Optional.ofNullable(SapIntegrationClientImpl.SAPCompanyConfiguration
                                        .fromSalesOrgForMaterial(salesOrganization));

                        for (Product orderProduct : products) {

                            Optional<Product> currentProduct = Optional.ofNullable(orderProduct);

                            if (currentProduct.isPresent()) {
                                Optional<SAPMaterial> materialForSalesOrg = Optional.ofNullable(productPriceCache
                                        .findByProduct(currentProduct.get(), salesOrganization));
                                final String errorMessage = String.format("%s is invalid for your quote %s because "
                                                                          + "the product is not available for that quotes sales"
                                                                          + " organization (%s).  Please check either"
                                                                          + " the selected product or the quote you"
                                                                          + " are using.",
                                        currentProduct.get().getDisplayName(), quote.getQuoteHeader().getQuoteNumber(),
                                        sapCompanyConfiguration.orElse(SapIntegrationClientImpl.SAPCompanyConfiguration.UNKNOWN).getDisplayName());
                                if (!materialForSalesOrg.isPresent()) {
                                    throw new InvalidProductException(errorMessage);
                                } else {
                                    if(sapCompanyConfiguration.isPresent()) {
                                        SapIntegrationClientImpl.SAPCompanyConfiguration companyConfig = sapCompanyConfiguration.get();
                                        if(companyConfig == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES &&
                                           !orderProduct.isLLCProduct() &&
                                           !orderProduct.getOfferedAsCommercialProduct()) {
                                            throw new InvalidProductException(errorMessage);
                                        } else if (companyConfig == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD &&
                                                   orderProduct.isLLCProduct()) {

                                            throw new InvalidProductException(errorMessage);
                                        }
                                    }
                                }
                            }
                        }
                    }

                } catch (SAPIntegrationException e) {
                    throw new InvalidProductException("The quote you are attempting to switch to is invalid.");
                }
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
            if (isNotFromHumans() || isFromClinicalLine()) {
                requireField(editOrder.canSkipRegulatoryRequirements(),
                        "a reason for bypassing the regulatory requirements", action);
            }
            if (editOrder.isRegulatoryInfoEditAllowed() && editOrder.getResearchProject() != null) {
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
                        Optional<ResearchProject> safeResearchProject = Optional.ofNullable(editOrder.getResearchProject());
                        addGlobalValidationError("Regulatory info {2} is not associated with research project {3}",
                                StringUtils.join(missingRegulatoryRequirements, ", "),
                                safeResearchProject.map(ResearchProject::getName).orElse(""));
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


    public boolean isNotFromHumans() {
        return notFromHumans;
    }

    public void setNotFromHumans(boolean notFromHumans) {
        this.notFromHumans = notFromHumans;
    }

    public boolean isFromClinicalLine() {
        return fromClinicalLine;
    }

    public void setFromClinicalLine(boolean fromClinicalLine) {
        this.fromClinicalLine = fromClinicalLine;
    }

    public boolean isSampleManipulationOnly() {
        return sampleManipulationOnly;
    }

    public void setSampleManipulationOnly(boolean sampleManipulationOnly) {
        this.sampleManipulationOnly = sampleManipulationOnly;
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
     * get HTML fragment summarizing samples receivred.
     */
    public String getSamplesNotReceivedString() {
        int samplesNotReceived=0;
        String result = "N/A";
        try {
            samplesNotReceived = editOrder.getSampleCount() - editOrder.getReceivedSampleCount();
        } catch (BSPLookupException e) {
            handleBspLookupFailed(e);
        }
        if (samplesNotReceived == 1) {
            result = "<em>NOTE:</em> There is one sample that has not yet been received. If the order is placed, "
                     + "this sample will be removed from the order.";

        } else if (samplesNotReceived > 1) {
            result = String.format("<em>NOTE:</em> There are %s samples that have not yet been received. If the order "
                                   + "is placed, these samples will be removed from the order.", samplesNotReceived);
        }

        return "<p>" + result + "</p>";
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
                    attributeArchetypeDao.findGenotypingProductOrderMapping(editOrder.getProductOrderId());
        }
        return genotypingProductOrderMapping;
    }

    private GenotypingProductOrderMapping findOrCreateGenotypingProductOrderMapping() {
        if (genotypingProductOrderMapping == null) {
            genotypingProductOrderMapping =
                    productOrderEjb.findOrCreateGenotypingChipProductOrderMapping(editOrder.getProductOrderId());
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
    private void populateAttributes(Long productOrderId) {
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
                attributeArchetypeDao.findGenotypingProductOrderMapping(productOrderId);
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

    /**
     * These are PDO specific overrides of defaults for chips
     */
    private Map<String, AttributeDefinition> getPdoAttributeDefinitions() {
        if (pdoSpecificDefinitions == null) {
            pdoSpecificDefinitions = attributeArchetypeDao.findAttributeNamesByTypeAndGroup(
                    AttributeDefinition.DefinitionType.GENOTYPING_PRODUCT_ORDER,
                    GenotypingProductOrderMapping.ATTRIBUTES_GROUP);
        }
        return pdoSpecificDefinitions;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }
    public Map<String, String> getChipDefaults() {
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

    @Inject
    public void setProductPriceCache(SAPProductPriceCache productPriceCache) {
        this.productPriceCache = productPriceCache;
    }

    @Inject
    public void setProductOrderEjb(ProductOrderEjb productOrderEjb) {
        this.productOrderEjb = productOrderEjb;
    }

    @Inject
    protected void setProductOrderDao(ProductOrderDao productOrderDao) {
        this.productOrderDao = productOrderDao;
    }

    @HandlesEvent(SAVE_SEARCH_DATA)
    public Resolution saveSearchData() throws Exception {
        preferenceSaver.saveTableData(tableState);
        return new StreamingResolution("text/json", preferenceSaver.getTableStateJson());
    }

    public boolean showColumn(String columnName) {
        return preferenceSaver.showColumn(columnName);
    }

    protected void setState(State state) {
        this.state = state;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public List<ProductOrder.OrderAccessType> getOrderTypeDisplayNames() {
        return Arrays.asList(ProductOrder.OrderAccessType.values());
    }

    public String getCustomizableProducts() {
        return customizableProducts;
    }

    public void setCustomizableProducts(String customizableProducts) {
        this.customizableProducts = customizableProducts;
    }

    public String getCustomizedProductPrices() {
        return customizedProductPrices;
    }

    public void setCustomizedProductPrices(String customizedProductPrices) {
        this.customizedProductPrices = customizedProductPrices;
    }

    public String getCustomizedProductQuantities() {
        return customizedProductQuantities;
    }

    public void setCustomizedProductQuantities(String customizedProductQuantities) {
        this.customizedProductQuantities = customizedProductQuantities;
    }

    public String getCustomizedProductNames() {
        return customizedProductNames;
    }

    public void setCustomizedProductNames(String customizedProductNames) {
        this.customizedProductNames = customizedProductNames;
    }

    public List<CustomizationValues> getProductCustomizations() {
        return productCustomizations;
    }

    public void setProductCustomizations(
            List<CustomizationValues> productCustomizations) {
        this.productCustomizations = productCustomizations;
    }

    public String getCustomizationJsonString() {
        return customizationJsonString;
    }

    public String getCustomPricePlaceholder() {
        return customPricePlaceholder;
    }

    public void setCustomPricePlaceholder(String customPricePlaceholder) {
        this.customPricePlaceholder = customPricePlaceholder;
    }

    public void setCustomizationJsonString(String customizationJsonString) {
        this.customizationJsonString = customizationJsonString;
    }

    public String getClinicalAttestationMessage() {
        return "I acknowledge that I have been properly trained in the handling of clinical projects, samples, and "
               + "data per Broad Genomics requirements, including HIPAA and data security policies, in order to order "
               + "clinical products";
    }

    public Boolean canEditPrice(String units) {
        return StringUtils.equalsIgnoreCase(units, "Sample") && (userBean.isPDMUser() || userBean.isDeveloperUser()) || !StringUtils.equalsIgnoreCase(units, "Sample") ;
    }

    public static boolean canChangeQuote(ProductOrder productOrder, String oldQuote, String newQuote) {
        boolean sameQuote = StringUtils.equals(oldQuote, newQuote);
        if (sameQuote){
            return true;
        }
        if (productOrder!=null) {
            if (!productOrder.getOrderStatus().canPlace()) {
                boolean sameQuoteType = StringUtils.isNumeric(oldQuote) == StringUtils.isNumeric(newQuote);
                boolean bothNotBlank = StringUtils.isNotBlank(oldQuote) && StringUtils.isNotBlank(newQuote);
                return sameQuoteType && bothNotBlank;
            }
        }
        return true;
    }

    /**
     * Get the list of available reagent designs.
     *
     * @return List of strings representing the reagent designs
     */
    public Collection<DisplayableItem> getReagentDesigns() {
        return makeDisplayableItemCollection(reagentDesignDao.findAll());
    }

    /**
     * Get the list of available coverages.
     *
     * @return UI helper object {@link DisplayableItem} representing the coverage
     */
    public Collection<DisplayableItem> getCoverageTypes() {
        return makeDisplayableItemCollection(coverageTypeDao.findAll());
    }

    @Inject
    public void setBspUserList(BSPUserList bspUserList) {
        this.bspUserList = bspUserList;
    }

    @Inject
    public void setJiraService(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    @Inject
    public void setProductTokenInput(ProductTokenInput productTokenInput) {
        this.productTokenInput = productTokenInput;
    }

    @Inject
    public void setProjectTokenInput(ProjectTokenInput projectTokenInput) {
        this.projectTokenInput = projectTokenInput;
    }

    @Inject
    public void setBspGroupCollectionTokenInput(BspGroupCollectionTokenInput bspGroupCollectionTokenInput) {
        this.bspGroupCollectionTokenInput = bspGroupCollectionTokenInput;
    }

    @Inject
    public void setBspShippingLocationTokenInput(BspShippingLocationTokenInput bspShippingLocationTokenInput) {
        this.bspShippingLocationTokenInput = bspShippingLocationTokenInput;
    }

    @Inject
    public void setResearchProjectDao(ResearchProjectDao researchProjectDao) {
        this.researchProjectDao = researchProjectDao;
    }

    public void setOwner(UserTokenInput owner) {
        this.owner = owner;
    }

    public List<ProductOrder.QuoteSourceType> getQuoteSources() {
        return Arrays.asList(ProductOrder.QuoteSourceType.values());
    }

    public ProductOrder.QuoteSourceType getQuoteSource() {
        return quoteSource;
    }

    public void setQuoteSource(ProductOrder.QuoteSourceType quoteSource) {
        this.quoteSource = quoteSource;
    }
}
