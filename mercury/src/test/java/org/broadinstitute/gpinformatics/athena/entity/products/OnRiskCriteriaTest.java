package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 1/22/13
 * Time: 3:19 PM
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class OnRiskCriteriaTest {

    private static final String LOW_NUMBER = "25.0";
    private static final String HIGH_NUMBER = "50.0";

    /**
     * Tests Concentration on risk
     */
    @Test
    public void testConcentrationOnRisk() {

        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO lowNumSample = BSPSampleDTO.createDummy();
        lowNumSample.setConcentration(Double.parseDouble(LOW_NUMBER));
        lowNumSample.setSampleId("TST-1234");

        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO highNumSample = BSPSampleDTO.createDummy();
        highNumSample.setConcentration(Double.parseDouble(HIGH_NUMBER));
        highNumSample.setSampleId("TST-1234");

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriteria.RiskCriteriaType.CONCENTRATION);
    }

    /**
     * Tests Volume on risk
     */
    @Test
    public void testVolumeOnRisk() {

        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO lowNumSample = BSPSampleDTO.createDummy();
        lowNumSample.setVolume(Double.parseDouble(LOW_NUMBER));
        lowNumSample.setSampleId("TST-1234");

        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO highNumSample = BSPSampleDTO.createDummy();
        highNumSample.setVolume(Double.parseDouble(HIGH_NUMBER));
        highNumSample.setSampleId("TST-1234");

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriteria.RiskCriteriaType.VOLUME);
    }

    @Test
    public void testTotalDnaOnRisk() {

        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO lowNumSample = BSPSampleDTO.createDummy();
        lowNumSample.setTotal(Double.parseDouble(LOW_NUMBER));
        lowNumSample.setSampleId("TST-1234");

        // Create a sample and set the name and conc to 25 ( ng/uL )
        BSPSampleDTO highNumSample = BSPSampleDTO.createDummy();
        highNumSample.setTotal(Double.parseDouble(HIGH_NUMBER));
        highNumSample.setSampleId("TST-1234");

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriteria.RiskCriteriaType.TOTAL_DNA);
    }

    /**
     * Tests WGA on risk
     */
    @Test
    public void testWGAOnRisk() {

        // Create 2 samples and set the name and material type
        BSPSampleDTO hasWgaDummy = BSPSampleDTO.createDummy();
        hasWgaDummy.setMaterialType("DNA:DNA WGA Cleaned");
        hasWgaDummy.setSampleId("TST-1234");

        BSPSampleDTO nonWgaDummy = BSPSampleDTO.createDummy();
        nonWgaDummy.setMaterialType("DNA:DNA Genomic");
        nonWgaDummy.setSampleId("TST-1235");

        handleBooleanOnRisk(hasWgaDummy, nonWgaDummy, RiskCriteria.RiskCriteriaType.WGA);
    }

    /**
     * Tests WGA on risk
     */
    @Test
    public void testFFPEOnRisk() {

        // Create 2 samples and set the name and material type
        BSPSampleDTO hasWgaDummy = BSPSampleDTO.createDummy();
        hasWgaDummy.setFfpeStatus(true);
        hasWgaDummy.setSampleId("TST-1234");

        BSPSampleDTO nonWgaDummy = BSPSampleDTO.createDummy();
        nonWgaDummy.setFfpeStatus(false);
        nonWgaDummy.setSampleId("TST-1235");

        handleBooleanOnRisk(hasWgaDummy, nonWgaDummy, RiskCriteria.RiskCriteriaType.FFPE);
    }

    ////////////////////////////////////////////////////////////
    // Logic to run the actual tests
    ////////////////////////////////////////////////////////////

    /**
     * Handles the testing of Boolean based risk criteria
     *
     * @param onRiskSample     Sample DTO Which is expected to be on risk
     * @param notOnRiskSample  Sample DTO which is expected to NOT be on risk
     * @param riskCriteriaType Risk Criteria Type to test with.  Should be boolean based
     */
    private void handleBooleanOnRisk(BSPSampleDTO onRiskSample, BSPSampleDTO notOnRiskSample,
                                     RiskCriteria.RiskCriteriaType riskCriteriaType) {

        Assert.assertEquals(riskCriteriaType.getOperatorType(), Operator.OperatorType.BOOLEAN);

        // create the PDO samples
        ProductOrderSample expectedOnRisk = new ProductOrderSample(onRiskSample.getSampleId(), onRiskSample);
        ProductOrderSample notOnRisk = new ProductOrderSample(notOnRiskSample.getSampleId(), notOnRiskSample);

        // Create a risk criteria where the sample would be on risk if material is WGA
        RiskCriteria wgaRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.IS, "true");
        boolean actual = wgaRiskCriteria.onRisk(expectedOnRisk);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a wga material.");

        // Use same risk criteria where the sample would be on risk if WGA but the sample is not
        actual = wgaRiskCriteria.onRisk(notOnRisk);
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to  wga material.");

        // Create an invalid risk criteria
        try {
            new RiskCriteria(riskCriteriaType, Operator.LESS_THAN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriteria(riskCriteriaType, Operator.EQUALS, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriteria(riskCriteriaType, Operator.EXACT_MATCH, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriteria(riskCriteriaType, Operator.GREATER_THAN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriteria(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriteria(riskCriteriaType, Operator.IS_IN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriteria(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
    }

    /**
     * Handles the testing of numeric on risk
     *
     * @param lowNumSample     sample with a low number of whatever is being tested
     * @param highNumSample    sample with a high number of whatever is being tested
     * @param riskCriteriaType criteria type to test
     */
    private void handleNumericOnRisk(BSPSampleDTO lowNumSample, BSPSampleDTO highNumSample,
                                     RiskCriteria.RiskCriteriaType riskCriteriaType) {
        ProductOrderSample lowNumberPOSample = new ProductOrderSample(lowNumSample.getSampleId(), lowNumSample);
        ProductOrderSample highNumberPOSample = new ProductOrderSample(highNumSample.getSampleId(), highNumSample);

        //  EXPECT TO BE ON RISK START

        // Create a risk criteria where the sample would be on risk if less than 50.0
        RiskCriteria numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.LESS_THAN, HIGH_NUMBER);
        boolean actual = numericRiskCriteria.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a low sample conc.");

        // Create a risk criteria where the sample would be on risk if greater than 25.0
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.GREATER_THAN, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should not have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if greater than or equal to 25.0 which would be
        // found with the equal to part
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if greater than or equal to 25.0 which would be
        // found with the greater than part
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if less than or equal to 25 which would be
        // found with the equal to part
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if less than or equal to 25 which would be
        // found with the less than part
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, HIGH_NUMBER);
        actual = numericRiskCriteria.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if less than or equal to 25
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.EQUALS, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // EXPECT TO BE ON RISK END


        // EXPECT TO NOT BE ON RISK START

        // Create a risk criteria where the sample would be on risk if less than 50.0
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.LESS_THAN, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to a low sample conc.");

        // Create a risk criteria where the sample would be on risk if greater than 25.0
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.GREATER_THAN, HIGH_NUMBER);
        actual = numericRiskCriteria.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if greater than or equal to 25.0
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, HIGH_NUMBER);
        actual = numericRiskCriteria.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if less than or equal to 25 which would be found
        // with the equal to part
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if less than or equal to 25 which would be found
        // with the less than part
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // Create a risk criteria where the sample would be on risk if less than or equal to 25
        numericRiskCriteria = new RiskCriteria(riskCriteriaType, Operator.EQUALS, LOW_NUMBER);
        actual = numericRiskCriteria.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // EXPECT TO NOT BE ON RISK END

        // Create an invalid risk criteria
        try {
            new RiskCriteria(riskCriteriaType, Operator.IS, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }

        // Create an invalid risk criteria
        try {
            new RiskCriteria(riskCriteriaType, Operator.IS_IN, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }

        // Create an invalid risk criteria
        try {
            new RiskCriteria(riskCriteriaType, Operator.EXACT_MATCH, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
    }
}
