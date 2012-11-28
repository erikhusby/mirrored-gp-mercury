package org.broadinstitute.gpinformatics.athena.entity.products;


import org.apache.commons.lang3.builder.CompareToBuilder;
import org.hibernate.envers.Audited;

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

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT", schema = "athena", sequenceName = "SEQ_PRODUCT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT")
    private Long productId;

    private String productName;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST}, optional = false)
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
    private Set<PriceItem> optionalPriceItems = new HashSet<PriceItem>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "athena")
    private Set<Product> addOns = new HashSet<Product>();

    private String workflowName;

    private boolean pdmOrderableOnly;

    /**
     * JPA package visible no arg constructor
     *
     * @return
     */
    Product() {}

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

    public void setPrimaryPriceItem(PriceItem defaultPriceItem) {
        this.primaryPriceItem = defaultPriceItem;
    }

    public Set<PriceItem> getOptionalPriceItems() {
        return optionalPriceItems;
    }


    public void setProductName(final String productName) {
        this.productName = productName;
    }

    public void setProductFamily(final ProductFamily productFamily) {
        this.productFamily = productFamily;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public void setAvailabilityDate(final Date availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public void setDiscontinuedDate(final Date discontinuedDate) {
        this.discontinuedDate = discontinuedDate;
    }

    public void setExpectedCycleTimeSeconds(final Integer expectedCycleTimeSeconds) {
        this.expectedCycleTimeSeconds = expectedCycleTimeSeconds;
    }

    public void setGuaranteedCycleTimeSeconds(final Integer guaranteedCycleTimeSeconds) {
        this.guaranteedCycleTimeSeconds = guaranteedCycleTimeSeconds;
    }

    public void setSamplesPerWeek(final Integer samplesPerWeek) {
        this.samplesPerWeek = samplesPerWeek;
    }

    public Integer getMinimumOrderSize() {
        return minimumOrderSize;
    }

    public void setMinimumOrderSize(final Integer minimumOrderSize) {
        this.minimumOrderSize = minimumOrderSize;
    }

    public void setInputRequirements(final String inputRequirements) {
        this.inputRequirements = inputRequirements;
    }

    public void setDeliverables(final String deliverables) {
        this.deliverables = deliverables;
    }

    public void setTopLevelProduct(final boolean topLevelProduct) {
        this.topLevelProduct = topLevelProduct;
    }

    public void setWorkflowName(final String workflowName) {
        this.workflowName = workflowName;
    }

    public void addPriceItem(PriceItem priceItem) {

        optionalPriceItems.add(priceItem);

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

        if (partNumber != null ? !partNumber.equals(product.partNumber) : product.partNumber != null) return false;

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
        builder.append(this.getPartNumber(), that.getPartNumber());
        return builder.build();
    }

    @Override
    public String toString() {
        return "Product{" +
                "productName='" + productName + '\'' +
                ", partNumber='" + partNumber + '\'' +
                '}';
    }
}
