package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
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
@Table(name = "PDO_ADDON_PRICE_ADJUSTMENT", schema = "athena")
public class ProductOrderAddOnPriceAdjustment {

    @Id
    @Column(name = "pdo_addon_price_adjustment_id")
    @SequenceGenerator(name = "SEQ_PDO_ADDON_PRICE_ADJUSTMENT", schema = "athena", sequenceName = "SEQ_PDO_ADDON_PRICE_ADJUSTMENT")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_PDO_ADDON_PRICE_ADJUSTMENT")
    private Long productOrderAddOnPriceAdjustmentId;

    @Index(name = "ix_pdo_add_on_price_adjustment")
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    private ProductOrderAddOn addOn;

    @Enumerated(EnumType.STRING)
    private Condition priceAdjustmentCondition;

    // number(19,4)  ?
    private BigDecimal adjustmentValue;

    public ProductOrderAddOnPriceAdjustment() {
    }

    public ProductOrderAddOnPriceAdjustment(ProductOrderAddOn addOn,
                                            Condition priceAdjustmentCondition, BigDecimal adjustmentValue) {
        this.addOn = addOn;
        this.priceAdjustmentCondition = priceAdjustmentCondition;
        this.adjustmentValue = adjustmentValue;
    }

    public ProductOrderAddOn getAddOn() {
        return addOn;
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

        if (!(o instanceof ProductOrderAddOnPriceAdjustment)) {
            return false;
        }

        ProductOrderAddOnPriceAdjustment that = (ProductOrderAddOnPriceAdjustment) o;

        return new EqualsBuilder()
                .append(getAddOn(), that.getAddOn())
                .append(getPriceAdjustmentCondition(), that.getPriceAdjustmentCondition())
                .append(getAdjustmentValue(), that.getAdjustmentValue())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getAddOn())
                .append(getPriceAdjustmentCondition())
                .append(getAdjustmentValue())
                .toHashCode();
    }
}
