package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.sap.entity.Condition;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.math.BigDecimal;

@Entity
@Audited
@Table(name = "PDO_ADDON_PRICE_ADJUSTMENT", schema = "athena")
public class ProductOrderAddOnPriceAdjustment extends PriceAdjustment{

    @Id
    @Column(name = "pdo_addon_price_adjustment_id")
    @SequenceGenerator(name = "SEQ_PDO_ADDON_PRICE_ADJUSTMENT", schema = "athena", sequenceName = "SEQ_PDO_ADDON_PRICE_ADJUSTMENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PDO_ADDON_PRICE_ADJUSTMENT")
    private Long productOrderAddOnPriceAdjustmentId;

    @Index(name = "ix_pdo_add_on_price_adjustment")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name="ADD_ON")
    private ProductOrderAddOn addOn;

        // number(19,4)  ?
    @Column(name="adjustment_value")
    private BigDecimal adjustmentValue;

    @Column(name="custom_product_name")
    private String customProductName;

    @Column(name = "adjustment_quantity")
    private Integer adjustmentQuantity;

    @Transient
    private Condition priceAdjustmentCondition;

    @Transient
    private BigDecimal listPrice;

    public ProductOrderAddOnPriceAdjustment() {
    }

    public ProductOrderAddOnPriceAdjustment(BigDecimal adjustmentValue, Integer quantity, String customProductName) {
        this.adjustmentValue = adjustmentValue;
        this.adjustmentQuantity = quantity;
        this.customProductName = customProductName;
    }

    public ProductOrderAddOn getAddOn() {
        return addOn;
    }

    public void setAddOn(ProductOrderAddOn addOn) {
        this.addOn = addOn;
    }

    public BigDecimal getAdjustmentValue() {
        return adjustmentValue;
    }

    public String getCustomProductName() {
        return customProductName;
    }

    public Integer getAdjustmentQuantity() {
        return adjustmentQuantity;
    }

    public Condition getPriceAdjustmentCondition() {
        return priceAdjustmentCondition;
    }

    public void setPriceAdjustmentCondition(Condition priceAdjustmentCondition) {
        this.priceAdjustmentCondition = priceAdjustmentCondition;
    }

    public BigDecimal getListPrice() {
        return listPrice;
    }

    public void setListPrice(BigDecimal listPrice) {
        this.listPrice = listPrice;
    }

    public void setAdjustmentValue(BigDecimal adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProductOrderAddOnPriceAdjustment)) {
            return false;
        }

        ProductOrderAddOnPriceAdjustment that = (ProductOrderAddOnPriceAdjustment) o;

        return new EqualsBuilder()
                .append(getAddOn(), that.getAddOn())
                .append(getAdjustmentValue(), that.getAdjustmentValue())
                .append(getCustomProductName(), that.getCustomProductName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getAddOn())
                .append(getAdjustmentValue())
                .append(getCustomProductName())
                .toHashCode();
    }

}
