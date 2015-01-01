/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class OperatorTest {
    @DataProvider
    public Object[][] stringDataProvider() {
        return new Object[][]{
                {"foo", "foo", Operator.EXACT_MATCH, true},
                {"bar", "foo", Operator.EXACT_MATCH, false},
                {"foo", "foobar", Operator.IS_IN, true},
                {"foo", "bfaoro", Operator.IS_IN, false}
        };
    }

    @Test(enabled = true, dataProvider = "stringDataProvider")
    public void testApplyTypedStringEqual(String firstObject, String secondObject, Operator operator,
                                          boolean expectedToPass) throws Exception {
        Assert.assertEquals(operator.apply(firstObject, secondObject), expectedToPass);
    }

    @DataProvider
    public Object[][] doubleDataProvider() {
        return new Object[][]{
                {1, 1, Operator.EQUALS, true},
                {2, 1, Operator.EQUALS, false},
                {2, 1, Operator.GREATER_THAN, true},
                {1, 2, Operator.GREATER_THAN, false},
                {1, 1, Operator.GREATER_THAN_OR_EQUAL_TO, true},
                {1, 2, Operator.GREATER_THAN_OR_EQUAL_TO, false},
                {2, 1, Operator.GREATER_THAN_OR_EQUAL_TO, true},

                {1, 2, Operator.LESS_THAN, true},
                {1, 1, Operator.LESS_THAN, false},
                {1, 2, Operator.LESS_THAN_OR_EQUAL_TO, true},
                {1, 1, Operator.LESS_THAN_OR_EQUAL_TO, true},
                {2, 1, Operator.LESS_THAN_OR_EQUAL_TO, false},

        };
    }


    @Test(enabled = true, dataProvider = "doubleDataProvider")
    public void testApplyTypedDoubleEqual(double firstObject, double secondObject, Operator operator,
                                          boolean expectedToPass) throws Exception {
        Assert.assertEquals(operator.apply(firstObject, secondObject), expectedToPass);
    }


    @DataProvider
    public Object[][] booleanDataProvider() {
        return new Object[][]{
                {true, true, Operator.IS, true},
                {false, false, Operator.IS, true},
                {true, false, Operator.IS, false},
                {false, true, Operator.IS, false},
        };
    }

    @Test(dataProvider = "booleanDataProvider")
    public void testApplyTypedBooleanEqual(boolean first, boolean second, Operator operator, boolean expectedToPass) throws Exception {
        Assert.assertEquals(operator.applyTyped(first, second), expectedToPass);
    }


}
