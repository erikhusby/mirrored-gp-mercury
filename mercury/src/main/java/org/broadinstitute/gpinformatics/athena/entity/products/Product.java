package org.broadinstitute.gpinformatics.athena.entity.products;


import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
public class Product implements Serializable {

    @Id
    @SequenceGenerator(name = "SEQ_PRODUCT", schema = "athena", sequenceName = "SEQ_PRODUCT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PRODUCT")
    private Long productId;

    private String productName;

    @ManyToOne(fetch = FetchType.LAZY)
    private ProductFamily productFamily;

    @Column(length = 2000)
    private String description;

    private String partNumber;
    private Date availabilityDate;
    private Date discontinuedDate;
    private Integer expectedCycleTimeSeconds;
    private Integer guaranteedCycleTimeSeconds;
    private Integer samplesPerWeek;

    @Column(length = 2000)
    private String inputRequirements;

    @Column(length = 2000)
    private String deliverables;

    /**
     * Whether this Product should show as a top-level product */
    private boolean topLevelProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    private PriceItem defaultPriceItem;

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "athena")
    private Set<PriceItem> priceItems = new HashSet<PriceItem>();

    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    @JoinTable(schema = "athena")
    private Set<Product> addOns = new HashSet<Product>();

    private String workflowName;

    // MLC This reaches into Orders and I don't want to step on what Hugh is currently working on in GPLIM-45
    // @OneToMany
    // private List<RiskContingency> riskContingencies;


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
                   String inputRequirements,
                   String deliverables,
                   boolean topLevelProduct,
                   String workflowName) {

        this.productName = productName;
        this.productFamily = productFamily;
        this.description = description;
        this.partNumber = partNumber;
        this.availabilityDate = availabilityDate;
        this.discontinuedDate = discontinuedDate;
        this.expectedCycleTimeSeconds = expectedCycleTimeSeconds;
        this.guaranteedCycleTimeSeconds = guaranteedCycleTimeSeconds;
        this.samplesPerWeek = samplesPerWeek;
        this.inputRequirements = inputRequirements;
        this.deliverables = deliverables;
        this.topLevelProduct = topLevelProduct;
        this.workflowName = workflowName;
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

    public void setAvailabilityDate(Date availabilityDate) {
        this.availabilityDate = availabilityDate;
    }

    public Date getDiscontinuedDate() {
        return discontinuedDate;
    }

    public void setDiscontinuedDate(Date discontinuedDate) {
        this.discontinuedDate = discontinuedDate;
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

    public PriceItem getDefaultPriceItem() {
        return defaultPriceItem;
    }

    public void setDefaultPriceItem(PriceItem defaultPriceItem) {
        this.defaultPriceItem = defaultPriceItem;
    }

    public Set<PriceItem> getPriceItems() {
        return priceItems;
    }


    public void addPriceItem(PriceItem priceItem) {

        priceItems.add(priceItem);

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


    public boolean isAvailable() {
        Date now = Calendar.getInstance().getTime();

        // need this logic in the dao too
        // available in the past and not yet discontinued
        return availabilityDate != null && (availabilityDate.compareTo(now) < 0) &&
                (discontinuedDate == null || discontinuedDate.compareTo(now) > 0);
    }

//    public List<RiskContingency> getRiskContingencies() {
//        return riskContingencies;
//    }
//
//    public void setRiskContingencies(List<RiskContingency> riskContingencies) {
//        this.riskContingencies = riskContingencies;
//    }


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
}
