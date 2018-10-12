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

package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingEjbTest {

    public static final String ALIQUOT_ID_1 = "SM-ALQT1";
    public static final String ALIQUOT_ID_2 = "SM-ALQT2";
    public static final String STOCK_ID = "SM-STOCK";

    private BillingEjb billingEjb = new BillingEjb(null, null, null, null,
            new SampleDataFetcher() {
                @Override
                public String getStockIdForAliquotId(@Nonnull String aliquotId) {
                    if (aliquotId.equals(ALIQUOT_ID_1) || aliquotId.equals(ALIQUOT_ID_2)) {
                        return STOCK_ID;
                    } else {
                        return aliquotId;
                    }
                }
            },
        null, null, null, null, null);

    public void testMapAliquotIdToSampleInvalid() {
        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(STOCK_ID);
        try {
            billingEjb.mapAliquotIdToSample(order, "SM-BLAH");
            Assert.fail("Exception should be thrown");
        } catch (Exception e) {
            // Error is expected.
            Assert.assertTrue(e.getMessage().contains(order.getBusinessKey()));
        }
    }

    public void testMapAliquotIdToSampleOne() throws Exception {
        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(STOCK_ID);

        // Test case where sample has not yet been mapped to an aliquot.
        List<ProductOrderSample> samples = order.getSamples();
        Assert.assertTrue(samples.size() == 1);
        Assert.assertTrue(samples.get(0).getAliquotId() == null);

        // Now map it.
        ProductOrderSample sample = billingEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        Assert.assertNotNull(sample);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);

        // Test case where sample has already been mapped, should return same sample again.
        sample = billingEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        Assert.assertNotNull(sample);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);

        sample = billingEjb.mapAliquotIdToSample(order, ALIQUOT_ID_2);
        Assert.assertNull(sample);
    }

    public void testMapAliquotToSampleTwo() throws Exception {
        // Test case where there are multiple samples, where each one maps to a different aliquot.

        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(STOCK_ID, STOCK_ID);
        ProductOrderSample sample = billingEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        ProductOrderSample sample2 = billingEjb.mapAliquotIdToSample(order, ALIQUOT_ID_2);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);
        Assert.assertEquals(sample2.getAliquotId(), ALIQUOT_ID_2);
    }
}
