package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/22/13
 * Time: 3:20 PM
 */
public class TotalDNARiskCriteriaTest {
    @Test
    public void testOnRisk() throws Exception {
        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO dummySample1 = new BSPSampleDTO("", "", "", "",
                                                    "", "", "", "",
                                                    "", "", "", "",
                                                    "", "200", "", "",
                                                    "", "", "", "TST-1234");
        ProductOrderSample productOrderSample = new ProductOrderSample(dummySample1.getSampleId(), dummySample1);

        // Create a risk criteria where the sample would be on risk if less than 300
        TotalDNARiskCriteria totalDNARiskCriteria = new TotalDNARiskCriteria( "test", NumericOperator.fromLabel("<"), 300.0 );
        boolean actual = totalDNARiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a low dna total.");

        // Create a risk criteria where the sample would be on risk if greater than the specified value
        totalDNARiskCriteria = new TotalDNARiskCriteria("test", NumericOperator.fromLabel(">"), 5000.0 );
        actual = totalDNARiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to high dna total.");

        // Create a risk criteria where the sample would be on risk if greater than or equal to 25.0
        totalDNARiskCriteria = new TotalDNARiskCriteria("test", NumericOperator.fromLabel(">="), 200.0 );
        actual = totalDNARiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high dna total");

        // Create an invalid risk criteria
        try  {
            totalDNARiskCriteria = new TotalDNARiskCriteria("test", null, 125.0 );
            Assert.fail("Can't create a Risk criterion with a null operator.");
        } catch ( NullPointerException e ) {
            // npe exception expected
        }
    }

    @Test
    public void testEquals() throws Exception {
        new EqualsMethodTester().testEqualsMethod(TotalDNARiskCriteria.class );
    }

    @Test
    public void testHashCode() throws Exception {
        new HashCodeMethodTester().testHashCodeMethod(TotalDNARiskCriteria.class);
    }

}
