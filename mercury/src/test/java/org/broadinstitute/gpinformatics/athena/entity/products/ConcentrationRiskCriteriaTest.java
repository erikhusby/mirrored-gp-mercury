package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO.*;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/22/13
 * Time: 3:19 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ConcentrationRiskCriteriaTest {

    @Test
    public void testOnRisk() throws Exception {

        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO dummySample1 = new BSPSampleDTO();
        dummySample1.setConcentration(25.0);
        dummySample1.setSampleId("TST-1234");

        ProductOrderSample productOrderSample = new ProductOrderSample(dummySample1.getSampleId(), dummySample1);

        // Create a risk criteria where the sample would be on risk if less than 50.0
        RiskCriteria concentrationRiskCriteria = new RiskCriteria(RiskCriteria.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "50.0");
        boolean actual = concentrationRiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a low sample conc.");

        // Create a risk criteria where the sample would be on risk if greater than 25.0
        concentrationRiskCriteria = new RiskCriteria(RiskCriteria.RiskCriteriaType.CONCENTRATION, Operator.GREATER_THAN, "25.0");
        actual = concentrationRiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if greater than or equal to 25.0
        concentrationRiskCriteria = new RiskCriteria(RiskCriteria.RiskCriteriaType.CONCENTRATION, Operator.GREATER_THAN_OR_EQUAL_TO, "25.0");
        actual = concentrationRiskCriteria.onRisk( productOrderSample );
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Create an invalid risk criteria
        try  {
            new RiskCriteria(RiskCriteria.RiskCriteriaType.CONCENTRATION, Operator.IS, "25.0");
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch ( Exception e ) {
            // npe exception expected
        }
    }
}
