/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderSampleTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class AggregationParticleDbFreeTest {

    public static final String PDO_12345 = "PDO-12345";
    public static final String SM_1234 = "SM-1234";
    ProductOrderSample productOrderSample;
    ProductOrder productOrder;

    @BeforeMethod
    public void setUp() {
        productOrderSample = ProductOrderSampleTestFactory.createDBFreeSampleList(SM_1234).iterator().next();
        productOrder = ProductOrderTestFactory.createDummyProductOrder(PDO_12345);
        productOrder.addSample(productOrderSample);
    }

    public void testFindAgpNullSample(){
        productOrder.setDefaultAggregationParticle(Product.AggregationParticle.PDO);
        assertThat(productOrderSample.getAggregationParticle(), equalTo(PDO_12345));
    }

    public void testFindAgpInSampleNullOrder(){
        productOrderSample.setAggregationParticle(SM_1234);
        assertThat(productOrderSample.getAggregationParticle(), equalTo(SM_1234));
    }

    public void testFindAgpNullSampleAndOrder(){
        assertThat(productOrderSample.getAggregationParticle(), nullValue());
    }

    public void testFindAgpSetInSampleThenChangeOrderAgp(){
        productOrderSample.setAggregationParticle(SM_1234);
        assertThat(productOrderSample.getAggregationParticle(), equalTo(SM_1234));

        productOrder.setDefaultAggregationParticle(Product.AggregationParticle.PDO);
        assertThat(productOrderSample.getAggregationParticle(), equalTo(SM_1234));
    }
}
