package org.broadinstitute.gpinformatics.athena.boundary.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderEjbTest {

    public static final String ALIQUOT_ID_1 = "SM-ALIQUOT1";
    public static final String ALIQUOT_ID_2 = "SM-ALIQUOT2";
    public static final String STOCK_ID = "SM-STOCK";

    ProductOrderEjb productOrderEjb = new ProductOrderEjb(null, null, null, null, null, null, null,
            new BSPSampleDataFetcher(new BSPSampleSearchService() {
                @Override
                public List<Map<BSPSampleSearchColumn, String>> runSampleSearch(final Collection<String> sampleIDs,
                                                                                BSPSampleSearchColumn... resultColumns) {
                    // For this test case, both aliquots map to the same sample.
                    return new ArrayList<Map<BSPSampleSearchColumn, String>>() {{
                        add(new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                            put(BSPSampleSearchColumn.STOCK_SAMPLE, STOCK_ID);
                            put(BSPSampleSearchColumn.SAMPLE_ID, sampleIDs.iterator().next());
                        }});
                    }};
                }
            }), null);

    public void testMapAliquotIdToSampleOne() throws Exception {
        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(STOCK_ID);

        // Test case where sample has not yet been mapped to an aliquot.
        List<ProductOrderSample> samples = order.getSamples();
        Assert.assertTrue(samples.size() == 1);
        Assert.assertTrue(samples.get(0).getAliquotId() == null);

        // Now map it.
        ProductOrderSample sample = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        Assert.assertNotNull(sample);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);

        // Test case where sample has already been mapped, should return same sample again.
        sample = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        Assert.assertNotNull(sample);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);

        // Try to map another aliquot, should get an exception.
        try {
            sample = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_2);
            Assert.fail("Exception should be thrown");
        } catch (Exception e) {
            // Error is expected.
        }
    }

    public void testMapAliquotToSampleTwo() throws Exception {
        // Test case where there are multiple samples, where each one maps to a different aliquot.

        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(STOCK_ID, STOCK_ID);
        ProductOrderSample sample = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_1);
        ProductOrderSample sample2 = productOrderEjb.mapAliquotIdToSample(order, ALIQUOT_ID_2);
        Assert.assertEquals(sample.getAliquotId(), ALIQUOT_ID_1);
        Assert.assertEquals(sample2.getAliquotId(), ALIQUOT_ID_2);
    }
}
