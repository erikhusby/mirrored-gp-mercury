package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.BusinessObject;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
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

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT", schema = "athena", sequenceName = "SEQ_PRODUCT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT")
    private Long productId;

    @Column(name = "PRODUCT_NAME", length = 255)
    private String productName;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
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

    /**
     * Whether this Product should show as a top-level product in the Product list or for Product Order creation/edit.
     **/
    private boolean topLevelProduct;

    /**
     * Primary price item for the product.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST}, optional = false)
    private PriceItem primaryPriceItem;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "athena")
    private final Set<Product> addOns = new HashSet<>();

    // If we store this as Workflow in the database, we need to determine the best way to store 'no workflow'.
    private String workflowName;

    private boolean pdmOrderableOnly;

    // This is used for edit to keep track of changes to the object.
    // TODO MLC We should review whether this is still needed in Stripes, it was added to deal with JSF lifecycle issues.
    @Transient
    private String originalPartNumber;

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;

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

    /**
     * Default no-arg constructor, also used when creating a new Product.
     */
    public Product() {}

    public Product(boolean topLevelProduct) {
        this(null, null, null, null, null, null, null, null, null, null, null, null, topLevelProduct, Workflow.NONE, false, null);
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
                   @Nonnull Workflow workflow,
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
        workflowName = workflow.getWorkflowName();
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

    public void setWorkflow(@Nonnull Workflow workflow) {
        workflowName = workflow.getWorkflowName();
    }

    public Set<Product> getAddOns() {
        return addOns;
    }

    public void addAddOn(Product addOn) {
        addOns.add(addOn);
    }

    @Nonnull
    public Workflow getWorkflow() {
        return Workflow.findByName(workflowName);
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
        return getProductFamily().isSupportsNumberOfLanes();
    }

    public boolean isSampleInitiationProduct() {
        return partNumber.equals(SAMPLE_INITIATION_PART_NUMBER);
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

    public boolean isSameProductFamily(ProductFamily.ProductFamilyName productFamilyName) {
        return productFamilyName.getFamilyName().equals(this.productFamily.getName());
    }

    public boolean getSupportsSkippingQuote() {
        return getProductFamily().isSupportsSkippingQuote();
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
        return productFamily.getName().equals(ProductFamily.ProductFamilyName.EXOME.getFamilyName()) && productName.startsWith(EXOME_EXPRESS);
    }

}
