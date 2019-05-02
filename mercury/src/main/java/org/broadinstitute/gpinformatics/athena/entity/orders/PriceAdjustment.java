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

    public boolean hasPriceAdjustment() {
        return getAdjustmentValue() != null || StringUtils.isNotBlank(getCustomProductName()) || getAdjustmentQuantity() != null;
    }

    public Condition getAdjustmentCondition() {
        return Condition.PRICE_OVERRIDE;
    }
}
