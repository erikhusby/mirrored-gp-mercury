package org.broadinstitute.gpinformatics.athena.entity.products;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.*;
import java.io.Serializable;
import java.util.*;

/**
 * Core entity for Products.
 *
 * @author mcovarr
 *
 */
@Entity
@Audited
@Table(schema = "athena",
        uniqueConstraints = @UniqueConstraint(columnNames = {"partNumber"}))
public class Product implements Serializable, Comparable<Product> {
    private static final int ONE_DAY_IN_SECONDS = 60 * 60 * 24;

    public static final boolean TOP_LEVEL_PRODUCT = true;

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT", schema = "athena", sequenceName = "SEQ_PRODUCT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT")
    private Long productId;

    private String productName;

    @ManyToOne(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST}, optional = false)
    private ProductFamily productFamily;

    @Column(length = 2000)
    private String description;

    @Column(unique = true)
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
     * Whether this Product should show as a top-level product */
    private boolean topLevelProduct;

    /**
     * Primary price item for the product. Should NOT also be in the priceItems set.
     */
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST}, optional = false)
    private PriceItem primaryPriceItem;

    /**
     * OPTIONAL price items for the product. Should NOT include defaultPriceItem.
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "athena", name = "PRODUCT_OPT_PRICE_ITEMS")
    private final Set<PriceItem> optionalPriceItems = new HashSet<PriceItem>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "athena")
    private final Set<Product> addOns = new HashSet<Product>();

    private String workflowName;

    private boolean pdmOrderableOnly;

    @Transient
    private String originalPartNumber;   // This is used for edit to keep track of changes to the object.

    public static final String DEFAULT_WORKFLOW_NAME = "";
    public static final Boolean DEFAULT_TOP_LEVEL = Boolean.TRUE;

    // Initialize our transient data after the object has been loaded from the database.
    @PostLoad
    private void initialize() {
        originalPartNumber = partNumber;
    }

    /** True if this product/add-on supports automated billing. */
    @Column(name = "USE_AUTOMATED_BILLING", nullable = false)
    private boolean useAutomatedBilling;

    /** To determine if this product can be billed, the following criteria must be true. */
    // Note that for now, there will only be one requirement. We use OneToMany to allow for lazy loading of the
    // requirement.
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
    @JoinColumn(name = "product", nullable = false)
    @AuditJoinTable(name = "product_requirement_join_aud")
    private List<BillingRequirement> requirements;

    /**
     * Allowable Material Types for the product.
     */
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "athena", name = "PRODUCT_MATERIAL_TYPES",
            joinColumns=@JoinColumn(name="PRODUCT_ID"),
            inverseJoinColumns=@JoinColumn(name="MATERIAL_TYPE_ID")
    )
    private Set<MaterialType> allowableMaterialTypes = new HashSet<MaterialType>();

    /**
     * The onRisk criteria that are associated with the Product. When creating new, default to empty list
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinColumn(name = "product", nullable = true)
    @AuditJoinTable(name = "product_risk_criteria_join_aud")
    private List<RiskCriterion> riskCriteria = new ArrayList<RiskCriterion>();

    /**
     * Default no-arg constructor, also used when creating a new Product.
     */
    public Product() {}

    public Product(boolean topLevelProduct) {
        this(null, null, null, null, null, null, null, null, null, null, null, null, topLevelProduct, null, false);
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
                   boolean pdmOrderableOnly) {

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
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
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

    public Set<PriceItem> getOptionalPriceItems() {
        return optionalPriceItems;
    }

    public Set<MaterialType> getAllowableMaterialTypes() {
        return allowableMaterialTypes;
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

    public void addPriceItem(PriceItem priceItem) {
        optionalPriceItems.add(priceItem);
    }

    public void addAllowableMaterialType(MaterialType materialType) {
        allowableMaterialTypes.add(materialType);
    }

    public Set<Product> getAddOns() {
        return addOns;
    }

    public void addAddOn(Product addOn) {
        addOns.add(addOn);
    }

    public String getWorkflowName() {
        return workflowName;
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

    public BillingRequirement getRequirement() {
        if (requirements == null) {
            requirements = Collections.singletonList(new BillingRequirement());
        } else if (requirements.isEmpty()) {
            requirements.add(new BillingRequirement());
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
        if (primaryPriceItem == priceItem) return true;

        if (primaryPriceItem == null) return false;

        return primaryPriceItem.equals(priceItem);
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
        List<String> duplicates = new ArrayList<String> ();
        Set<String> priceItemNames = new HashSet<String> ();

        // Add the duplicates for this product
        addProductDuplicates(duplicates, priceItemNames);

        // Add the duplicates for addOns
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
            // No price items yet, so can just add it
            priceItemNames.add(primaryPriceItem.getName());
        }

        for (PriceItem optionalPriceItem : optionalPriceItems) {
            if (!priceItemNames.add(optionalPriceItem.getName())) {
                duplicates.add(optionalPriceItem.getName());
            }
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
     * @return the number of seconds.
     */
    public static int convertCycleTimeDaysToSeconds(Integer cycleTimeDays) {
        return (cycleTimeDays == null) ? 0 : cycleTimeDays * ONE_DAY_IN_SECONDS;
    }

    /**
     * Converts cycle times from seconds to days.
     * This method rounds down to the nearest day
     *
     * @param cycleTimeSeconds The cycle time in seconds
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


    public static Product makeEmptyProduct() {
        return new Product(null, null, null, null, null, null, null,
                null, null, null, null, null, DEFAULT_TOP_LEVEL, DEFAULT_WORKFLOW_NAME, false);
    }

    public String getDisplayName() {
        return productName + " - " + partNumber;
    }

    public boolean getSupportsNumberOfLanes() {
        return getProductFamily().isSupportsNumberOfLanes();
    }

    public List<RiskCriterion> getRiskCriteria() {
        return riskCriteria;
    }

    public String[] getAllowableMaterialTypeNames() {
        String[] names = new String[allowableMaterialTypes.size()];
        int i = 0;

        for (MaterialType materialType : allowableMaterialTypes) {
            names[i++] = materialType.getFullName();
        }

        return names;
    }

    public String[] getAddOnBusinessKeys() {
        String[] keys = new String[addOns.size()];
        int i = 0;

        for (Product addOn : addOns) {
            keys[i++] = addOn.getBusinessKey();
        }

        return keys;
    }

    public Long[] getPriceItemIds() {
        if (primaryPriceItem == null) {
            return new Long[0];
        }

        return new Long[] { primaryPriceItem.getPriceItemId() };
    }

    public void updateRiskCriteria(@Nonnull String[] criteria, @Nonnull String[] operators, @Nonnull String[] values) {

        assert (criteria.length == operators.length) && (criteria.length == values.length);

        // The new list
        List<RiskCriterion> newList = new ArrayList<RiskCriterion>();
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
}
