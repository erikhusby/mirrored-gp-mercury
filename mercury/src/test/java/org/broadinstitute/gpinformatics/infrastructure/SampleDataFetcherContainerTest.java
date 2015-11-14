/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class SampleDataFetcherContainerTest extends ContainerTest {

    public void testGetProductOrderSampleData() {
        ProductOrder productOrder = ProductOrderTestFactory.createProductOrder("KGBN1", "KGBC1", "KGBC2");
        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            assertThat(productOrderSample.isHasBspSampleDataBeenInitialized(), is(false));
        }

        productOrder.loadSampleData();

        for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
            assertThat(productOrderSample.isHasBspSampleDataBeenInitialized(), is(true));
        }

    }



}
