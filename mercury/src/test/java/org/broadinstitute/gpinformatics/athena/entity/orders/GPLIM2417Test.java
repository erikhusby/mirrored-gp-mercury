package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

@Test(groups = {TestGroups.STUBBY})
@Dependent
public class GPLIM2417Test extends StubbyContainerTest {

    public GPLIM2417Test(){}

    @Inject
    ProductOrderDao pdoDao;

    @Test
    public void testThatPDOWIthMoreThan1000SamplesWillLoad() {
        ProductOrder pdo = pdoDao.findByBusinessKey("PDO-1312");
        try {
            ProductOrder.loadLabEventSampleData(pdo.getSamples());

            int vessels = 0;
            for (ProductOrderSample sample : pdo.getSamples()) {
                vessels += sample.getLabEventSampleDTO().getLabVessels().size();
            }

            // If this does not explode, then the number of vessels should be >1000, so check that this is the case.
            Assert.assertTrue(vessels > BaseSplitter.DEFAULT_SPLIT_SIZE);
        }
        catch(Throwable t) {
            Assert.fail();
        }

    }
}
