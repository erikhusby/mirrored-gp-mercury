package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
@Entity
@Audited
@Table(name= "PRODUCT_ORDER_ADD_ON", schema = "athena")
public class ProductOrderAddOn {
    @Id
    @SequenceGenerator(name = "SEQ_ORDER_ADD_ON", schema = "athena", sequenceName = "SEQ_ORDER_ADD_ON")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ORDER_ADD_ON")
    private Long productOrderAddOnId;

    /**
     * This is eager fetched because this class' whole purpose is to bridge a specific person and project. If you
     * ever only need the ID, you should write a specific projection query in the DAO
     */
    @Index(name = "ix_product_order_add_on")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name="PRODUCT_ORDER")
    private ProductOrder productOrder;

    @ManyToOne(cascade = CascadeType.PERSIST, optional = false)
    @JoinColumn(name="ADD_ON")
    private Product addOn;


    @OneToMany(mappedBy = "addOn", cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true, fetch = FetchType.LAZY)
    Set<ProductOrderAddOnPriceAdjustment> customPriceAdjustments = new HashSet<>();

    @Transient
    Set<ProductOrderAddOnPriceAdjustment> quotePriceAdjustments = new HashSet<>();

    protected ProductOrderAddOn() {
    }

    public ProductOrderAddOn(@Nonnull Product addOn, @Nonnull ProductOrder productOrder) {
        this.addOn = addOn;
        this.productOrder = productOrder;
    }

    public Product getAddOn() {
        return addOn;
    }

    public Long getProductOrderAddOnId() {
        return productOrderAddOnId;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public Set<ProductOrderAddOnPriceAdjustment> getQuotePriceAdjustments() {
        return quotePriceAdjustments;
    }

    public void setQuotePriceAdjustments(
            Set<ProductOrderAddOnPriceAdjustment> quotePriceAdjustments) {
        this.quotePriceAdjustments = quotePriceAdjustments;
    }

    public Set<ProductOrderAddOnPriceAdjustment> getCustomPriceAdjustments() {
        return customPriceAdjustments;
    }

    public ProductOrderAddOnPriceAdjustment getSingleCustomPriceAdjustment() {
        ProductOrderAddOnPriceAdjustment found = null;
        if (!customPriceAdjustments.isEmpty()) {
            found = customPriceAdjustments.iterator().next();
        }

        return found;
    }

    public void setCustomPriceAdjustment(ProductOrderAddOnPriceAdjustment customPriceAdjustment) {

        clearCustomPriceAdjustment();

        addCustomPriceAdjustment(customPriceAdjustment);
    }

    public void clearCustomPriceAdjustment() {
        this.customPriceAdjustments.clear();
    }

    private void addCustomPriceAdjustment(ProductOrderAddOnPriceAdjustment customPriceAdjustment) {
        this.customPriceAdjustments.add(customPriceAdjustment);
        customPriceAdjustment.setAddOn(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProductOrderAddOn)) {
            return false;
        }

        ProductOrderAddOn that = (ProductOrderAddOn) o;
        return new EqualsBuilder().append(productOrder, that.getProductOrder()).append(addOn, that.getAddOn()).build();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(productOrder).append(addOn).build();
    }
}
