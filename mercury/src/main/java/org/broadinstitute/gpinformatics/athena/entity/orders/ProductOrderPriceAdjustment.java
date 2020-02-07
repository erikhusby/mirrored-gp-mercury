package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.sap.entity.Condition;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "PDO_PRICE_ADJUSTMENT", schema = "athena")
public class ProductOrderPriceAdjustment extends PriceAdjustment{

    @Id
    @Column(name = "pdo_price_adjustment_id")
    @SequenceGenerator(name = "SEQ_PDO_PRICE_ADJUSTMENT", schema = "athena", sequenceName = "SEQ_PDO_PRICE_ADJUSTMENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PDO_PRICE_ADJUSTMENT")
    private Long ProductOrderPriceAdjustmentId;

    @Index(name = "ix_pdo_price_adjustment")
    @ManyToOne(optional = false)
    @JoinColumn(name = "PRODUCT_ORDER")
    private ProductOrder productOrder;

    public void setAdjustmentValue(BigDecimal adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }

    // number(19,4)  ?
    @Column(name="adjustment_value")
    private BigDecimal adjustmentValue;

    @Column(name="custom_product_name")
    private String customProductName;

    @Column(name = "adjustment_quantity")
    private BigDecimal adjustmentQuantity;

    @Transient
    private Condition priceAdjustmentCondition;

    public ProductOrderPriceAdjustment() {
    }

    public ProductOrderPriceAdjustment(BigDecimal adjustmentValue, BigDecimal quantity, String customProductName) {
        this.adjustmentValue = adjustmentValue;
        this.adjustmentQuantity = quantity;
        this.customProductName = customProductName;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(ProductOrder productOrder) {
        this.productOrder = productOrder;
    }

    public BigDecimal getAdjustmentValue() {
        return adjustmentValue;
    }

    public String getCustomProductName() {
        return customProductName;
    }

    public BigDecimal getAdjustmentQuantity() {
        return adjustmentQuantity;
    }

    public Condition getPriceAdjustmentCondition() {
        return priceAdjustmentCondition;
    }

    public void setPriceAdjustmentCondition(Condition priceAdjustmentCondition) {
        this.priceAdjustmentCondition = priceAdjustmentCondition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof ProductOrderPriceAdjustment)) {
            return false;
        }

        ProductOrderPriceAdjustment that = (ProductOrderPriceAdjustment) o;

        return new EqualsBuilder()
                .append(getProductOrder(), that.getProductOrder())
                .append(getAdjustmentValue(), that.getAdjustmentValue())
                .append(getCustomProductName(), that.getCustomProductName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getProductOrder())
                .append(getAdjustmentValue())
                .append(getCustomProductName())
                .toHashCode();
    }
}
