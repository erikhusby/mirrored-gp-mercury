package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.sap.entity.Condition;

import java.math.BigDecimal;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface PriceAdjustment {


    BigDecimal getAdjustmentValue();

    String getCustomProductName();

    Integer getAdjustmentQuantity();

    BigDecimal getListPrice();

    Condition deriveAdjustmentCondition();

    BigDecimal getAdjustmentDifference();
}
