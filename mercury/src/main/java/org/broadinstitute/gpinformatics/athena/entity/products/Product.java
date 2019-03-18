package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.sap.entity.Condition;
import org.broadinstitute.sap.entity.material.SAPMaterial;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.hibernate.annotations.BatchSize;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
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
import javax.persistence.PostLoad;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This entity represents all the stored information for a Mercury Project.
 *
 */
@Entity
@Audited
@Table(schema = "athena",
        uniqueConstraints = @UniqueConstraint(columnNames = "PART_NUMBER"))
public class Product implements BusinessObject, Serializable, Comparable<Product> {

    private static final long serialVersionUID = 4859861191078406439L;

    private static final int ONE_DAY_IN_SECONDS = 60 * 60 * 24;

    public static final boolean TOP_LEVEL_PRODUCT = true;

    public static final String SUPPORTS_SKIPPING_QUOTE = "supportsSkippingQuote";

    /** The part number for the sample initiation product. */
    public static final String SAMPLE_INITIATION_PART_NUMBER = "P-ESH-0001";
    public static final String EXOME_EXPRESS_V2_PART_NUMBER = "P-EX-0007";
    public static final String EXOME_EXPRESS = "Exome Express";
    public static final String EXOME = "Exome";
    public static final String INFINIUM = "Infinium";

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT", schema = "athena", sequenceName = "SEQ_PRODUCT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT")
    private Long productId;

    @Column(name = "PRODUCT_NAME", length = 255)
    private String productName;

    @Column(name = "ALTERNATE_EXTERNAL_NAME", length = 255)
    private String alternateExternalName;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name="PRODUCT_FAMILY")
    private ProductFamily productFamily;

    @Column(name = "DESCRIPTION", length = 2000)
    private String description;

    @Column(name = "AGGREGATION_DATA_TYPE", length = 200)
    private String aggregationDataType;


    @Column(name = "ANALYSIS_TYPE_KEY", nullable = true, length = 200)
    private String analysisTypeKey;

    @Column(name = "REAGENT_DESIGN_KEY", nullable = true, length = 200)
    private String reagentDesignKey;


    @Column(name = "PART_NUMBER")
    private String partNumber;
    private Date availabilityDate;
    private Date discontinuedDate;
    private Integer expectedCycleTimeSeconds;
    private Integer guaranteedCycleTimeSeconds;
    private Integer samplesPerWeek;
    private Integer minimumOrderSize;

    @Column(length = 2000)
    private String inputRequirements;

    @Column(length = 2000)
    private String deliverables;

    private Integer readLength;
    private Integer insertSize;
    private BigDecimal loadingConcentration;
    private Boolean pairedEndRead;

    @Enumerated(EnumType.STRING)
    private FlowcellDesignation.IndexType indexType;

    @Enumerated(EnumType.STRING)
    private AggregationParticle defaultAggregationParticle;
    /**
     * A sample with MetadataSource.BSP can have its initial quant in Mercury, e.g. SONIC.  This flag avoids the
     * performance hit of looking for Mercury quants in Products that don't have them.
     */
    @Column(name = "EXPECT_INIT_QUANT_IN_MERCURY")
    private Boolean expectInitialQuantInMercury = false;

    /**
     * Whether this Product should show as a top-level product in the Product list or for Product Order creation/edit.
     **/
    private boolean topLevelProduct;

    /**
     * Primary price item for the product.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name="PRIMARY_PRICE_ITEM")
    private PriceItem primaryPriceItem;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST}, optional = false)
    @JoinColumn(name = "EXTERNAL_PRICE_ITEM")
    private PriceItem externalPriceItem;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "athena", name = "PRODUCT_ADD_ONS"
            , joinColumns = {@JoinColumn(name = "PRODUCT")}
            , inverseJoinColumns = {@JoinColumn(name = "ADD_ONS")})
    private final Set<Product> addOns = new HashSet<>();

    private String workflowName;

    private boolean pdmOrderableOnly;

    // This is used for edit to keep track of changes to the object.
    // TODO MLC We should review whether this is still needed in Stripes, it was added to deal with JSF lifecycle issues.
    @Transient
    private String originalPartNumber;

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;

    @Transient
    private SAPMaterial sapMaterial;

    // Initialize our transient data after the object has been loaded from the database.
    @PostLoad
    private void initialize() {
        originalPartNumber = partNumber;
    }

    // True if this product/add-on supports automated billing.
    @Column(name = "USE_AUTOMATED_BILLING", nullable = false)
    private boolean useAutomatedBilling;

    /** To determine if this product can be billed, the following criteria must be true. */
    // Note that for now, there will only be one requirement. We use OneToMany to allow for lazy loading of the
    // requirement.
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "product", nullable = false)
    @AuditJoinTable(name = "product_requirement_join_aud")
    private List<BillingRequirement> requirements;

    // The onRisk criteria that are associated with the Product. When creating new, default to empty list.
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "product", nullable = true)
    @AuditJoinTable(name = "product_risk_criteria_join_aud")
    private List<RiskCriterion> riskCriteria = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST})
    @JoinColumn(name = "POSITIVE_CONTROL_RP_ID")
    private ResearchProject positiveControlResearchProject;

    @Column(name ="EXTERNAL_ONLY_PRODUCT")
    private Boolean externalOnlyProduct = false;

    @Column(name = "SAVED_IN_SAP")
    private Boolean savedInSAP = false;

    @Column(name="CLINICAL_ONLY_PRODUCT")
    private Boolean clinicalProduct = false;

    @Column(name = "ANALYZE_UMI")
    private Boolean analyzeUmi = false;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "product", cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @BatchSize(size = 20)
    private List<ProductOrder> productOrders = new ArrayList<>();

    @Column(name = "BAIT_LOCKED")
    private Boolean baitLocked;

    /**
     * Helper method to allow the quick creation of a new Product based on the contents of an existing product
     *
     * @param productToClone Existing product from which the content of the cloned product will be based.
     * @param newProductName Title name to be given for the new Product
     * @param newPartNumber New part number to be applied to the new product
     * @return The newly created product to be saved
     */
    @NotNull
    public static Product cloneProduct(Product productToClone, String newProductName, String newPartNumber) {

        GregorianCalendar futureDate = new GregorianCalendar();
        futureDate.add(Calendar.MONTH, 6);
        Product clonedProduct = new Product(newProductName,
                productToClone.getProductFamily(), productToClone.getDescription(),
                newPartNumber,
                futureDate.getTime(),null,
                productToClone.getExpectedCycleTimeSeconds(), productToClone.getGuaranteedCycleTimeSeconds(),
                productToClone.getSamplesPerWeek(),productToClone.getMinimumOrderSize(),
                productToClone.getInputRequirements(), productToClone.getDeliverables(),
                productToClone.isTopLevelProduct(), productToClone.getWorkflowName(),
                productToClone.isPdmOrderableOnly(),productToClone.getAggregationDataType());

        clonedProduct.setExternalOnlyProduct(productToClone.isExternalOnlyProduct());

        clonedProduct.setAggregationDataType(productToClone.getAggregationDataType());
        clonedProduct.setAnalysisTypeKey(productToClone.getAnalysisTypeKey());
        clonedProduct.setReagentDesignKey(productToClone.getReagentDesignKey());
        clonedProduct.setBaitLocked(productToClone.getBaitLocked());
        clonedProduct.setPositiveControlResearchProject(productToClone.getPositiveControlResearchProject());
        clonedProduct.setReadLength(productToClone.getReadLength());
        clonedProduct.setInsertSize(productToClone.getInsertSize());
        clonedProduct.setLoadingConcentration(productToClone.getLoadingConcentration());
        clonedProduct.setPairedEndRead(productToClone.getPairedEndRead());
        clonedProduct.setIndexType(productToClone.getIndexType());
        clonedProduct.setClinicalProduct(productToClone.isClinicalProduct());

        for (RiskCriterion riskCriterion : productToClone.getRiskCriteria()) {
            clonedProduct.addRiskCriteria(new RiskCriterion(riskCriterion.getType(), riskCriterion.getOperator(), riskCriterion.getValue()));
        }

        for (Product product : productToClone.getAddOns()) {
            clonedProduct.addAddOn(product);
        }

        clonedProduct.setPrimaryPriceItem(productToClone.getPrimaryPriceItem());
        clonedProduct.setAlternateExternalName(productToClone.getAlternateExternalName());
        clonedProduct.setExternalPriceItem(productToClone.getExternalPriceItem());
        return clonedProduct;
    }

    /**
     * Default no-arg constructor, also used when creating a new Product.
     */
    public Product() {}

    public Product(boolean topLevelProduct) {
        this(null, null, null, null, null, null, null, null, null, null, null, null, topLevelProduct, null, false, null);
    }

    public Product(String productName,
                   ProductFamily productFamily,
                   String description,
                   String partNumber,
                   Date availabilityDate,
                   Date discontinuedDate,
                   Integer expectedCycleTimeSeconds,
                   Integer guaranteedCycleTimeSeconds,
                   Integer samplesPerWeek,
                   Integer minimumOrderSize,
                   String inputRequirements,
                   String deliverables,
                   boolean topLevelProduct,
                   String workflowName,
                   boolean pdmOrderableOnly,
                   String aggregationDataType) {
        this.productName = productName;
        this.productFamily = productFamily;
        this.description = description;
        this.partNumber = partNumber;
        this.availabilityDate = availabilityDate;
        this.discontinuedDate = discontinuedDate;
        this.expectedCycleTimeSeconds = expectedCycleTimeSeconds;
        this.guaranteedCycleTimeSeconds = guaranteedCycleTimeSeconds;
        this.samplesPerWeek = samplesPerWeek;
        this.minimumOrderSize = minimumOrderSize;
        this.inputRequirements = inputRequirements;
        this.deliverables = deliverables;
        this.topLevelProduct = topLevelProduct;
        this.workflowName = workflowName;
        this.pdmOrderableOnly = pdmOrderableOnly;
        this.aggregationDataType = aggregationDataType;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    @Override
    public String getName() {
        return getProductName();
    }

    public ProductFamily getProductFamily() {
        return productFamily;
    }

    public String getDescription() {
        return description;
    }

    public String getPartNumber() {
        return partNumber;
    }

    public void setPartNumber(String partNumber) {
        this.partNumber = partNumber;
    }

    public Date getAvailabilityDate() {
        return availabilityDate;
    }

    public Date getDiscontinuedDate() {
        return discontinuedDate;
    }

    public Integer getExpectedCycleTimeSeconds() {
        return expectedCycleTimeSeconds;
    }

    public Integer getGuaranteedCycleTimeSeconds() {
        return guaranteedCycleTimeSeconds;
    }

    public Integer getSamplesPerWeek() {
        return samplesPerWeek;
    }

    public String getInputRequirements() {
        return inputRequirements;
    }

    public String getDeliverables() {
        return deliverables;
    }

    @Nullable
    public Integer getReadLength() {
        return readLength;
    }

    public void setReadLength(Integer readLength) {
        this.readLength = readLength;
    }

    @Nullable
    public Integer getInsertSize() {
        return insertSize;
    }

    public void setInsertSize(Integer insertSize) {
        this.insertSize = insertSize;
    }

    @Nullable
    public BigDecimal getLoadingConcentration() {
        return loadingConcentration;
    }

    public void setLoadingConcentration(BigDecimal loadingConcentration) {
        this.loadingConcentration = loadingConcentration;
    }

    @Nullable
    public Boolean getPairedEndRead() {
        // Disallows null when sequencing params are present.
        if (StringUtils.isNotBlank(aggregationDataType)) {
            return Boolean.TRUE.equals(pairedEndRead);
        }
        return pairedEndRead;
    }

    public void setPairedEndRead(Boolean pairedEndRead) {
        // Disallows setting a non-null value to null.
        this.pairedEndRead = (this.pairedEndRead != null) ? Boolean.TRUE.equals(pairedEndRead) : pairedEndRead;
    }

    public FlowcellDesignation.IndexType getIndexType() {
        return indexType == null ? FlowcellDesignation.IndexType.DUAL : indexType;
    }

    public void setIndexType(FlowcellDesignation.IndexType indexType) {
        this.indexType = indexType;
    }

    public AggregationParticle getDefaultAggregationParticle() {
        return defaultAggregationParticle;
    }

    public void setDefaultAggregationParticle(AggregationParticle defaultAggregationParticle) {
        this.defaultAggregationParticle = defaultAggregationParticle;
    }

    public boolean isTopLevelProduct() {
        return topLevelProduct;
    }

    public PriceItem getPrimaryPriceItem() {
        return primaryPriceItem;
    }

    public void setPrimaryPriceItem(PriceItem primaryPriceItem) {
        this.primaryPriceItem = primaryPriceItem;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public void setProductFamily(ProductFamily productFamily) {
        this.productFamily = productFamily;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAvailabilityDate(Date availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public void setDiscontinuedDate(Date discontinuedDate) {
        this.discontinuedDate = discontinuedDate;
    }

    public void setExpectedCycleTimeSeconds(Integer expectedCycleTimeSeconds) {
        this.expectedCycleTimeSeconds = expectedCycleTimeSeconds;
    }

    public void setGuaranteedCycleTimeSeconds(Integer guaranteedCycleTimeSeconds) {
        this.guaranteedCycleTimeSeconds = guaranteedCycleTimeSeconds;
    }

    public void setSamplesPerWeek(Integer samplesPerWeek) {
        this.samplesPerWeek = samplesPerWeek;
    }

    public Integer getMinimumOrderSize() {
        return minimumOrderSize;
    }

    public void setMinimumOrderSize(Integer minimumOrderSize) {
        this.minimumOrderSize = minimumOrderSize;
    }

    public void setInputRequirements(String inputRequirements) {
        this.inputRequirements = inputRequirements;
    }

    public void setDeliverables(String deliverables) {
        this.deliverables = deliverables;
    }

    public void setTopLevelProduct(boolean topLevelProduct) {
        this.topLevelProduct = topLevelProduct;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
    }

    public Set<Product> getAddOns() {
        return addOns;
    }

    /**
     * Retrieve AddOns which can be used by specified user
     *
     * @param user the user seeking AddOns
     */
    public Set<Product> getAddOns(UserBean user) {
        Set<Product> filteredAddOns = new HashSet<>();
        Collection<Role> roles = user.getRoles();
        for (Product addOn : addOns) {
            if (!addOn.isPdmOrderableOnly()) {
                filteredAddOns.add(addOn);
            } else if (roles.contains(Role.PDM) || roles.contains(Role.Developer) || (roles.contains(Role.GPProjectManager) && this.isExternalOnlyProduct())) {
                filteredAddOns.add(addOn);
            }
        }
        return filteredAddOns;
    }

    public void addAddOn(Product addOn) {
        addOns.add(addOn);
    }

    @Nullable
    public String getWorkflowName() {
        return workflowName;
    }

    public String getAnalysisTypeKey() {
        return analysisTypeKey;
    }

    public void setAnalysisTypeKey(String analysisTypeKey) {
        this.analysisTypeKey = analysisTypeKey;
    }

    public String getReagentDesignKey() {
        return reagentDesignKey;
    }

    public void setReagentDesignKey(String reagentDesignKey) {
        this.reagentDesignKey = reagentDesignKey;
    }

    public boolean isPdmOrderableOnly() {
        return pdmOrderableOnly;
    }

    public void setPdmOrderableOnly(boolean pdmOrderableOnly) {
        this.pdmOrderableOnly = pdmOrderableOnly;
    }

    public boolean isUseAutomatedBilling() {
        return useAutomatedBilling;
    }

    public void setUseAutomatedBilling(boolean useAutomatedBilling) {
        this.useAutomatedBilling = useAutomatedBilling;
    }

    public String getAggregationDataType() {
        return aggregationDataType;
    }

    public void setAggregationDataType(String aggregationDataType) {
        this.aggregationDataType = aggregationDataType;
    }

    public BillingRequirement getRequirement() {

        if (CollectionUtils.isEmpty(requirements)) {
            return new BillingRequirement();
        }

        return requirements.get(0);
    }

    public boolean isAvailable() {
        Date now = Calendar.getInstance().getTime();

        // need this logic in the dao too
        // available in the past and not yet discontinued
        return availabilityDate != null && (availabilityDate.compareTo(now) < 0) &&
                (discontinuedDate == null || discontinuedDate.compareTo(now) > 0);
    }

    public boolean isDiscontinued() {
        Date now = Calendar.getInstance().getTime();

        return discontinuedDate != null && discontinuedDate.before(now);
    }

    public boolean isAvailableNowOrLater() {
        Date now = Calendar.getInstance().getTime();

        // need this logic in the dao too
        // available in the future
        return availabilityDate != null && (isAvailable() || availabilityDate.compareTo(now) > 0);
    }

    public boolean isPriceItemDefault(PriceItem priceItem) {
        return primaryPriceItem == priceItem || primaryPriceItem != null && primaryPriceItem.equals(priceItem);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Product product = (Product) o;

        if (partNumber != null ? !partNumber.equals(product.getPartNumber()) : product.getPartNumber() != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return partNumber != null ? partNumber.hashCode() : 0;
    }

    @Override
    public String getBusinessKey() {
        return partNumber;
    }

    @Override
    public int compareTo(Product that) {
        CompareToBuilder builder = new CompareToBuilder();
        builder.append(getPartNumber(), that.getPartNumber());
        return builder.build();
    }

    @Override
    public String toString() {
        return "Product{" +
                "productName='" + productName + '\'' +
                ", partNumber='" + partNumber + '\'' +
                '}';
    }

    /**
     * @return Get all duplicate price item names for any products or add ons on this product. Null if none.
     */
    public String[] getDuplicatePriceItemNames() {
        List<String> duplicates = new ArrayList<>();
        Set<String> priceItemNames = new HashSet<>();

        // Add the duplicates for this product.
        addProductDuplicates(duplicates, priceItemNames);

        // Add the duplicates for addOns.
        for (Product addOn : addOns) {
            addOn.addProductDuplicates(duplicates, priceItemNames);
        }

        if (duplicates.isEmpty()) {
            return null;
        }

        return duplicates.toArray(new String[duplicates.size()]);
    }

    private void addProductDuplicates(List<String> duplicates, Set<String> priceItemNames) {
        if (primaryPriceItem != null) {
            // No price items yet, so can just add it.
            priceItemNames.add(primaryPriceItem.getName());
        }
    }

    public Integer getExpectedCycleTimeDays() {
        return convertCycleTimeSecondsToDays(getExpectedCycleTimeSeconds()) ;
    }
    public void setExpectedCycleTimeDays(final Integer expectedCycleTimeDays) {
        setExpectedCycleTimeSeconds(convertCycleTimeDaysToSeconds(expectedCycleTimeDays));
    }

    public Integer getGuaranteedCycleTimeDays() {
        return convertCycleTimeSecondsToDays(getGuaranteedCycleTimeSeconds()) ;
    }
    public void setGuaranteedCycleTimeDays(final Integer guaranteedCycleTimeDays) {
        setGuaranteedCycleTimeSeconds(convertCycleTimeDaysToSeconds(guaranteedCycleTimeDays));
    }

    /**
     * Converts cycle times from days to seconds.
     *
     * @return the number of seconds.
     */
    public static int convertCycleTimeDaysToSeconds(Integer cycleTimeDays) {
        return (cycleTimeDays == null) ? 0 : cycleTimeDays * ONE_DAY_IN_SECONDS;
    }

    /**
     * Converts cycle times from seconds to days. This method rounds down to the nearest day.
     *
     * @param cycleTimeSeconds The cycle time in seconds/
     *
     * @return the number of days.
     */
    public static Integer convertCycleTimeSecondsToDays(Integer cycleTimeSeconds) {
        Integer cycleTimeDays = null;
        if ((cycleTimeSeconds != null) && cycleTimeSeconds >= ONE_DAY_IN_SECONDS) {
            cycleTimeDays =  (cycleTimeSeconds - (cycleTimeSeconds % ONE_DAY_IN_SECONDS)) / ONE_DAY_IN_SECONDS;
        }
        return cycleTimeDays;
    }

    public String getProductLabel() {

        if (getProductName() == null) {
            return "";
        }

        final int MAX_NAME = 45;

        if (getProductName().length() > MAX_NAME){
            return getProductName().substring(0, MAX_NAME) + "... ";
        } else if ( getProductName().length() + getPartNumber().length() < MAX_NAME ){
            return getProductName() + " : " + getPartNumber();
        }

        return getProductName();
    }

    public String getOriginalPartNumber() {
        return originalPartNumber;
    }


    public String getDisplayName() {
        return productName + " - " + partNumber;
    }

    public boolean getSupportsNumberOfLanes() {
        return getProductFamily().isSupportsNumberOfLanes() ;
    }

    public boolean isSampleInitiationProduct() {
        return partNumber.equals(SAMPLE_INITIATION_PART_NUMBER);
    }

    public ResearchProject getPositiveControlResearchProject() {
        return positiveControlResearchProject;
    }

    public void setPositiveControlResearchProject(ResearchProject positiveControlResearchProject) {
        this.positiveControlResearchProject = positiveControlResearchProject;
    }

    /**
     * Adds the criterion if it's not already in the list
     */
    public void addRiskCriteria(RiskCriterion riskCriterion) {
        if (!riskCriteria.contains(riskCriterion)) {
            riskCriteria.add(riskCriterion);
        }
    }

    public List<RiskCriterion> getRiskCriteria() {
        return riskCriteria;
    }

    public String[] getAddOnBusinessKeys() {
        String[] keys = new String[addOns.size()];
        int i = 0;

        for (Product addOn : addOns) {
            keys[i++] = addOn.getBusinessKey();
        }

        return keys;
    }

    public void updateRiskCriteria(@Nonnull String[] criteria, @Nonnull String[] operators, @Nonnull String[] values) {

        assert (criteria.length == operators.length) && (criteria.length == values.length);

        // The new list.
        List<RiskCriterion> newList = new ArrayList<>();
        // Assume that the new list is no different than the original.
        boolean isDifferent = false;

        // If the lengths are not the same, then these ARE different.
        if (criteria.length !=  riskCriteria.size()) {
            isDifferent = true;
        }

        // Go through specified criteria and find matching existing risk criteria.
        for (int i = 0; i < criteria.length; i++) {
            String value = values[i];

            boolean sameAsCurrent;
            RiskCriterion currentCriteria = null;

            if (riskCriteria.size() > i) {
                currentCriteria = riskCriteria.get(i);
                sameAsCurrent = currentCriteria.isSame(criteria[i], operators[i], value);
            } else {
                sameAsCurrent = false;
            }

            if (!sameAsCurrent) {
                // Not the same, so set isDifferent so we know to update the list later.
                isDifferent = true;
                currentCriteria = findMatching(criteria[i], operators[i], value);
                if (currentCriteria == null) {
                    currentCriteria = new RiskCriterion(RiskCriterion.RiskCriteriaType.findByLabel(criteria[i]), Operator.findByLabel(operators[i]), value);
                }
            }

            newList.add(currentCriteria);
        }

        if (isDifferent) {
            riskCriteria.clear();
            riskCriteria.addAll(newList);
        }
    }

    private RiskCriterion findMatching(String criteriaName, String operator, String value) {
        for (RiskCriterion criterion : riskCriteria) {
            if (criterion.isSame(criteriaName, operator, value)) {
                return criterion;
            }
        }

        return null;
    }

    public boolean isSupportsPico() {
        return getProductFamily().isSupportsPico();
    }

    public boolean isSupportsRin() {
        return getProductFamily().isSupportsRin();
    }

    public boolean isSameProductFamily(ProductFamily.ProductFamilyInfo productFamilyInfo) {
        return productFamilyInfo.getFamilyName().equals(this.productFamily.getName());
    }

    public boolean getSupportsSkippingQuote() {
        return getProductFamily().isSupportsSkippingQuote();
    }

    public boolean isExternalProduct() {
        return isExternallyNamed() || isExternalOnlyProduct();
    }

    public boolean hasExternalCounterpart() {
        return StringUtils.isNotBlank(alternateExternalName) || externalPriceItem != null;
    }

    public boolean isExternallyNamed() {
        return getPartNumber().startsWith("XT");
    }

    @Transient
    public static Comparator<Product> BY_PRODUCT_NAME = new Comparator<Product>() {
        @Override
        public int compare(Product product, Product anotherProduct) {
            return product.getName().compareTo(anotherProduct.getName());
        }
    };
    @Transient
    public static Comparator<Product> BY_FAMILY_THEN_PRODUCT_NAME = new Comparator<Product>() {
        @Override
        public int compare(Product product, Product anotherProduct) {
            int compare = product.getProductFamily().getName().compareTo(anotherProduct.getProductFamily().getName());
            if (compare!=0){
                return compare;
            }
            return BY_PRODUCT_NAME.compare(product, anotherProduct);
        }
    };

    /**
     * @return Whether this is an exome express product or not.
     */
    public boolean isExomeExpress() {
        return productFamily.getName().equals(ProductFamily.ProductFamilyInfo.EXOME.getFamilyName()) && productName.startsWith(EXOME_EXPRESS);
    }

    public Boolean getExpectInitialQuantInMercury() {
        return expectInitialQuantInMercury == null ? false : expectInitialQuantInMercury;
    }

    public void setExpectInitialQuantInMercury(Boolean expectInitialQuantInMercury) {
        this.expectInitialQuantInMercury = expectInitialQuantInMercury;
    }


    public boolean isExternalOnlyProduct() {
        return externalOnlyProduct;
    }

    public void setExternalOnlyProduct(boolean externalOnlyProduct) {
        this.externalOnlyProduct = externalOnlyProduct;
    }

    public boolean isSavedInSAP() {
        return savedInSAP;
    }

    public void setSavedInSAP(boolean savedInSAP) {
        this.savedInSAP = savedInSAP;
    }

    public boolean nocanPublishToSAP() {
        return !isSavedInSAP() && !isExternalOnlyProduct();
    }

    public String getAlternateExternalName() {
        return alternateExternalName;
    }

    public void setAlternateExternalName(String externalProductName) {
        this.alternateExternalName = externalProductName;
    }

    public PriceItem getExternalPriceItem() {
        return externalPriceItem;
    }

    public void setExternalPriceItem(PriceItem externalPriceItem) {
        this.externalPriceItem = externalPriceItem;
    }

    public void setClinicalProduct(boolean clinicalProduct) {
        this.clinicalProduct = clinicalProduct;
    }

    public boolean isClinicalProduct() {
        return clinicalProduct;
    }

    public Boolean getAnalyzeUmi() {
        return analyzeUmi == null ? false : analyzeUmi;
    }

    public void setAnalyzeUmi(Boolean analyzeUmi) {
        this.analyzeUmi = analyzeUmi;
    }

    public Boolean getBaitLocked() {
        if (baitLocked == null) {
            return true;
        }
        return baitLocked;
    }

    public void setBaitLocked(Boolean baitLocked) {
        this.baitLocked = baitLocked;
    }

    public SapIntegrationClientImpl.SAPCompanyConfiguration determineCompanyConfiguration () {

        SapIntegrationClientImpl.SAPCompanyConfiguration configuration = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD;

        if(isClinicalProduct() || isExternalOnlyProduct()) {
            configuration = SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES;
        }
        return configuration;
    }

    public void setSapMaterial(SAPMaterial sapMaterial) {
        this.sapMaterial = sapMaterial;
    }

    public SAPMaterial getSapMaterial() {
        return sapMaterial;
    }

    public String getSapClinicalCharge() {
        return (determineCompanyConfiguration() == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES)?getFeeByCondition(Condition.CLINICAL_CHARGE):"";
    }
    
    public String getSapCommercialCharge() {
        return (determineCompanyConfiguration() == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES)?getFeeByCondition(Condition.COMMERCIAL_CHARGE):"";
    }

    public String getSapSSFIntercompanyCharge() {
        return (determineCompanyConfiguration() == SapIntegrationClientImpl.SAPCompanyConfiguration.BROAD_EXTERNAL_SERVICES)?getFeeByCondition(Condition.INTERCOMPANY_FEE):"";
    }

    public String getSapFullPrice() {
        String price = "";
        if(this.sapMaterial != null && StringUtils.isNotBlank(this.sapMaterial.getBasePrice())) {
            final BigDecimal basePrice = new BigDecimal(this.sapMaterial.getBasePrice());
            if (basePrice.doubleValue() > 0d) {
                price = NumberFormat.getCurrencyInstance().format(basePrice);
            }
        }
        return price;
    }

    public String getQuoteServerPrice() {
        String price = "";
        if(getPrimaryPriceItem() != null && StringUtils.isNotBlank(getPrimaryPriceItem().getPrice())) {
            final BigDecimal quotePrice = new BigDecimal(getPrimaryPriceItem().getPrice());
            if (quotePrice.doubleValue() > 0d) {
                price = NumberFormat.getCurrencyInstance().format(quotePrice);
            }
        }
        return price;
    }

    private String getFeeByCondition(Condition condition) {
        String fee = "";
        if (this.sapMaterial != null) {
            final Map<Condition, BigDecimal> possibleOrderConditions = this.sapMaterial.getPossibleOrderConditions();
            if(possibleOrderConditions.containsKey(condition)) {

                final BigDecimal bigDecimalCharge = possibleOrderConditions.get(condition);
                if(bigDecimalCharge != null && bigDecimalCharge.doubleValue() > 0d) {
                    fee = NumberFormat.getCurrencyInstance().format(bigDecimalCharge);
                }
            }
        }
        return fee;
    }

    public enum AggregationParticle implements Displayable {
        PDO("PDO (eg: PDO-1243)"),
        PDO_ALIQUOT("PDO, Aliquot (eg: PDO-12.SM-34)");
        private static final Log log = LogFactory.getLog(AggregationParticle.class);

        private final String displayName;

        AggregationParticle(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        /**
         * Build an AggregationParticle based on the sampleId and productOrderKey.
         *
         * @param sampleId When the sample aliquot is used as as a part of the AGP, The sampleID passed in should
         *                 be the sampleID of the plating event in order to guarantee uniqueness.
         * @param productOrderKey the PDO key
         */
        public String build(String sampleId, String productOrderKey) {
            switch (this) {
            case PDO:
                return productOrderKey;
            case PDO_ALIQUOT:
                if (!StringUtils.isAnyBlank(sampleId, productOrderKey)) {
                    return String.format("%s.%s", productOrderKey, sampleId);
                } else {
                    log.error(String.format(
                        "null value passed into AggregationParticle.build [sampleId: %s, productOrderKey: %s]",
                        sampleId, productOrderKey));
                }
            }
            return null;
        }
    }
}
