package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.work.MessageDataValue;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author pshapiro
 */
@Test(groups = "DatabaseFree")
public class BillingRequirementTest {

    static final String CALL_RATE = "callRate";

    static Map<String, MessageDataValue> makeMap(String name, String value) {
        Map<String, MessageDataValue> data = new HashMap<String, MessageDataValue>();
        data.put(name, new MessageDataValue(name, value));
        return data;
    }

    @DataProvider(name = "canBill")
    public static Object[][] makeCanBillData() {
        BillingRequirement requirement = new BillingRequirement();
        requirement.setAttribute(CALL_RATE);
        requirement.setOperator(BillingRequirement.Operator.GREATER_THAN);
        requirement.setValue(95.0);

        return new Object[][] {
                new Object[] { requirement, makeMap(CALL_RATE, "100"), true },
                new Object[] { requirement, makeMap(CALL_RATE, "95.1"), true },
                new Object[] { requirement, makeMap(CALL_RATE, "95"), false },
                new Object[] { requirement, makeMap(CALL_RATE, "0"), false },
                new Object[] { requirement, Collections.emptyMap(), false }
        };
    }

    @Test(dataProvider = "canBill")
    public void testCanBill(BillingRequirement requirement, Map<String, MessageDataValue> data, boolean result) throws Exception {
        Assert.assertEquals(requirement.canBill(data), result, data.toString());
    }

    @DataProvider(name = "ops")
    public static Object[][] makeOpsData() {
        return new Object[][] {
                new Object[] { BillingRequirement.Operator.LESS_THAN, 1, 2, true },
                new Object[] { BillingRequirement.Operator.LESS_THAN_OR_EQUAL_TO, 2, 2, true },
                new Object[] { BillingRequirement.Operator.GREATER_THAN, 1, 2, false },
                new Object[] { BillingRequirement.Operator.GREATER_THAN_OR_EQUAL_TO, 2, 2, true }
        };
    }

    @Test(dataProvider = "ops")
    public void testOperatorApply(BillingRequirement.Operator operator,
                                  double d1, double d2, boolean result) throws Exception {
        Assert.assertEquals(operator.apply(d1, d2), result, d1 + " " + operator.label + " " + d2 + " is " + result);

    }
}
