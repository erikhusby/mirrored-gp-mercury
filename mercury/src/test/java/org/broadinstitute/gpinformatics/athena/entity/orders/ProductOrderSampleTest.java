package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfigProducer;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/3/12
 * Time: 5:59 PM
 */
@Test(groups = {TestGroups.DATABASE_FREE})
public class ProductOrderSampleTest {

    @Test
    public void testIsInBspFormat() throws Exception {

        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG"));
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG5"));
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG6"));
        Assert.assertFalse(ProductOrderSample.isInBspFormat(null));

    }

    public void testOrderSampleConstruction() {

        BSPSampleDataFetcher fetcher =
                new BSPSampleDataFetcher(BSPSampleSearchServiceProducer.stubInstance());

    }


}
