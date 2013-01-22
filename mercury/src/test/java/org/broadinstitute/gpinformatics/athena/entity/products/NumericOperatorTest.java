package org.broadinstitute.gpinformatics.athena.entity.products;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/22/13
 * Time: 3:59 PM
 */
public class NumericOperatorTest {
    @Test
    public void testApply() throws Exception {
        Assert.assertTrue(NumericOperator.LESS_THAN.apply(1.0, 2.0));
        Assert.assertTrue(NumericOperator.LESS_THAN_OR_EQUAL_TO.apply(1.0, 1.0));
        Assert.assertTrue(NumericOperator.LESS_THAN_OR_EQUAL_TO.apply(1.0, 2.0));
        Assert.assertTrue(NumericOperator.GREATER_THAN.apply(2.0, 1.0));
        Assert.assertTrue(NumericOperator.GREATER_THAN_OR_EQUAL_TO.apply(1.0, 1.0));
        Assert.assertTrue(NumericOperator.GREATER_THAN_OR_EQUAL_TO.apply(2.0, 1.0));
    }

    @Test
    public void testFromLabel() throws Exception {
        Assert.assertEquals(NumericOperator.LESS_THAN, NumericOperator.fromLabel("<") );
        Assert.assertEquals(NumericOperator.LESS_THAN_OR_EQUAL_TO, NumericOperator.fromLabel("<=") );
        Assert.assertEquals(NumericOperator.GREATER_THAN, NumericOperator.fromLabel(">") );
        Assert.assertEquals(NumericOperator.GREATER_THAN_OR_EQUAL_TO, NumericOperator.fromLabel(">=") );
        // Create an invalid Numeric Operator for equals to
        Assert.assertEquals(null, NumericOperator.fromLabel("=") );
    }
}
