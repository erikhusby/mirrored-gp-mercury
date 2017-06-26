package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.sap.entity.Condition;
import org.hibernate.annotations.Index;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
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
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private ProductOrder productOrder;

    @Enumerated(EnumType.STRING)
    private Condition priceAdjustmentCondition;

    // number(19,4)  ?
    private BigDecimal adjustmentValue;

    public ProductOrderPriceAdjustment() {
    }

    public ProductOrderPriceAdjustment(ProductOrder productOrder,
                                       Condition priceAdjustmentCondition, BigDecimal adjustmentValue) {
        this.productOrder = productOrder;
        this.priceAdjustmentCondition = priceAdjustmentCondition;
        this.adjustmentValue = adjustmentValue;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public Condition getPriceAdjustmentCondition() {
        return priceAdjustmentCondition;
    }

    public void setAdjustmentValue(BigDecimal adjustmentValue) {
        this.adjustmentValue = adjustmentValue;
    }

    public BigDecimal getAdjustmentValue() {
        return adjustmentValue;
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
                .append(getPriceAdjustmentCondition(), that.getPriceAdjustmentCondition())
                .append(getAdjustmentValue(), that.getAdjustmentValue())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getProductOrder())
                .append(getPriceAdjustmentCondition())
                .append(getAdjustmentValue())
                .toHashCode();
    }
}
