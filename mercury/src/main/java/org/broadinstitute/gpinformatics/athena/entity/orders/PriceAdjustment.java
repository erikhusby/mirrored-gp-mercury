package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.sap.entity.Condition;

import java.math.BigDecimal;

/**
 * TODO scottmat fill in javadoc!!!
 */
public abstract class PriceAdjustment {


    public abstract BigDecimal getAdjustmentValue();

    public abstract String getCustomProductName();

    public abstract Integer getAdjustmentQuantity();

    public abstract BigDecimal getListPrice();

    public boolean hasPriceAdjustment() {
        return getAdjustmentValue() != null || StringUtils.isNotBlank(getCustomProductName()) || getAdjustmentQuantity() != null;
    }

    public Condition deriveAdjustmentCondition() {
        if(getListPrice().compareTo(getAdjustmentValue()) <0) {
            return Condition.MARK_UP_LINE_ITEM;
        } else {
            return Condition.DOLLAR_DISCOUNT_LINE_ITEM;
        }
    }

    public BigDecimal getAdjustmentDifference() {
        if(getListPrice().compareTo(getAdjustmentValue()) <0) {
            return getAdjustmentValue().subtract(getListPrice());
        } else {
            return getListPrice().subtract(getAdjustmentValue());
        }
    }
}
