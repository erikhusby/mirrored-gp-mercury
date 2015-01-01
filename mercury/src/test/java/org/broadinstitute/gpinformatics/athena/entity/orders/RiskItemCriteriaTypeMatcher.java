package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Hamcrest matcher for {@link RiskItem} comparing the items {@link RiskCriterion.RiskCriteriaType}. Does not match risk
 * items that have no {@link RiskCriterion}, specifically items indicating that there is no risk.
 */
public class RiskItemCriteriaTypeMatcher extends BaseMatcher<RiskItem> {

    private RiskCriterion.RiskCriteriaType criteriaType;

    public static RiskItemCriteriaTypeMatcher forCriteriaType(RiskCriterion.RiskCriteriaType criteriaType) {
        RiskItemCriteriaTypeMatcher matcher = new RiskItemCriteriaTypeMatcher();
        matcher.criteriaType = criteriaType;
        return matcher;
    }

    @Override
    public boolean matches(Object item) {
        RiskItem riskItem = (RiskItem) item;
        return riskItem.isOnRisk() && riskItem.getRiskCriterion().getType() == criteriaType;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("RiskItem for criteria: " + criteriaType);
    }
}
