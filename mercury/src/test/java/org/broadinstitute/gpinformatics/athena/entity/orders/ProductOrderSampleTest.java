package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.test.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

    @Test
    public void testBeaniness() {
        Configuration configuration = new ConfigurationBuilder().ignoreProperty("productOrder").ignoreProperty("sampleComment")
                .ignoreProperty("bspDTO").ignoreProperty("billingStatus").build();
        new BeanTester().testBean(ProductOrderSample.class, configuration);

        class ProductOrderSampleFactory implements EquivalentFactory<ProductOrderSample> {
            @Override public ProductOrderSample create() {
                ProductOrderSample sample = new ProductOrderSample("SM-12345", BSPSampleDTO.DUMMY);
                sample.setSamplePosition(0);
                return sample;
            }
        }

        new EqualsMethodTester().testEqualsMethod(new ProductOrderSampleFactory(), configuration);

        new HashCodeMethodTester().testHashCodeMethod(new ProductOrderSampleFactory());

    }

    public static List<ProductOrderSample> createSampleList(String[] sampleArray,
                                                            Collection<BillingLedger> billableItems,
                                                            boolean dbFree) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>(sampleArray.length);
        for (String sampleName : sampleArray) {
            ProductOrderSample productOrderSample;
            if (dbFree) {
                productOrderSample = new ProductOrderSample(sampleName, BSPSampleDTO.DUMMY);
            } else {
                productOrderSample = new ProductOrderSample(sampleName);
            }

            productOrderSample.setSampleComment("athenaComment");

            productOrderSample.getLedgerItems().addAll( billableItems );

            productOrderSamples.add(productOrderSample);


        }
        return productOrderSamples;
    }


}
