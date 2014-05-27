package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class NumericOperatorTest {

    @Test
    public void testFromLabel() throws Exception {
        Assert.assertEquals(Operator.LESS_THAN, Operator.fromLabel("<") );
        Assert.assertEquals(Operator.LESS_THAN_OR_EQUAL_TO, Operator.fromLabel("<=") );
        Assert.assertEquals(Operator.GREATER_THAN, Operator.fromLabel(">") );
        Assert.assertEquals(Operator.GREATER_THAN_OR_EQUAL_TO, Operator.fromLabel(">=") );
        // Create an invalid Numeric Operator for equals to
        Assert.assertEquals(Operator.EQUALS, Operator.fromLabel("=") );
    }
}
