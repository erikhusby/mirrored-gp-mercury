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

    private String customProductName;

    public ProductOrderPriceAdjustment() {
    }

    public ProductOrderPriceAdjustment(Condition priceAdjustmentCondition, BigDecimal adjustmentValue) {
        this.priceAdjustmentCondition = priceAdjustmentCondition;
        this.adjustmentValue = adjustmentValue;
    }

    public ProductOrder getProductOrder() {
        return productOrder;
    }

    public void setProductOrder(ProductOrder productOrder) {
        this.productOrder = productOrder;
    }

    public Condition getPriceAdjustmentCondition() {
        return priceAdjustmentCondition;
    }

    public BigDecimal getAdjustmentValue() {
        return adjustmentValue;
    }

    public String getCustomProductName() {
        return customProductName;
    }

    public void setCustomProductName(String customProductName) {
        this.customProductName = customProductName;
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
                .append(getCustomProductName(), that.getCustomProductName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getProductOrder())
                .append(getPriceAdjustmentCondition())
                .append(getAdjustmentValue())
                .append(getCustomProductName())
                .toHashCode();
    }
}
