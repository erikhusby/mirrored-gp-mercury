package org.broadinstitute.gpinformatics.athena.entity.orders;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Multimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.time.DateUtils;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.orders.CustomizationValues;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.LabEventSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.submission.SubmissionBioSampleBean;
import org.broadinstitute.gpinformatics.mercury.boundary.zims.BSPLookupException;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Formula;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderColumn;
import javax.persistence.PostLoad;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnusedDeclaration")
@Entity
@Audited
@Table(name = "PRODUCT_ORDER", schema = "athena")
public class ProductOrder implements BusinessObject, JiraProject, Serializable {
    private static final long serialVersionUID = 2712946561792445251L;

    // for clarity in jira, we use this string in the quote field when there is no quote
    public static final String QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY = "no quote";

    private static final String DRAFT_PREFIX = "Draft-";

    private static final String REQUISITION_PREFIX = "REQ-";

    public static final String IRB_REQUIRED_START_DATE_STRING = "04/01/2014";

    public enum SaveType {CREATING, UPDATING}

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "product_order", nullable = false)
    @OrderColumn(name = "SAMPLE_POSITION", nullable = false)
    @AuditJoinTable(name = "product_order_sample_join_aud")
    @BatchSize(size = 500)
    private final List<ProductOrderSample> samples = new ArrayList<>();

    @Transient
    private final SampleCounts sampleCounts = new SampleCounts();

    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, mappedBy = "productOrder", orphanRemoval = true)
    @BatchSize(size = 100)
    private final Set<ProductOrderAddOn> addOns = new HashSet<>();

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT_ORDER", schema = "athena", sequenceName = "SEQ_PRODUCT_ORDER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT_ORDER")
    private Long productOrderId;

    @Column(name = "CREATED_DATE")
    private Date createdDate;

    @Column(name = "CREATED_BY")
    private Long createdBy;

    @Column(name = "PLACED_DATE")
    private Date placedDate;

    @Column(name = "MODIFIED_DATE")
    private Date modifiedDate;

    @Column(name = "MODIFIED_BY", nullable = false)
    private long modifiedBy;

    @NotAudited
    @Formula("(select count(*) from athena.product_order_sample where product_order_sample.product_order = product_order_id)")
    private int sampleCount;

    /**
     * Unique title for the order.
     */
    @Column(name = "TITLE", unique = true, length = 255, nullable = false)
    private String title = "";

    @ManyToOne(cascade = CascadeType.PERSIST)
    private ResearchProject researchProject;

    @ManyToOne(cascade = CascadeType.PERSIST)
    private Product product;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus = OrderStatus.Draft;

    /**
     * Alphanumeric Id
     */
    @Column(name = "QUOTE_ID")
    private String quoteId = "";

    /**
     * Additional comments on the order.
     */
    @Column(length = 2000)
    private String comments;

    @Column(name = "FUNDING_DEADLINE")
    private Date fundingDeadline;

    @Column(name = "PUBLICATION_DEADLINE")
    private Date publicationDeadline;

    /**
     * Reference to the Jira Ticket created when the order is placed. Null means that the order is a draft.
     */
    @Column(name = "JIRA_TICKET_KEY", nullable = true)
    private String jiraTicketKey;

    /**
     * Counts the number of lanes, the default value is one lane.
     */
    @Column(name = "count")
    private int laneCount = 1;

    @Column(name = "REQUISITION_NAME", length = 256)
    private String requisitionName;

    // this should not cause n+1 select performance issue if it is LAZY and mandatory
    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = {CascadeType.ALL}, orphanRemoval = true)
    private ProductOrderKit productOrderKit;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "athena", name = "PDO_REGULATORY_INFOS", joinColumns = {@JoinColumn(name = "PRODUCT_ORDER")})
    private Collection<RegulatoryInfo> regulatoryInfos = new ArrayList<>();

    // This is used for edit to keep track of changes to the object.
    @Transient
    private String originalTitle;

    @Transient
    private Date oneYearAgo = DateUtils.addYears(new Date(), -1);

    @Column(name = "SKIP_QUOTE_REASON")
    private String skipQuoteReason;

    @Version
    private long version;

    @Column(name = "SKIP_REGULATORY_REASON")
    private String skipRegulatoryReason;

    @Column(name = "attestation_confirmed")
    private Boolean attestationConfirmed = false;

    @Column(name = "squid_work_request")
    private String squidWorkRequest;

    @OneToMany(mappedBy = "productOrder")
    private Set<BucketEntry> bucketEntries;

    @Column(name = "sap_order_number")
    private String sapOrderNumber;

    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "athena", name = "product_order_sap_orders", joinColumns = {@JoinColumn(name = "reference_product_order")})
    private List<SapOrderDetail> sapReferenceOrders = new ArrayList<>();

    @OneToMany(mappedBy = "parentOrder", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    private Set<ProductOrder> childOrders = new HashSet<>();

    @ManyToOne(cascade = {CascadeType.PERSIST}, fetch = FetchType.EAGER)
    @JoinColumn(name="PARENT_PRODUCT_ORDER")
    private ProductOrder parentOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "PIPELINE_LOCATION", nullable = true)
    private PipelineLocation pipelineLocation;

    @OneToMany(mappedBy = "productOrder", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    private Set<ProductOrderPriceAdjustment> customPriceAdjustments = new HashSet<>();

    @Transient
    private Set<ProductOrderPriceAdjustment> quotePriceMatchAdjustments = new HashSet<>();

    private Boolean priorToSAP1_5 = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "ORDER_TYPE")
    private OrderAccessType orderType = OrderAccessType.BROAD_PI_ENGAGED_WORK;

    @Column(name = "CLINICAL_ATTESTATION_CONFIRMED")
    private Boolean clinicalAttestationConfirmed = false;


    /**
     * Default no-arg constructor, also used when creating a new ProductOrder.
     */
    public ProductOrder() {
        // Do stuff that needs to happen after serialization and here.
        readResolve();
    }

    public ProductOrder(String title, String comments, String quoteId) {
        // Constructor for ProductOrderData.toProductOrder()
        this.title = title;
        this.comments = comments;
        this.quoteId = quoteId;
    }

    /**
     * Constructor called when creating a new ProductOrder.
     */
    public ProductOrder(@Nonnull BspUser createdBy, ResearchProject researchProject) {
        this(createdBy.getUserId(), "", new ArrayList<ProductOrderSample>(), "", null, researchProject);
    }

    /**
     * Helper method to take specific basic elements of a Product Order for the purposes of creating a new cloned order
     * @param toClone Original order to be cloned AND set as the parent of the cloned order
     * @param shareSapOrder Indicates whether the cloned order will refer to the original orders SAP order reference
     * @return A new Product Order which has certain elements copied from the original order
     */
    public static ProductOrder cloneProductOrder(ProductOrder toClone, boolean shareSapOrder) {

        final ProductOrder cloned = new ProductOrder(toClone.getCreatedBy(),
                "Clone " + toClone.getChildOrders().size() + ": " + toClone.getTitle(),
                new ArrayList<ProductOrderSample>(), toClone.getQuoteId(), toClone.getProduct(),
                toClone.getResearchProject());
        List<Product> potentialAddons = new ArrayList<>();

        for(ProductOrderAddOn cloneAddon : toClone.getAddOns()) {
            potentialAddons.add(cloneAddon.getAddOn());
        }
        if(CollectionUtils.isNotEmpty(potentialAddons)) {
            cloned.updateAddOnProducts(potentialAddons);
        }

        if (shareSapOrder & toClone.isSavedInSAP()) {
            cloned.addSapOrderDetail(new SapOrderDetail(toClone.latestSapOrderDetail().getSapOrderNumber(),
                    toClone.latestSapOrderDetail().getPrimaryQuantity(), toClone.latestSapOrderDetail().getQuoteId(),
                    toClone.latestSapOrderDetail().getCompanyCode(), toClone.latestSapOrderDetail().getOrderProductsHash(),toClone.latestSapOrderDetail().getOrderPricesHash()));
        }

        toClone.addChildOrder(cloned);

        return cloned;
    }

    public static List<Product> getAllProductsOrdered(ProductOrder order ) {
        List<Product> orderedListOfProducts = new ArrayList<>();
        orderedListOfProducts.add(order.getProduct());
        for (ProductOrderAddOn addOn : order.getAddOns()) {
            orderedListOfProducts.add(addOn.getAddOn());
        }
        Collections.sort(orderedListOfProducts);
        return orderedListOfProducts;
    }

    /**
     * helper method to see if at least one ledger entries across the collection of product order samples is marked
     * as having completed billing.
     * @return
     */
    public boolean hasAtLeastOneBilledLedgerEntry() {
        boolean oneBilled = false;
        for (ProductOrderSample sample : this.getSamples()) {
            for (LedgerEntry ledgerEntry : sample.getLedgerItems()) {
                if (ledgerEntry.getBillingSession() != null && ledgerEntry.getBillingSession().isComplete()) {
                    oneBilled = true;
                    break;
                }
            }
        }
        return oneBilled;
    }

    /**
     * Used for test purposes only.
     */
    public ProductOrder(@Nonnull Long creatorId, @Nonnull String title,
                        @Nonnull List<ProductOrderSample> samples, String quoteId,
                        Product product, ResearchProject researchProject) {

        // Set the dates and modified values so that tests don't have to call prepare and will never create bogus
        // data. Before adding this, tests were being created with null values, which caused problems, so taking out
        // the requirement on prepare.
        Date now = new Date();
        createdBy = creatorId;
        createdDate = now;
        modifiedBy = creatorId;
        modifiedDate = now;

        this.title = title;
        setSamples(samples);
        this.quoteId = quoteId;
        this.product = product;
        setResearchProject(researchProject);

        // Do stuff that needs to happen after serialization and here.
        readResolve();
    }

    /**
     * Utility method to create a PDO business key given an order ID and a JIRA ticket.
     *
     * @param productOrderId the order ID
     * @param jiraTicketKey  the JIRA ticket, can be null
     *
     * @return the business key for the PDO
     */
    public static String createBusinessKey(Long productOrderId, @Nullable String jiraTicketKey) {
        if (jiraTicketKey == null) {
            return DRAFT_PREFIX + productOrderId;
        }

        return jiraTicketKey;
    }

    /**
     * Use this method to convert from a business key to either a JIRA ID or a product order ID.
     *
     * @param businessKey the key to convert
     *
     * @return either a JIRA ID or a product order ID.
     */
    public static JiraOrId convertBusinessKeyToJiraOrId(@Nonnull String businessKey) {
        // This is currently happening in DEV at least, not sure why.
        //noinspection ConstantConditions
        if (businessKey == null) {
            return null;
        }

        if (businessKey.startsWith(DRAFT_PREFIX)) {
            return new JiraOrId(Long.parseLong(businessKey.substring(DRAFT_PREFIX.length())), null);
        }

        return new JiraOrId(0, businessKey);
    }

    private static Object formatCountTotal(int count, int compareCount) {
        if (count == 0) {
            return "None";
        }

        if (count == compareCount) {
            return "All";
        }

        return count;
    }

    /**
     * Initializes the {@link LabEventSampleDTO} for each {@link ProductOrderSample} with the {@link LabVessel}
     * associated with the ProductOrderSample so far.
     *
     * @param samples A list of ProductOrderSample objects to get the LabEvents for.
     */
    public static void loadLabEventSampleData(List<ProductOrderSample> samples) {

        LabEventSampleDataFetcher labDataFetcher = ServiceAccessUtility.getBean(LabEventSampleDataFetcher.class);

        Multimap<String, LabVessel> vesselMap = labDataFetcher.findMapBySampleKeys(samples);
        for (ProductOrderSample sample : samples) {
            Collection<LabVessel> labVessels = vesselMap.get(sample.getSampleKey());
            if (CollectionUtils.isNotEmpty(labVessels)) {
                sample.setLabEventSampleDTO(new LabEventSampleDTO(labVessels, sample.getSampleKey()));
            }
        }
    }

    /**
     * Load SampleData for all the supplied ProductOrderSamples.
     *
     * @see SampleDataFetcher
     */
    public static void loadSampleData(List<ProductOrderSample> samples) {
        loadSampleData(samples, BSPSampleSearchColumn.PDO_SEARCH_COLUMNS);
    }

    /**
     * Load CollaboratorSampleName for all the supplied ProductOrderSamples.
     *
     * @see SampleDataFetcher
     */
    public static void loadCollaboratorSampleName(List<ProductOrderSample> samples) {
        loadSampleData(samples, BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID);
    }

    /**
     * Load SampleData for all the supplied ProductOrderSamples.
     *
     * @see SampleDataFetcher
     */
    public static void loadSampleData(List<ProductOrderSample> samples,
                                      BSPSampleSearchColumn... bspSampleSearchColumns) {

        // Create a subset of the samples so we only call BSP for BSP samples that aren't already cached.
        Set<String> sampleNames = new HashSet<>(samples.size());
        for (ProductOrderSample productOrderSample : samples) {
            if (!productOrderSample.isHasBspSampleDataBeenInitialized()) {
                sampleNames.add(productOrderSample.getName());
            }
        }
        if (sampleNames.isEmpty()) {
            // This early return is needed to avoid making a unnecessary injection, which could cause
            // DB Free automated tests to fail.
            return;
        }

        // This gets all the sample names. We could get unique sample names from BSP as a future optimization.
        SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);
        Map<String, SampleData> sampleDataMap = Collections.emptyMap();

        try {
            sampleDataMap = sampleDataFetcher.fetchSampleDataForSamples(samples, bspSampleSearchColumns);
        } catch (BSPLookupException ignored) {
            // not a bsp sample?
        }

        // Collect SampleData which we will then use to look up FFPE status.
        List<SampleData> nonNullSampleData = new ArrayList<>();
        for (ProductOrderSample sample : samples) {
            SampleData sampleData = sampleDataMap.get(sample.getName());

            // If the DTO is null, we do not need to set it because it defaults to DUMMY inside sample.
            if (sampleData != null) {
                sample.setSampleData(sampleData);
                nonNullSampleData.add(sampleData);
            } else {
                sample.setSampleData(sample.makeSampleData());
            }
        }

        // Fill out all the SampleData with FFPE status in one shot.
        sampleDataFetcher.fetchFFPEDerived(nonNullSampleData);
    }

    // Initialize our transient data after the object has been loaded from the database.
    @PostLoad
    private void initialize() {
        originalTitle = title;
        readResolve();
    }

    public void readResolve() {
        oneYearAgo = DateUtils.addYears(new Date(), -1);
    }

    /**
     * @return The business key is the jira ticket key when this is not a draft, otherwise it is the DRAFT_KEY plus the
     * internal database id.
     */
    @Override
    @Nonnull
    public String getBusinessKey() {
        return createBusinessKey(productOrderId, jiraTicketKey);
    }

    public String getAddOnList() {
        return getAddOnList(", ");
    }

    public String getAddOnList(String delimiter) {
        if (getAddOns().isEmpty()) {
            return "no Add-ons";
        }

        String[] addOnArray = new String[getAddOns().size()];
        int i = 0;
        for (ProductOrderAddOn poAddOn : getAddOns()) {
            addOnArray[i++] = poAddOn.getAddOn().getProductName();
        }

        return StringUtils.join(addOnArray, delimiter);
    }

    public int getLaneCount() {
        return (isChildOrder())?parentOrder.getLaneCount():laneCount;
    }

    public void setLaneCount(int laneCount) {
        this.laneCount = laneCount;
    }

    public boolean requiresLaneCount() {
        boolean laneCountNeeded = getProduct()!=null && getProduct().getProductFamily()!=null
                                  && getProduct().getProductFamily().isSupportsNumberOfLanes();

        if(!laneCountNeeded) {
            for (ProductOrderAddOn addOn : getAddOns()) {
                laneCountNeeded = addOn.getAddOn().getProductFamily().isSupportsNumberOfLanes();
                if(laneCountNeeded) {
                    break;
                }
            }
        }

        return laneCountNeeded;
    }

    public void updateData(ResearchProject researchProject, Product product, List<Product> addOnProducts,
                           List<ProductOrderSample> samples) {
        updateAddOnProducts(addOnProducts);
        if(this.product != null && this.product != product) {
            this.clearCustomPriceAdjustment();
            if(product.getSapMaterial() != null) {
                if(product.isExternalOnlyProduct() || product.isClinicalProduct()) {
                    final ProductOrderPriceAdjustment priceAdjustment = new ProductOrderPriceAdjustment();
                    priceAdjustment.setAdjustmentValue(new BigDecimal(product.getSapMaterial().getBasePrice()));
                    this.addCustomPriceAdjustment(priceAdjustment);
                }
            }
        }
        this.product = product;
        setResearchProject(researchProject);
        setSamples(samples);
    }

    /**
     * This calculates risk for all samples in the order.
     *
     * @return The number of samples calculated to be on risk.
     */
    public int calculateRisk(List<ProductOrderSample> selectedSamples) {
        // Load the bsp data for the selected samples
        loadSampleData(selectedSamples);

        Set<String> uniqueSampleNamesOnRisk = new HashSet<>();
        for (ProductOrderSample sample : selectedSamples) {
            if (sample.calculateRisk()) {
                uniqueSampleNamesOnRisk.add(sample.getName());
            }
        }

        return uniqueSampleNamesOnRisk.size();
    }

    /**
     * This calculates risk for all samples in the order.
     *
     * @return The number of samples calculated to be on risk.
     */
    public int calculateRisk() {
        return calculateRisk(samples);
    }

    /**
     * Count the number of unique samples on risk on the product order. This is calculated on the summary page, but
     * for this call, we only want to do the calculation without going to BSP.
     *
     * @return The count of items on risk.
     */
    public int countItemsOnRisk() {
        int count = 0;
        Set<String> uniqueKeys = new HashSet<>();

        for (ProductOrderSample sample : samples) {
            if (sample.isOnRisk() && (!uniqueKeys.contains(sample.getName()))) {
                uniqueKeys.add(sample.getName());
                count++;
            }
        }

        return count;
    }

    /**
     * Call this method before saving changes to the database.  It updates the modified date and modified user.
     *
     * @param user the user doing the save operation.
     */
    public void prepareToSave(BspUser user) {
        prepareToSave(user, SaveType.UPDATING);
    }

    /**
     * Call this method before saving changes to the database.  It updates the modified date and modified user,
     * and sets the create date and create user if these haven't been set yet.
     *
     * FIXME: 12/2/16 SGM This method call should be ripped out and have the Product Order class extend Updatable
     * Doing so will automatically update these fields on save without this Hokey "Save Type"
     *
     * @param user     the user doing the save operation.
     * @param saveType a {@link SaveType} enum to define if creating or just updating
     */
    public void prepareToSave(BspUser user, SaveType saveType) {
        Date now = new Date();
        long userId = user.getUserId();

        if (saveType == SaveType.CREATING) {
            // createdBy is now set in the UI.
            if (createdBy == null) {
                // Used by tests only.
                createdBy = userId;
            }
            createdDate = now;
        }

        modifiedBy = userId;
        modifiedDate = now;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String getName() {
        return getTitle();
    }

    public List<ProductOrderAddOn> getAddOns() {
        return isChildOrder()?parentOrder.getAddOns():ImmutableList.copyOf(addOns);
    }

    public void updateAddOnProducts(List<Product> addOnList) {
        addOns.clear();
        for (Product addOn : addOnList) {
            final ProductOrderAddOn pdoAddOn = new ProductOrderAddOn(addOn, this);
            if (addOn.getSapMaterial() != null && (addOn.isClinicalProduct() || addOn.isExternalOnlyProduct()) ) {
                final ProductOrderAddOnPriceAdjustment customPriceAdjustment = new ProductOrderAddOnPriceAdjustment();
                customPriceAdjustment.setAdjustmentValue(new BigDecimal(addOn.getSapMaterial().getBasePrice()));
                pdoAddOn.setCustomPriceAdjustment(customPriceAdjustment);
            }
            addOns.add(pdoAddOn);
        }
    }

    public void setProductOrderAddOns(Collection<ProductOrderAddOn> addOns) {
        this.addOns.clear();
        this.addOns.addAll(addOns);
    }

    public ResearchProject getResearchProject() {
        return isChildOrder()?parentOrder.getResearchProject():researchProject;
    }

    public void setResearchProject(ResearchProject researchProject) {
        if (this.researchProject != null) {
            this.researchProject.removeProductOrder(this);
        }
        this.researchProject = researchProject;
        if (researchProject != null) {
            researchProject.addProductOrder(this);
        }
    }

    public Product getProduct() {
        return isChildOrder()?parentOrder.getProduct():product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public String getQuoteId() {
        return isChildOrder()?parentOrder.getQuoteId():quoteId;
    }

    public void setQuoteId(String quoteId) {
        this.quoteId = quoteId;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(OrderStatus orderStatus) {
        this.orderStatus = orderStatus;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public Date getFundingDeadline() {
        return fundingDeadline;
    }

    public void setFundingDeadline(Date fundingDeadline) {
        this.fundingDeadline = fundingDeadline;
    }

    public Date getPublicationDeadline() {
        return publicationDeadline;
    }

    public void setPublicationDeadline(Date publicationDeadline) {
        this.publicationDeadline = publicationDeadline;
    }

    public List<ProductOrderSample> getSamples() {
        return samples;
    }

    /**
     * Replace the current list of samples with a new list of samples. The order of the provided samples collection is
     * preserved, even if it contains a mix of existing and new instances.
     *
     * @param samples the samples to set
     */
    public void setSamples(@Nonnull Collection<ProductOrderSample> samples) {
        if (samples.isEmpty()) {
            // FIXME: This seems incorrect in the case where current sample list is non-empty and incoming samples are empty.
            return;
        }

        // Only update samples if the new sample list is different from the old one.
        if (isSampleListDifferent(samples)) {
            removeAllSamples();
            addSamplesInternal(samples, 0);
        }
    }

    /**
     * Remove all samples from this PDO and detach them from all other objects so they will be deleted.
     */
    private void removeAllSamples() {
        for (ProductOrderSample sample : samples) {
            sample.remove();
        }
        samples.clear();
    }

    private void addSamplesInternal(Collection<ProductOrderSample> newSamples, int samplePos) {
        for (ProductOrderSample sample : newSamples) {
            sample.setProductOrder(this);
            sample.setSamplePosition(samplePos++);
            samples.add(sample);
        }
        sampleCounts.invalidate();
    }

    public void addSample(@Nonnull ProductOrderSample newSample) {
        addSamples(Collections.singleton(newSample));
    }

    /**
     * Add to the list of samples. The order of samples is preserved.
     *
     * @param newSamples the samples to add
     */
    public void addSamples(@Nonnull Collection<ProductOrderSample> newSamples) {
        if (samples.isEmpty()) {
            setSamples(newSamples);
        } else {
            int samplePos = samples.get(samples.size() - 1).getSamplePosition();
            addSamplesInternal(newSamples, samplePos);
        }
    }

    /**
     * Determine if the list of samples names is exactly the same as the original list of sample names.
     *
     * @param newSamples new sample list
     *
     * @return true, if the name lists are different
     */
    private boolean isSampleListDifferent(Collection<ProductOrderSample> newSamples) {
        List<String> originalSampleNames = ProductOrderSample.getSampleNames(samples);
        List<String> newSampleNames = ProductOrderSample.getSampleNames(newSamples);

        // If not equals, then this has changed.
        return !newSampleNames.equals(originalSampleNames);
    }

    /**
     * @return If any sample has ledger items, then return true. Otherwise return false
     */
    private boolean hasLedgerItems() {
        for (ProductOrderSample sample : samples) {
            if (!sample.getLedgerItems().isEmpty()) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String getJiraTicketKey() {
        return jiraTicketKey;
    }

    /**
     * Used for test purposes only.
     */
    public void setJiraTicketKey(String jiraTicketKey) {
        this.jiraTicketKey = jiraTicketKey;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    /**
     * Call this method to change the 'owner' attribute of a Product Order.  We are currently storing this
     * attribute in the created by column in the database.
     *
     * @param userId the owner ID
     */
    public void setCreatedBy(@Nullable Long userId) {
        createdBy = userId;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public long getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(long modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    public Date getPlacedDate() {
        return placedDate;
    }

    /**
     * This should only be called from tests or for database backpopulation.  The placed date is normally set internally
     * when an order is placed.
     *
     * @param placedDate the date to set
     */
    public void setPlacedDate(Date placedDate) {
        this.placedDate = placedDate;
    }

    /**
     * Get the requisition key which should be the same ID as the product order, just a different prefix.  If there is
     * no product order business key, then it returns null. We use the jiraTicketKey because the product order would
     * not exist in the draft state.
     */
    public String getRequisitionKey() {
        if (jiraTicketKey != null) {
            return REQUISITION_PREFIX + jiraTicketKey.substring(jiraTicketKey.indexOf('-') + 1);
        }

        return null;
    }

    public String getRequisitionName() {
        return requisitionName;
    }

    public void setRequisitionName(String requisitionName) {
        this.requisitionName = requisitionName;
    }

    /**
     * Returns the text used for the quote in
     * jira.  If there is no quote, the text
     * {@link #QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY} is used.
     */
    public String getQuoteStringForJiraTicket() {
        String quoteForJira = QUOTE_TEXT_USED_IN_JIRA_WHEN_QUOTE_FIELD_IS_EMPTY;
        if (!StringUtils.isEmpty(quoteId)) {
            quoteForJira = quoteId;
        }
        return quoteForJira;
    }

    /**
     * Use the BSP Manager to load the bsp data for every sample in this product order.
     */
    public void loadSampleData() {
        loadSampleData(samples);
    }

    public void loadSampleDataForBillingTracker() {
        loadSampleData(samples, BSPSampleSearchColumn.BILLING_TRACKER_COLUMNS);
    }

    // Return the sample counts object to allow call chaining.
    private SampleCounts updateSampleCounts() {
        sampleCounts.generateCounts();
        return sampleCounts;
    }

    /**
     * Provides the summation of all unique participants represented in the list of samples
     * registered to this product order.
     *
     * @return a count of every participant that is represented by at least one sample in the list of product order
     * samples
     */
    public int getUniqueParticipantCount() {
        return updateSampleCounts().uniqueParticipantCount;
    }

    /**
     * @return the number of unique samples, as determined by the sample name
     */
    public int getUniqueSampleCount() {
        return updateSampleCounts().uniqueSampleCount;
    }

    /**
     * @return the list of sample names for this order, including duplicates. in the same sequence that
     * they were entered.
     */
    public String getSampleString() {
        return StringUtils.join(ProductOrderSample.getSampleNames(samples), '\n');
    }

    /**
     * Exposes how many samples are registered to this product order.
     *
     * @return a count of all samples registered to this product order
     */
    public int getTotalSampleCount() {
        return samples.size();
    }

    /**
     * Exposes how many samples registered to this product order are represented by more than one
     * sample in the list.
     *
     * @return a count of all samples that have more than one entry in the registered sample list
     */
    public int getDuplicateCount() {
        updateSampleCounts();
        return sampleCounts.totalSampleCount - sampleCounts.uniqueSampleCount;
    }

    public int getTumorCount() {
        return updateSampleCounts().sampleTypeCounter.get(ProductOrderSample.TUMOR_IND);
    }

    public int getNormalCount() {
        return updateSampleCounts().sampleTypeCounter.get(ProductOrderSample.NORMAL_IND);
    }

    public int getFemaleCount() {
        return updateSampleCounts().genderCounter.get(ProductOrderSample.FEMALE_IND);
    }

    public int getMaleCount() {
        return updateSampleCounts().genderCounter.get(ProductOrderSample.MALE_IND);
    }

    /**
     * Exposes the summation of each unique stock type found in the list of samples registered to
     * this product order.
     *
     * @return a Map, indexed by the unique stock type found, which gives a count of how many samples in the list of
     * product order samples, are related to that stock type
     */
    public Map<String, Integer> getCountsByStockType() {
        return updateSampleCounts().stockTypeCounter.countMap;
    }

    /**
     * Helper method that determines how many BSP samples registered to this product order
     * are marked as Received.
     *
     * @return a count of all samples in this product order that are in a RECEIVED state
     */
    public int getReceivedSampleCount() {
        return updateSampleCounts().receivedSampleCount;
    }

    /**
     * @return the number of samples that are both not received and not abandoned
     */
    public int getNonReceivedNonAbandonedCount() {
        return updateSampleCounts().notReceivedAndNotAbandonedCount;
    }

    /**
     * This enum helps to identfy how to aggregate the count totals across a PDO and its child orders
     */
    public enum CountAggregation{ ALL, BILL_READY, SHARE_SAP_ORDER, SHARE_SAP_ORDER_AND_BILL_READY}

    /**
     * Helper function to get the total number of samples associated with this order.  What makes this different than
     * the other sample count methods are 2 things:
     * <ol><li>This will work for Database Free test cases.  The sample counts internal class ultimately relies on
     * calling ServiceAccessUtility during its initialization which is difficult to mock out </li>
     * <li>The scenarios that utilize this method are mainly geared toward understanding the count for SAP
     * communication.  At present time, that excludes the need to include abandoned Samples</li>
     * </ol>
     * @param sameSapOrderOnly tells what kind of aggregation the count should do
     * @return
     */
    public int getTotalNonAbandonedCount(CountAggregation sameSapOrderOnly) {
        int count = 0;
        count = getNonAbandonedCount();

        for (ProductOrder childOrder : getChildOrders()) {
            switch (sameSapOrderOnly) {
            case SHARE_SAP_ORDER:
                if (!StringUtils.equals(childOrder.getSapOrderNumber(),getSapOrderNumber())) {
                        continue;
                }
                break;
            case SHARE_SAP_ORDER_AND_BILL_READY:
                if (!StringUtils.equals(childOrder.getSapOrderNumber(),getSapOrderNumber())
                        || !childOrder.getOrderStatus().canBillNotAbandoned()) {
                    continue;
                }
                break;
            case BILL_READY:
                if(!childOrder.getOrderStatus().canBillNotAbandoned()) {
                    continue;
                }
                break;
            }
            count += childOrder.getNonAbandonedCount();
        }

        return count;
    }

    /**
     * Only used to support @see getTotalNonAbandonedCount, this helper method will retrieve the current count of all
     * samples in the current order which are not abandoned
     * @return count of samples in the order that matches the criteria in the code
     */
    protected int getNonAbandonedCount() {
        int count = 0;

        for (ProductOrderSample sample : getSamples()) {
            if (sample.getDeliveryStatus() != ProductOrderSample.DeliveryStatus.ABANDONED) {
                count++;
            }
        }
        return count;
    }

    /**
     * Helper method that determines how many BSP samples registered to the product order are
     * in an Active state.
     *
     * @return a count of all samples in this product order that are in an ACTIVE state
     */
    public int getActiveSampleCount() {
        return updateSampleCounts().activeSampleCount;
    }

    public int getMissingBspMetaDataCount() {
        return updateSampleCounts().missingBspMetaDataCount;
    }

    public Long getProductOrderId() {
        return productOrderId;
    }


    public Collection<RegulatoryInfo> getRegulatoryInfos() {
        return isChildOrder()?parentOrder.getRegulatoryInfos():regulatoryInfos;
    }

    /**
     * @return true if order is in Draft
     */
    public boolean isDraft() {
        return orderStatus.isDraft();
    }

    /**
     * @return true if order is abandoned
     */
    public boolean isAbandoned() {
        return OrderStatus.Abandoned == orderStatus;
    }

    /**
     * @return true if order is submitted
     */
    public boolean isSubmitted() {
        return OrderStatus.Submitted == orderStatus;
    }

    /**
     * @return true if order is pending
     */
    public boolean isPending() {
        return orderStatus == OrderStatus.Pending;
    }

    /**
     * This is a helper method encapsulating the validations run against the samples contained
     * within this product order.  The results of these validation checks are then added to the existing Jira Ticket.
     */
    public List<String> getSampleSummaryComments() {
        return updateSampleCounts().sampleSummary();
    }

    public List<String> getSampleValidationComments() {
        return updateSampleCounts().sampleValidation();
    }

    /**
     * Returns true if the product for this PDO
     * has a RIN risk criteria.
     */
    public boolean isRinScoreValidationRequired() {
        if (getProduct() != null) {
            for (RiskCriterion riskCriterion : getProduct().getRiskCriteria()) {
                if (RiskCriterion.RiskCriteriaType.RIN == riskCriterion.getType()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Validates the existence of samples in the product order.
     *
     * @return true if there are no samples currently assigned to this product order
     */
    private boolean isSheetEmpty() {
        return samples.isEmpty();
    }

    public String getSkipQuoteReason() {
        return isChildOrder()?parentOrder.getSkipQuoteReason():skipQuoteReason;
    }

    public void setSkipQuoteReason(String skipQuoteReason) {
        this.skipQuoteReason = skipQuoteReason;
    }

    public String getSkipRegulatoryReason() {
        return isChildOrder()?parentOrder.getSkipRegulatoryReason():skipRegulatoryReason;
    }

    public void setSkipRegulatoryReason(String skipRegulatoryReason) {
        this.skipRegulatoryReason = skipRegulatoryReason;
    }

    public Collection<RegulatoryInfo> findAvailableRegulatoryInfos() {
        return getResearchProject().getRegulatoryInfos();
    }

    public void setRegulatoryInfos(Collection<RegulatoryInfo> regulatoryInfos) {
        this.regulatoryInfos.clear();
        getRegulatoryInfos().addAll(regulatoryInfos);
    }

    public void addRegulatoryInfo(@Nonnull RegulatoryInfo... regulatoryInfo) {
        getRegulatoryInfos().addAll(Arrays.asList(regulatoryInfo));
    }

    public boolean orderPredatesRegulatoryRequirement() {
        Date testDate = ((getPlacedDate() == null) ? new Date() : getPlacedDate());
        return testDate.before(getIrbRequiredStartDate());
    }

    public boolean regulatoryRequirementsMet() {
        if (orderPredatesRegulatoryRequirement()) {
            return true;
        }
        if (!getRegulatoryInfos().isEmpty() || canSkipRegulatoryRequirements()) {
            return true;
        }
        return false;
    }

    public boolean isRegulatoryInfoEditAllowed() {
        return isDraft() || isPending();
    }

    public boolean canSkipRegulatoryRequirements() {
        return !StringUtils.isBlank(getSkipRegulatoryReason());
    }

    public String getSquidWorkRequest() {
        return squidWorkRequest;
    }

    public void setSquidWorkRequest(String squidWorkRequest) {
        this.squidWorkRequest = squidWorkRequest;
    }

    public Set<BucketEntry> getBucketEntries() {
        return bucketEntries;
    }

    /**
     * @see ResearchProject#getRegulatoryDesignationCodeForPipeline
     */
    public String getRegulatoryDesignationCodeForPipeline() {
        if (getResearchProject() == null) {
            throw new RuntimeException(
                    "No research project for PDO " + getTitle() + ".  Cannot determine regulatory designation.");
        }
        return getResearchProject().getRegulatoryDesignationCodeForPipeline();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof ProductOrder)) {
            return false;
        }

        ProductOrder castOther = (ProductOrder) other;
        return new EqualsBuilder().append(getBusinessKey(), castOther.getBusinessKey()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getBusinessKey()).toHashCode();
    }

    public boolean hasJiraTicketKey() {
        return !StringUtils.isBlank(jiraTicketKey);
    }

    public String getOriginalTitle() {
        return originalTitle;
    }

    public ProductOrderKit getProductOrderKit() {
        if (productOrderKit == null) {
            productOrderKit = new ProductOrderKit();
        }

        return productOrderKit;
    }

    public void setProductOrderKit(ProductOrderKit productOrderKit) {
        this.productOrderKit = productOrderKit;
    }

    /**
     * Update this order's status based on the state of its samples.  The status is only changed if the current
     * status is Submitted or Completed.
     * <p/>
     * A order is complete if all samples are either billed or abandoned.
     *
     * @return true if the status was changed.
     */
    public boolean updateOrderStatus() {
        if (orderStatus != OrderStatus.Submitted && orderStatus != OrderStatus.Completed) {
            // We only support automatic status transitions from Submitted or Complete states.
            return false;
        }
        OrderStatus newStatus = OrderStatus.Completed;
        for (ProductOrderSample sample : samples) {
            if (sample.isToBeBilled()) {
                // Found an incomplete item.
                newStatus = OrderStatus.Submitted;
                break;
            }
        }

        if (newStatus != orderStatus) {
            orderStatus = newStatus;
            return true;
        }
        return false;
    }

    public Date getOneYearAgo() {
        return oneYearAgo;
    }

    /**
     * Convenience method to determine whether or not the current PDO is for sample initiation.
     *
     * @return true if this is a sample initiation PDO; false otherwise
     */
    public boolean isSampleInitiation() {
        return getProduct() != null && getProduct().isSampleInitiationProduct();
    }

    public String getSapOrderNumber() {

        String sapOrderNumber = null;
        if(latestSapOrderDetail()!= null) {
            sapOrderNumber = latestSapOrderDetail().getSapOrderNumber();
        }
        return sapOrderNumber;
    }

    public SapOrderDetail latestSapOrderDetail() {

        SapOrderDetail sapOrderDetail = null;
        if (CollectionUtils.isNotEmpty(sapReferenceOrders)) {
            ArrayList<SapOrderDetail> orderDetailSortList = new ArrayList<>(sapReferenceOrders);
            Collections.sort(orderDetailSortList);

            sapOrderDetail = orderDetailSortList.get(orderDetailSortList.size() - 1);
        }
        return sapOrderDetail;
    }

    public void setSapOrderNumber(String sapOrderNumber) {
        throw new UnsupportedOperationException("No longer just setting the SAP Order Number");
    }

    /**
     * This is used to help create or update a PDO's Jira ticket.
     */
    public enum JiraField implements CustomField.SubmissionField {
        PRODUCT_FAMILY("Product Family"),
        PRODUCT("Product"),
        QUOTE_ID("Quote ID"),
        MERCURY_URL("Mercury URL"),
        SAMPLE_IDS("Sample IDs"),
        REPORTER("Reporter"),
        FUNDING_DEADLINE("Funding Deadline", true),
        PUBLICATION_DEADLINE("Publication Deadline", true),
        DESCRIPTION("Description"),
        STATUS("Status"),
        REQUISITION_ID("Requisition ID"),
        LANES_PER_SAMPLE("Lanes Per Sample"),
        REQUISITION_NAME("Requisition Name"),
        NUMBER_OF_SAMPLES("Number of Samples"),
        ADD_ONS("Add-ons"),
        SUMMARY("Summary"),
        PMS("PMs", true);

        private final String fieldName;
        private final boolean nullable;

        JiraField(String fieldName) {
            this(fieldName, false);
        }

        JiraField(String fieldName, boolean nullable) {
            this.fieldName = fieldName;
            this.nullable = nullable;
        }

        @Nonnull
        @Override
        public String getName() {
            return fieldName;
        }


        @Override
        public boolean isNullable() {
            return nullable;
        }
    }

    public enum OrderStatus implements StatusType {
        Draft("label label-info"),
        Pending("label label-success"),
        Submitted,
        Abandoned,
        Completed;

        public static final EnumSet<OrderStatus> canAbandonStatuses = EnumSet.of(Pending, Submitted);
        /**
         * CSS Class to use when displaying this status in HTML.
         */
        private final String cssClass;

        OrderStatus() {
            this("");
        }

        OrderStatus(String cssClass) {
            this.cssClass = cssClass;
        }

        /**
         * Get all status values using the name strings.
         *
         * @param statusStrings The desired list of statuses.
         *
         * @return The statuses that are listed.
         */
        public static Set<OrderStatus> getFromNames(@Nonnull Collection<String> statusStrings) {
            if (CollectionUtils.isEmpty(statusStrings)) {
                return Collections.emptySet();
            }

            Set<OrderStatus> statuses = EnumSet.noneOf(OrderStatus.class);
            for (String statusString : statusStrings) {
                statuses.add(OrderStatus.valueOf(statusString));
            }

            return statuses;
        }

        @Override
        public String getDisplayName() {
            return name();
        }

        public boolean isDraft() {
            return this == Draft;
        }

        public String getCssClass() {
            return cssClass;
        }

        /**
         * @return true if an order can be abandoned from this state.
         */
        public boolean canAbandon() {
            return canAbandonStatuses.contains(this);
        }

        /**
         * @return true if an order can be placed from this state.
         */
        public boolean canPlace() {
            return EnumSet.of(Draft, Pending).contains(this);
        }

        /**
         * @return true if an order is ready for the lab to begin work on it.
         */
        public boolean readyForLab() {
            return !EnumSet.of(Draft, Pending, Abandoned).contains(this);
        }

        /**
         * @return true if an order can be billed from this state.
         */
        public boolean canBill() {
            return EnumSet.of(Submitted, Abandoned, Completed).contains(this);
        }
        /**
         * @return true if an order can be billed from this state.
         */
        public boolean canBillNotAbandoned() {
            return EnumSet.of(Submitted, Completed).contains(this);
        }
    }

    public enum PipelineLocation implements StatusType {
        US_CLOUD("US Cloud"),
        ON_PREMISES("On Premises");

        private String displayName;

        PipelineLocation(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    public static class JiraOrId {
        @Nullable
        public final String jiraTicketKey;
        public final long productOrderId;

        public JiraOrId(long productOrderId, @Nullable String jiraTicketKey) {
            this.jiraTicketKey = jiraTicketKey;
            this.productOrderId = productOrderId;
        }
    }

    private static class Counter implements Serializable {
        private static final long serialVersionUID = 4354572758557412220L;
        private final Map<String, Integer> countMap = new HashMap<>();

        private void clear() {
            countMap.clear();
        }

        private void increment(@Nonnull String key) {
            if (!StringUtils.isEmpty(key)) {
                Integer count = countMap.get(key);
                if (count == null) {
                    count = 0;
                }
                countMap.put(key, count + 1);
            }
        }

        private int get(@Nonnull String key) {
            Integer count = countMap.get(key);
            return count == null ? 0 : count;
        }

        private void output(List<String> output, @Nonnull String label, int compareCount) {
            for (Map.Entry<String, Integer> entry : countMap.entrySet()) {
                output.add(MessageFormat.format(
                        "{0} ''{1}'': {2}", label, entry.getKey(), formatCountTotal(entry.getValue(), compareCount)));
            }
        }
    }

    /**
     * Class that encapsulates counting samples and storing the results.
     */
    private class SampleCounts implements Serializable {
        private static final long serialVersionUID = 3984705436979136125L;
        private final Counter stockTypeCounter = new Counter();
        private final Counter primaryDiseaseCounter = new Counter();
        private final Counter genderCounter = new Counter();
        private final Counter sampleTypeCounter = new Counter();
        private boolean countsValid;
        private int totalSampleCount;
        private int onRiskCount;
        private int lastPicoCount;
        private int receivedSampleCount;
        private int activeSampleCount;
        private int hasSampleKitUploadRackscanMismatch;
        private int missingBspMetaDataCount;
        private int uniqueSampleCount;
        private int uniqueParticipantCount;
        private int abandonedCount;

        /**
         * This keeps track of unique and total samples per metadataSource.
         */
        private Map<MercurySample.MetadataSource, SampleCountForSource> samplesCountsBySource =
                new EnumMap<>(MercurySample.MetadataSource.class);

        /**
         * This is the number of samples that are both not received and not abandoned. This is to compute the number
         * of samples that will become abandoned when a Pending PDO is placed.
         */
        private int notReceivedAndNotAbandonedCount;

        private void initializeSamplesCountsBySource() {
            for (MercurySample.MetadataSource metadataSource : MercurySample.MetadataSource.values()) {
                samplesCountsBySource.put(metadataSource, new SampleCountForSource());
            }
        }

        /**
         * Go through all the samples and tabulate statistics.
         */
        private void generateCounts() {
            if (countsValid) {
                return;
            }

            // This gets the BSP data for every sample in the order.
            loadSampleData();

            // Initialize all counts.
            clearAllData();

            Set<String> sampleSet = new HashSet<>(samples.size());
            Set<String> participantSet = new HashSet<>();

            for (ProductOrderSample sample : samples) {
                if (sampleSet.add(sample.getName())) {

                    // This is a unique sample name, so do any counts that are only needed for unique names. Since
                    // BSP looks up samples by name, it would always get the same data, so only counting unique values.
                    if (sample.isInBspFormat()) {
                        updateSampleCounts(participantSet, sample);
                    }
                }

                // We can calculate on risk for any sample on the product and it can be done differently for each
                // sample of the same name. Therefore, we want to calculate this for each item.
                if (sample.isOnRisk()) {
                    onRiskCount++;
                }
            }

            uniqueSampleCount = sampleSet.size();
            uniqueParticipantCount = participantSet.size();
            countsValid = true;
        }

        private void incrementSampleCountByMetadata(ProductOrderSample sample) {
            samplesCountsBySource.get(sample.getMetadataSource()).addSample(sample.getName());
        }

        /**
         * This method returns the total number of unique samples for all MetadataSources.
         */
        private int getTotalUniqueSamplesWithMetadata() {
            int totalUnique = 0;
            for (SampleCountForSource counts : samplesCountsBySource.values()) {
                totalUnique += counts.getUnique();
            }
            return totalUnique;
        }

        /**
         * initialize all the counters.
         */
        private void clearAllData() {
            totalSampleCount = samples.size();
            receivedSampleCount = 0;
            activeSampleCount = 0;
            hasSampleKitUploadRackscanMismatch = 0;
            missingBspMetaDataCount = 0;
            onRiskCount = 0;
            notReceivedAndNotAbandonedCount = 0;
            abandonedCount = 0;
            stockTypeCounter.clear();
            primaryDiseaseCounter.clear();
            genderCounter.clear();
            initializeSamplesCountsBySource();
        }

        /**
         * Update all the counts related to sample data information.
         *
         * @param participantSet The unique collection of participants by Id.
         * @param sample         the sample to update the counts with
         */
        private void updateSampleCounts(Set<String> participantSet, ProductOrderSample sample) {
            if (sample.bspMetaDataMissing()) {
                missingBspMetaDataCount++;
            }

            incrementSampleCountByMetadata(sample);
            SampleData sampleData = sample.getSampleData();

            //Exposing that we look at received to be either received or accessioned.
            if (sample.isSampleAvailable()) {
                receivedSampleCount++;
            } else if (!sample.getDeliveryStatus().isAbandoned()) {
                notReceivedAndNotAbandonedCount++;
            }

            if (sample.getDeliveryStatus().isAbandoned()) {
                abandonedCount++;
            }

            if (sampleData.isActiveStock()) {
                activeSampleCount++;
            }

            // If the pico has never been run then it is not warned in the last pico date highlighting.
            Date picoRunDate = sampleData.getPicoRunDate();
            if ((picoRunDate == null) || picoRunDate.before(oneYearAgo)) {
                lastPicoCount++;
            }

            stockTypeCounter.increment(sampleData.getStockType());

            String participantId = sampleData.getPatientId();
            if (StringUtils.isNotBlank(participantId)) {
                participantSet.add(participantId);
            }

            primaryDiseaseCounter.increment(sampleData.getPrimaryDisease());
            genderCounter.increment(sampleData.getGender());

            if (sampleData.getHasSampleKitUploadRackscanMismatch()) {
                hasSampleKitUploadRackscanMismatch++;
            }

            sampleTypeCounter.increment(sampleData.getSampleType());
        }

        /**
         * Format the number to say None if the value is zero.
         *
         * @param count The number to format
         */
        private void formatSummaryNumber(List<String> output, String message, int count) {
            output.add(MessageFormat.format(message, (count == 0) ? "None" : count));
        }

        /**
         * Format the number to say None if the value is zero if it matches the comparison number.
         *
         * @param metadataSource The metadataSource for this sample
         * @param count          The number to format
         */
        private void formatSummaryNumber(List<String> output, String message,
                                         MercurySample.MetadataSource metadataSource, int count) {
            output.add(MessageFormat.format(message, metadataSource.getDisplayName(), count));
        }

        /**
         * Format the number to say None if the value is zero, or All if it matches the comparison number.
         *
         * @param count          The number to format
         * @param compareCount   The number to compare to
         * @param metadataSource The metadataSource for this sample
         */

        private void formatSummaryNumber(List<String> output, String message,
                                         MercurySample.MetadataSource metadataSource, int count, int compareCount) {
            output.add(MessageFormat
                    .format(message, metadataSource.getDisplayName(), formatCountTotal(count, compareCount)));
        }

        /**
         * Format the number to say None if the value is zero, or All if it matches the comparison number.
         *
         * @param count        The number to format
         * @param compareCount The number to compare to
         */

        private void formatSummaryNumber(List<String> output, String message, int count, int compareCount) {
            output.add(MessageFormat.format(message, formatCountTotal(count, compareCount)));
        }

        public List<String> sampleSummary() {
            generateCounts();

            List<String> output = new ArrayList<>();
            if (totalSampleCount == 0) {
                output.add("No Samples");
            } else {
                formatSummaryNumber(output, "Total: {0}", totalSampleCount);
                formatSummaryNumber(output, "Unique: {0}", uniqueSampleCount, totalSampleCount);
                formatSummaryNumber(output, "Duplicate: {0}", totalSampleCount - uniqueSampleCount, totalSampleCount);
                formatSummaryNumber(output, "On Risk: {0}", onRiskCount, totalSampleCount);
                int totalUniqueSamplesWithMetadata = getTotalUniqueSamplesWithMetadata();

                for (MercurySample.MetadataSource metadataSource : MercurySample.MetadataSource.values()) {
                    int currentCount = samplesCountsBySource.get(metadataSource).getTotal();
                    int currentUnique = samplesCountsBySource.get(metadataSource).getUnique();

                    formatSummaryNumber(output, "From {0}: {1}", metadataSource, currentCount, totalSampleCount);

                    if (currentCount != 0 && currentCount == currentUnique) {
                        formatSummaryNumber(output, "Unique {0}: {1}", metadataSource, currentCount);
                    }
                }
                if (uniqueSampleCount > totalUniqueSamplesWithMetadata) {
                    formatSummaryNumber(output, "Unique Not BSP/Mercury: {0}",
                            uniqueSampleCount - totalUniqueSamplesWithMetadata,
                            totalSampleCount);
                }

                if (uniqueParticipantCount != 0) {
                    formatSummaryNumber(output, "Unique Participants: {0}", uniqueParticipantCount, totalSampleCount);
                }

                stockTypeCounter.output(output, "Stock Type", totalSampleCount);
                primaryDiseaseCounter.output(output, "Disease", totalSampleCount);
                genderCounter.output(output, "Gender", totalSampleCount);
                sampleTypeCounter.output(output, "Sample Type", totalSampleCount);

                if (product != null && product.isSupportsPico()) {
                    formatSummaryNumber(output, "Last Pico over a year ago: {0}",
                            lastPicoCount);
                }

                if (hasSampleKitUploadRackscanMismatch != 0) {
                    formatSummaryNumber(output, "<div class=\"text-error\">Rackscan Mismatch: {0}</div>",
                            hasSampleKitUploadRackscanMismatch, totalSampleCount);
                }
            }

            return output;
        }

        private void checkCount(int count, String message, List<String> output) {
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
        public List<String> sampleValidation() {
            int totalUniqueSamplesWithMetadata = getTotalUniqueSamplesWithMetadata();
            List<String> output = new ArrayList<>();
            checkCount(missingBspMetaDataCount, "No BSP Data: {0}", output);
            checkCount(totalUniqueSamplesWithMetadata - activeSampleCount, "Not ACTIVE: {0}", output);
            checkCount(totalUniqueSamplesWithMetadata - receivedSampleCount, "Not RECEIVED: {0}", output);
            return output;
        }

        public void invalidate() {
            countsValid = false;
        }

        /**
         * This class keeps track of sample totals per Metadatasource
         */
        private class SampleCountForSource {
            private int total = 0;
            private final Set<String> uniqueSampleIds = new HashSet<>();

            public void addSample(String... sampleIds) {
                for (String sampleId : sampleIds) {
                    total++;
                    uniqueSampleIds.add(sampleId);
                }
            }

            public void clear() {
                total = 0;
                uniqueSampleIds.clear();
            }

            public int getTotal() {
                return total;
            }

            public int getUnique() {
                return uniqueSampleIds.size();
            }
        }
    }


    public Boolean isAttestationConfirmed() {
        return isChildOrder()?parentOrder.getAttestationConfirmed():getAttestationConfirmed();
    }

    public Boolean getAttestationConfirmed() {
        if (attestationConfirmed == null) {
            attestationConfirmed = false;
        }
        return attestationConfirmed;
    }

    public void setAttestationConfirmed(Boolean attestationConfirmed) {
        this.attestationConfirmed = attestationConfirmed;
    }

    public static Date getIrbRequiredStartDate() {
        Date irbRequiredStartDate;
        Date date;
        try {
            date = org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils.parseDate(
                    IRB_REQUIRED_START_DATE_STRING);
        } catch (ParseException e) {
            date = new Date();
        }
        irbRequiredStartDate =
                org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils.getStartOfDay(date);
        return irbRequiredStartDate;
    }

    /**
     * Go through every add on and report if any of them are sample initiation products.
     *
     * @return true if there is even one initiation product, false otherwise.
     */
    public boolean hasSampleInitiationAddOn() {
        for (ProductOrderAddOn addOn : getAddOns()) {
            if (addOn.getAddOn().isSampleInitiationProduct()) {
                return true;
            }
        }

        return false;
    }

    /**
     * If a reason is specified for why you can skip a quote it is OK to do.
     */
    @Transient
    public boolean canSkipQuote() {
        return StringUtils.isNotBlank(getSkipQuoteReason()) && allowedToSkipQuote();
    }

    public boolean allowedToSkipQuote() {
        return null != getProduct() &&
               getProduct().getSupportsSkippingQuote();
    }

    public int getSampleCount() {
        return sampleCount;
    }

    public Collection<String> getPrintFriendlyRegulatoryInfo() {
        Set<String> regInfo = new HashSet<>();

        for (RegulatoryInfo regulatoryInfo : getRegulatoryInfos()) {
            regInfo.add(regulatoryInfo.printFriendlyValue());
        }
        return regInfo;
    }

    /**
     * A product can have more then one Workflow if its AddOns have workflows. This method returns a List of
     * AddOn workflows and the workflow of its Product.
     * <p/>
     * If the AddOn's Workflow is Workflow.NONE, it will Not be included in the List.
     *
     * @return List of all Workflows associated with the ProductOrder
     */
    public List<Workflow> getProductWorkflows() {
        List<Workflow> workflows = new ArrayList<>();
        for (ProductOrderAddOn addOn : getAddOns()) {
            Workflow addOnWorkflow = addOn.getAddOn().getWorkflow();
            if (addOnWorkflow != Workflow.NONE) {
                workflows.add(addOnWorkflow);
            }
        }

        Workflow workflow = getProduct().getWorkflow();
        if (workflow != Workflow.NONE) {
            workflows.add(workflow);
        }
        return workflows;
    }

    public int getUnbilledSampleCount() {
        Iterable<ProductOrderSample> filteredResults = null;

        List<ProductOrderSample> samplesToFilter = new ArrayList<>();
        samplesToFilter.addAll(samples);

        for (ProductOrder childProductOrder : getChildOrders()) {
            if(childProductOrder.isSubmitted()) {
                samplesToFilter.addAll(childProductOrder.samples);
            }
        }

        filteredResults = Iterables.filter(samplesToFilter,
                new Predicate<ProductOrderSample>() {
                    @Override
                    public boolean apply(@Nullable ProductOrderSample productOrderSample) {
                        return productOrderSample.isToBeBilled();
                    }
                });

        return (filteredResults != null) ? Iterators.size(filteredResults.iterator()) : 0;
    }

    public static double getUnbilledNonSampleCount(ProductOrder order, Product targetProduct, int totalCount) {
        double existingCount = 0;

        for (ProductOrderSample targetSample : order.getSamples()) {
            for (LedgerEntry ledgerItem: targetSample.getLedgerItems()) {
                PriceItem priceItem = order.determinePriceItemByCompanyCode(targetProduct);

                if(ledgerItem.getPriceItem().equals(priceItem)) {
                    existingCount += ledgerItem.getQuantity();
                }
            }

        }
        return totalCount - existingCount;
    }

    public boolean isSavedInSAP() {
        return StringUtils.isNotBlank(getSapOrderNumber());
    }

    public ProductOrder getParentOrder() {
        return parentOrder;
    }

    public void setParentOrder(ProductOrder parentOrder) {

        this.parentOrder = parentOrder;
    }

    public Collection<ProductOrder> getChildOrders() {
        Collection<ProductOrder> result = Collections.emptyList();

        if (CollectionUtils.isNotEmpty(childOrders)) {
            result = childOrders;
        }
        return result;
    }

    public void addChildOrders(Collection<ProductOrder> childOrders) {

        for (ProductOrder childOrder : childOrders) {
            addChildOrder(childOrder);
        }
    }

    public void addChildOrder(ProductOrder childOrder) {
        childOrder.setParentOrder(this);
        childOrders.add(childOrder);
    }

    public int getNumberForReplacement() {
        return getSamples().size() - getTotalNonAbandonedCount(CountAggregation.ALL);
    }

    public boolean isChildOrder() {
        return this.parentOrder != null;
    }

    public List<SapOrderDetail> getSapReferenceOrders() {
        return sapReferenceOrders;
    }

    public void setSapReferenceOrders(
            List<SapOrderDetail> sapReferenceOrders) {
        for (SapOrderDetail orderDetail : sapReferenceOrders) {
            addSapOrderDetail(orderDetail);
        }
    }

    public void addSapOrderDetail(SapOrderDetail orderDetail) {

        this.sapReferenceOrders.add(orderDetail);
        orderDetail.addReferenceProductOrder(this);

    }

    public PipelineLocation getPipelineLocation() {
        return pipelineLocation;
    }

    public String submissionProcessingLocation(){
        String processingLocation = null;
        if (pipelineLocation == PipelineLocation.ON_PREMISES) {
            processingLocation = SubmissionBioSampleBean.ON_PREM;
        } else if (pipelineLocation == PipelineLocation.US_CLOUD) {
            processingLocation = SubmissionBioSampleBean.GCP;
        }
        return processingLocation;
    }

    public void setPipelineLocation(PipelineLocation pipelineLocation) {
        this.pipelineLocation = pipelineLocation;
    }

    public Boolean getPriorToSAP1_5() {
        return priorToSAP1_5;
    }

    public boolean isPriorToSAP1_5() { return getPriorToSAP1_5();}

    public void setPriorToSAP1_5(Boolean priorToSAP1_5) {
        this.priorToSAP1_5 = priorToSAP1_5;
    }

    public Set<ProductOrderPriceAdjustment> getQuotePriceMatchAdjustments() {
        return quotePriceMatchAdjustments;
    }

    public void setQuotePriceMatchAdjustments(Set<ProductOrderPriceAdjustment> quotePriceMatchAdjustments) {
        this.quotePriceMatchAdjustments = quotePriceMatchAdjustments;
    }

    public Set<ProductOrderPriceAdjustment> getCustomPriceAdjustments() {
        return customPriceAdjustments;
    }

    public ProductOrderPriceAdjustment getSinglePriceAdjustment() {
        ProductOrderPriceAdjustment found = null;
        if(!customPriceAdjustments.isEmpty()) {
            found = customPriceAdjustments.iterator().next();
        }

        return found;
    }

    public void setCustomPriceAdjustment(ProductOrderPriceAdjustment customPriceAdjustment) {

        clearCustomPriceAdjustment();
        addCustomPriceAdjustment(customPriceAdjustment);
    }

    public void clearCustomPriceAdjustment() {
        this.customPriceAdjustments.clear();
    }

    public void addCustomPriceAdjustment(ProductOrderPriceAdjustment priceAdjustment) {
        this.customPriceAdjustments.add(priceAdjustment);
        priceAdjustment.setProductOrder(this);
    }

    public OrderAccessType getOrderType() {
        return orderType;
    }

    public void setOrderType(OrderAccessType orderType) {
        this.orderType = orderType;
    }

    public String getOrderTypeDisplay() {

        String displayName = getOrderType().getDisplayName();

        if (getProduct().isClinicalProduct()) {
            displayName = "Clinical (2000)";
        }

        return displayName;
    }

    public Boolean isClinicalAttestationConfirmed() {
        return isChildOrder() ? parentOrder.getClinicalAttestationConfirmed() : getClinicalAttestationConfirmed();
    }

    public Boolean getClinicalAttestationConfirmed() {
        if (clinicalAttestationConfirmed == null) {
            clinicalAttestationConfirmed = false;
        }
        return clinicalAttestationConfirmed;
    }

    public void setClinicalAttestationConfirmed(Boolean clinicalAttestationConfirmed) {
        this.clinicalAttestationConfirmed = clinicalAttestationConfirmed;
    }

    public static void checkQuoteValidity(Quote quote) throws QuoteServerException {
        final Date todayTruncated = DateUtils.truncate(new Date(), Calendar.DATE);

        checkQuoteValidity(quote, todayTruncated);
    }

    public static void checkQuoteValidity(Quote quote, Date todayTruncated) throws QuoteServerException {
        for (FundingLevel fundingLevel : quote.getQuoteFunding().getFundingLevel(true)) {
            for (Funding funding : fundingLevel.getFunding()) {

                if (funding.getFundingType().equals(Funding.FUNDS_RESERVATION)) {
                    if (!FundingLevel.isGrantActiveForDate(todayTruncated, funding)) {
                        throw new QuoteServerException("The funding source " + funding.getGrantNumber() +
                                                       " has expired making this quote currently unfunded.");
                    }
                }
            }
        }
    }

    /**
     * Helps to determine if all orders associated with a product order (the main order and any "Child" orders created
     * when replacing abandoned orders) are completed.
     *
     * @return boolean flag indicating all orders in the chain are completed
     */
    public boolean allOrdersAreComplete() {

        boolean completeFlag = false;
        if (isChildOrder() && StringUtils.equals(getParentOrder().getSapOrderNumber(), getSapOrderNumber())) {
            completeFlag = getParentOrder().allOrdersAreComplete();
        } else {
            completeFlag = getOrderStatus() == OrderStatus.Completed;

            for (ProductOrder childProductOrder : getChildOrders()) {
                if(StringUtils.equals(childProductOrder.getSapOrderNumber(), getSapOrderNumber())) {
                    if (completeFlag) {
                        completeFlag = childProductOrder.getOrderStatus() == OrderStatus.Completed;
                    } else {
                        break;
                    }
                }
            }
        }
        return completeFlag;
    }

    /**
     * Helps to retrieve the highest lever order in a heirarchy which deals with the SAP order in question.  This makes
     * it easier to get the total non abandoned sample count of the order.
     *
     * @param productOrder Product order associated with an SAP order which needs to be updated
     * @return   either the product order passed in, or its' parent order depending on the relationship to the SAP
     * order
     */
    public static ProductOrder getTargetSAPProductOrder(ProductOrder productOrder) {
        ProductOrder returnOrder;
        if(productOrder.isChildOrder()) {
            final ProductOrder parentOrder = productOrder.getParentOrder();

            final String sapOrderNumber = parentOrder.getSapOrderNumber();

            boolean sameSAPOrderAsParent = sharesSAPOrderWithParent(productOrder, sapOrderNumber);
            returnOrder = sameSAPOrderAsParent ? parentOrder : productOrder;
        } else {
            returnOrder = productOrder;
        }
        return returnOrder;
    }

    public void updateCustomSettings(List<CustomizationValues> productCustomizations){

        Map<String, CustomizationValues> mappedValues = new HashMap<>();
        for (CustomizationValues productCustomization : productCustomizations) {
            mappedValues.put(productCustomization.getProductPartNumber(), productCustomization);
        }

        if(getProduct() != null){
            final Product product = getProduct();
            final String primaryPartNumber = product.getPartNumber();
            if(mappedValues.containsKey(primaryPartNumber)) {
                if(mappedValues.get(primaryPartNumber).isEmpty()) {
                    if(getSinglePriceAdjustment() != null) {
                        clearCustomPriceAdjustment();
                    }
                } else {
                    ProductOrderPriceAdjustment primaryAdjustment =
                            new ProductOrderPriceAdjustment(
                                    new BigDecimal(mappedValues.get(primaryPartNumber).getPrice()),
                                    (StringUtils.isNotBlank(mappedValues.get(primaryPartNumber).getQuantity())) ?
                                            Integer.valueOf(mappedValues.get(primaryPartNumber).getQuantity()) : null,
                                    mappedValues.get(primaryPartNumber).getCustomName());
                    setCustomPriceAdjustment(primaryAdjustment);
                }
            }
        }

        for (ProductOrderAddOn productOrderAddOn : getAddOns()) {
            final Product addOnProduct = productOrderAddOn.getAddOn();
            if(mappedValues.containsKey(addOnProduct.getPartNumber())) {
                if(mappedValues.get(addOnProduct.getPartNumber()).isEmpty()) {
                    if(productOrderAddOn.getSingleCustomPriceAdjustment() != null) {
                        productOrderAddOn.clearCustomPriceAdjustment();
                    }
                } else {
                    final ProductOrderAddOnPriceAdjustment productOrderAddOnPriceAdjustment =
                            new ProductOrderAddOnPriceAdjustment(
                                    new BigDecimal(mappedValues.get(addOnProduct.getPartNumber()).getPrice()),
                                    (StringUtils
                                            .isNotBlank(mappedValues.get(addOnProduct.getPartNumber()).getQuantity())) ?
                                            Integer.valueOf(
                                                    mappedValues.get(addOnProduct.getPartNumber()).getQuantity()) :
                                            null,
                                    mappedValues.get(addOnProduct.getPartNumber()).getCustomName());

                    productOrderAddOn.setCustomPriceAdjustment(productOrderAddOnPriceAdjustment);
                }
            }
        }
    }

    /**
     * Encapsulates the conditional logic to determine if a given product order not only has a parent order but also if
     * that parent order shares an sap order with the given product order
     * @param childOrder            target order to determine if its parent shares the same sap number
     * @param parentSAPOrderNumber  SAP number to compare between PDOs
     * @return
     */
    public static boolean sharesSAPOrderWithParent(ProductOrder childOrder, String parentSAPOrderNumber) {
        return childOrder.isChildOrder() && StringUtils
                .equals(childOrder.getSapOrderNumber(), parentSAPOrderNumber);
    }


    public void updateSapDetails(int sampleCount, String productListHash, String pricesForProducts) {
        final SapOrderDetail sapOrderDetail = latestSapOrderDetail();
        sapOrderDetail.setPrimaryQuantity(sampleCount);
        sapOrderDetail.setOrderProductsHash(productListHash);
        sapOrderDetail.setOrderPricesHash(pricesForProducts);
    }

    public boolean isResearchOrder () {
        boolean result = true;
        if(orderType != null) {
            result = orderType == OrderAccessType.BROAD_PI_ENGAGED_WORK;
        }
        return result;
    }

    public enum OrderAccessType implements StatusType {
        BROAD_PI_ENGAGED_WORK("Broad PI engaged Work (1000)"),
        COMMERCIAL("Commercial (2000)");

        private String displayName;

        OrderAccessType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        public static List<String> displayNames() {

            List<String> displayNames = new ArrayList<>();

            for (OrderAccessType orderAccessType : values()) {
                displayNames.add(orderAccessType.getDisplayName());
            }
            return displayNames;
        }

        public static OrderAccessType fromDisplayName(String displayName) {

            OrderAccessType foundType = null;

            for (OrderAccessType orderAccessType : values()) {
                if(orderAccessType.getDisplayName().equals(displayName)) {
                    foundType = orderAccessType;
                    break;
                }
            }
            return foundType;
        }
    }
    public boolean needsCustomization(Product product) {
        if(getProduct().equals(product)) {
            return getSinglePriceAdjustment() != null;
        } else {
            for (ProductOrderAddOn productOrderAddOn : getAddOns()) {
                if(productOrderAddOn.getAddOn().equals(product)) {
                    return productOrderAddOn.getSingleCustomPriceAdjustment() != null;
                }
            }

        }
        return false;
    }

    public void addQuoteAdjustment(Product product, BigDecimal effectivePrice, BigDecimal listPrice) {

        if(getProduct().equals(product)) {
            final ProductOrderPriceAdjustment quoteAdjustment = new ProductOrderPriceAdjustment();
            quoteAdjustment.setListPrice(listPrice);
            quoteAdjustment.setAdjustmentValue(effectivePrice);

            quotePriceMatchAdjustments.add(quoteAdjustment);
        } else {
            for (ProductOrderAddOn productOrderAddOn : getAddOns()) {
                if(productOrderAddOn.getAddOn().equals(product)) {
                    final ProductOrderAddOnPriceAdjustment quoteAdjustment = new ProductOrderAddOnPriceAdjustment();
                    quoteAdjustment.setListPrice(listPrice);
                    quoteAdjustment.setAdjustmentValue(effectivePrice);

                    productOrderAddOn.getQuotePriceAdjustments().add(quoteAdjustment);
                }
            }
        }
    }

    public PriceAdjustment getAdjustmentForProduct(Product product) {

        PriceAdjustment foundAdjustment = null;

        if(getProduct().equals(product)) {
            foundAdjustment = getSinglePriceAdjustment();
        } else {
            for (ProductOrderAddOn productOrderAddOn : getAddOns()) {
                if(productOrderAddOn.getAddOn().equals(product)) {
                    foundAdjustment = productOrderAddOn.getSingleCustomPriceAdjustment();
                    break;
                }
            }
        }

        return foundAdjustment;
    }

    public PriceItem determinePriceItemByCompanyCode(Product product) {
        PriceItem priceItem = product.getPrimaryPriceItem();
        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode = this.getSapCompanyConfigurationForProductOrder(
        );
        if(companyCode == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES &&
           product.getExternalPriceItem() != null) {
            priceItem = product.getExternalPriceItem();
        }
        return priceItem;
    }

    @NotNull
    public SapIntegrationClientImpl.SAPCompanyConfiguration getSapCompanyConfigurationForProductOrder() {
        SapIntegrationClientImpl.SAPCompanyConfiguration companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;
        if ((getOrderType() == OrderAccessType.COMMERCIAL)) {
            companyCode = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES;
        }
        return companyCode;
    }



}
