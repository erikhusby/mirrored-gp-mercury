package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.work.MessageDataValue;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingRequirementTest {

    static final String CALL_RATE = "callRate";

    static Map<String, MessageDataValue> makeMap(String name, String value) {
        Map<String, MessageDataValue> data = new HashMap<>();
        data.put(name, new MessageDataValue(name, value));
        return data;
    }

    @DataProvider(name = "canBill")
    public static Object[][] makeCanBillData() {
        BillingRequirement requirement = new BillingRequirement();
        requirement.setAttribute(CALL_RATE);
        requirement.setOperator(Operator.GREATER_THAN);
        requirement.setValue(95.0);

        BillingRequirement requirement2 = new BillingRequirement();
        requirement2.setAttribute(CALL_RATE);
        requirement2.setOperator(Operator.LESS_THAN);
        requirement2.setValue(1.0);

        return new Object[][] {
                new Object[] { requirement, makeMap(CALL_RATE, "100"), true },
                new Object[] { requirement, makeMap(CALL_RATE, "95.1"), true },
                new Object[] { requirement, makeMap(CALL_RATE, "95"), false },
                new Object[] { requirement, makeMap(CALL_RATE, "0"), false },
                new Object[] { requirement, Collections.emptyMap(), false },
                new Object[] { requirement2, makeMap(CALL_RATE, "8.66E-4"), true }
        };
    }

    @Test(dataProvider = "canBill")
    public void testCanBill(BillingRequirement requirement, Map<String, MessageDataValue> data, boolean result) throws Exception {
        Assert.assertEquals(requirement.canBill(data), result, data.toString());
    }

    @DataProvider(name = "ops")
    public static Object[][] makeOpsData() {
        return new Object[][] {
                new Object[] { Operator.LESS_THAN, 1, 2, true },
                new Object[] { Operator.LESS_THAN_OR_EQUAL_TO, 2, 2, true },
                new Object[] { Operator.GREATER_THAN, 1, 2, false },
                new Object[] { Operator.GREATER_THAN_OR_EQUAL_TO, 2, 2, true }
        };
    }

    @Test(dataProvider = "ops")
    public void testOperatorApply(Operator operator,
                                  double d1, double d2, boolean result) throws Exception {
        Assert.assertEquals(operator.apply(d1, d2), result, d1 + " " + operator.getLabel() + " " + d2 + " is " + result);

    }
}
