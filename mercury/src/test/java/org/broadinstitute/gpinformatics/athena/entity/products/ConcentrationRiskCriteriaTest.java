package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.meanbean.test.*;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/22/13
 * Time: 3:19 PM
 */
public class ConcentrationRiskCriteriaTest {

    @Test
    public void testOnRisk() throws Exception {

        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO dummySample1 = new BSPSampleDTO("", "", "", "",
                                                    "", "", "", "",
                                                    "", "25.0", "", "",
                                                    "", "", "", "",
                                                    "", "", "", "TST-1234");
        ProductOrderSample productOrderSample = new ProductOrderSample(dummySample1.getSampleId(), dummySample1);

        // Create a risk criteria where the sample would be on risk if less than 50.0
        ConcentrationRiskCriteria concentrationRiskCriteria = new ConcentrationRiskCriteria( "test", NumericOperator.fromLabel("<"), 50.0 );
        boolean actual = concentrationRiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a low sample conc.");

        // Create a risk criteria where the sample would be on risk if greater than 25.0
        concentrationRiskCriteria = new ConcentrationRiskCriteria("test", NumericOperator.fromLabel(">"), 25.0 );
        actual = concentrationRiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if greater than or equal to 25.0
        concentrationRiskCriteria = new ConcentrationRiskCriteria("test", NumericOperator.fromLabel(">="), 25.0 );
        actual = concentrationRiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Create an invalid risk criteria
        try  {
            concentrationRiskCriteria = new ConcentrationRiskCriteria("test", null, 125.0 );
            Assert.fail("Can't create a Risk criterion with a null operator.");
        } catch ( NullPointerException e ) {
            // npe exception expected
        }
    }

    @Test
    public void test_beaniness() {
        BeanTester tester = new BeanTester();
        Configuration configuration = new ConfigurationBuilder()
                .ignoreProperty("risk_criteria_id")
                .build();

        new BeanTester().testBean(ConcentrationRiskCriteria.class, configuration);
    }

    @Test
    public void testEquals() throws Exception {
        new EqualsMethodTester().testEqualsMethod(ConcentrationRiskCriteriaTest.class,"risk_criteria_id");
    }

    @Test
    public void testHashCode() throws Exception {
        new HashCodeMethodTester().testHashCodeMethod(ConcentrationRiskCriteriaTest.class);
    }
}
