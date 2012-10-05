package org.broadinstitute.gpinformatics.athena.entity.products;


import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

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
    private Long id;

    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    /**
     * Not cascading anything here, assuming if there ever is a means to edit ProductFamilies that those
     * ProductFamilies will be explicitly persisted and won't depend on a cascade from a referencing Product
     */
    private ProductFamily productFamily;

    private String description;
    private String partNumber;
    private Date availabilityDate;
    private Date discontinuedDate;
    private Integer expectedCycleTimeSeconds;
    private Integer guaranteedCycleTimeSeconds;
    private Integer samplesPerWeek;
    private String inputRequirements;
    private String deliverables;

    /**
     * Whether this Product should show as a top-level product */
    private boolean topLevelProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    private PriceItem defaultPriceItem;

    @OneToMany(mappedBy = "product")
    private List<PriceItem> priceItems;

    /**
     * May need to revisit cascade options for a Product editor
     */
    @ManyToMany(cascade = CascadeType.PERSIST)
    @JoinTable(schema = "athena")
    private List<Product> addOns;

    private String workflowName;

    // MLC This reaches into Orders and I don't want to step on what Hugh is currently working on in GPLIM-45
    // @OneToMany
    // private List<RiskContingency> riskContingencies;


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ProductFamily getProductFamily() {
        return productFamily;
    }

    public void setProductFamily(ProductFamily productFamily) {
        this.productFamily = productFamily;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public void setExpectedCycleTimeSeconds(Integer expectedCycleTimeSeconds) {
        this.expectedCycleTimeSeconds = expectedCycleTimeSeconds;
    }

    public Integer getGuaranteedCycleTimeSeconds() {
        return guaranteedCycleTimeSeconds;
    }

    public void setGuaranteedCycleTimeSeconds(Integer guaranteedCycleTimeSeconds) {
        this.guaranteedCycleTimeSeconds = guaranteedCycleTimeSeconds;
    }

    public Integer getSamplesPerWeek() {
        return samplesPerWeek;
    }

    public void setSamplesPerWeek(Integer samplesPerWeek) {
        this.samplesPerWeek = samplesPerWeek;
    }

    public String getInputRequirements() {
        return inputRequirements;
    }

    public void setInputRequirements(String inputRequirements) {
        this.inputRequirements = inputRequirements;
    }

    public String getDeliverables() {
        return deliverables;
    }

    public void setDeliverables(String deliverables) {
        this.deliverables = deliverables;
    }

    public boolean isTopLevelProduct() {
        return topLevelProduct;
    }

    public void setTopLevelProduct(boolean topLevelProduct) {
        this.topLevelProduct = topLevelProduct;
    }

    public PriceItem getDefaultPriceItem() {
        return defaultPriceItem;
    }

    public void setDefaultPriceItem(PriceItem defaultPriceItem) {
        this.defaultPriceItem = defaultPriceItem;
    }

    public List<PriceItem> getPriceItems() {
        return priceItems;
    }

    public void setPriceItems(List<PriceItem> priceItems) {
        this.priceItems = priceItems;
    }

    public List<Product> getAddOns() {
        return addOns;
    }

    public void setAddOns(List<Product> addOns) {
        this.addOns = addOns;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public void setWorkflowName(String workflowName) {
        this.workflowName = workflowName;
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
        if (!(o instanceof Product)) return false;

        Product product = (Product) o;

        if (!partNumber.equals(product.partNumber)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return partNumber.hashCode();
    }

}
