package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * A test.
 *
 * @author mccory
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class ProductOrderSampleTest {
    @Test
    public void testIsInBspFormat() throws Exception {
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG"));
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG5"));
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG6"));
        Assert.assertFalse(ProductOrderSample.isInBspFormat("Blahblahblah"));
        Assert.assertFalse(ProductOrderSample.isInBspFormat("12345"));
    }
}
