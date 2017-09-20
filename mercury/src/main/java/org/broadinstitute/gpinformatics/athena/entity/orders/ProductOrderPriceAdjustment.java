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
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.math.BigDecimal;

@Entity
@Audited
@Table(name = "PDO_PRICE_ADJUSTMENT", schema = "athena")
public class ProductOrderPriceAdjustment {

    @Id
    @Column(name = "pdo_price_adjustment_id")
    @SequenceGenerator(name = "SEQ_PDO_PRICE_ADJUSTMENT", schema = "athena", sequenceName = "SEQ_PDO_PRICE_ADJUSTMENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PDO_PRICE_ADJUSTMENT")
    private Long ProductOrderPriceAdjustmentId;

    @Index(name = "ix_pdo_price_adjustment")
    @ManyToOne(optional = false)
    private ProductOrder productOrder;

    // number(19,4)  ?
    @Column(name="adjustment_value")
    private BigDecimal adjustmentValue;

    @Column(name="custom_product_name")
    private String customProductName;

    @Column(name = "adjustment_quantity")
    private int adjustmentQuantity;

    public ProductOrderPriceAdjustment() {
    }

    public ProductOrderPriceAdjustment(BigDecimal adjustmentValue, int quantity, String customProductName) {
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

    public int getAdjustmentQuantity() {
        return adjustmentQuantity;
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
