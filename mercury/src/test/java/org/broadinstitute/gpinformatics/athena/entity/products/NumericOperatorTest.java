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
        Assert.assertTrue(Operator.LESS_THAN.apply(1.0, 2.0));
        Assert.assertTrue(Operator.LESS_THAN_OR_EQUAL_TO.apply(1.0, 1.0));
        Assert.assertTrue(Operator.LESS_THAN_OR_EQUAL_TO.apply(1.0, 2.0));
        Assert.assertTrue(Operator.GREATER_THAN.apply(2.0, 1.0));
        Assert.assertTrue(Operator.GREATER_THAN_OR_EQUAL_TO.apply(1.0, 1.0));
        Assert.assertTrue(Operator.GREATER_THAN_OR_EQUAL_TO.apply(2.0, 1.0));
    }

    @Test
    public void testFromLabel() throws Exception {
        Assert.assertEquals(Operator.LESS_THAN, Operator.fromLabel("<") );
        Assert.assertEquals(Operator.LESS_THAN_OR_EQUAL_TO, Operator.fromLabel("<=") );
        Assert.assertEquals(Operator.GREATER_THAN, Operator.fromLabel(">") );
        Assert.assertEquals(Operator.GREATER_THAN_OR_EQUAL_TO, Operator.fromLabel(">=") );
        // Create an invalid Numeric Operator for equals to
        Assert.assertEquals(null, Operator.fromLabel("=") );
    }
}
