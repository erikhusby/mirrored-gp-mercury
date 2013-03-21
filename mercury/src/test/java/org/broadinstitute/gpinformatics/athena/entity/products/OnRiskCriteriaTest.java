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

        // Create one sample with LOW_NUMBER for the conc, and one with the HIGH_NUMBER for the conc to be tested
        BSPSampleDTO lowNumSample = BSPSampleDTO.createConcentrationSampleDummy(LOW_NUMBER, "TST-1234");
        BSPSampleDTO highNumSample =  BSPSampleDTO.createConcentrationSampleDummy(HIGH_NUMBER, "TST-1235");

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriterion.RiskCriteriaType.CONCENTRATION);
    }

    /**
     * Tests Volume on risk
     */
    @Test
    public void testVolumeOnRisk() {
        BSPSampleDTO lowNumSample = BSPSampleDTO.createVolumeSampleDummy(LOW_NUMBER, "TST-1234");
        BSPSampleDTO highNumSample =  BSPSampleDTO.createVolumeSampleDummy(HIGH_NUMBER, "TST-1235");

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriterion.RiskCriteriaType.VOLUME);
    }

    /**
     * test Total DNA On risk
     */
    @Test
    public void testTotalDnaOnRisk() {
        BSPSampleDTO lowNumSample = BSPSampleDTO.createTotalDNASampleDummy(LOW_NUMBER, "TST-1234");
        BSPSampleDTO highNumSample =  BSPSampleDTO.createTotalDNASampleDummy(HIGH_NUMBER, "TST-1235");

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriterion.RiskCriteriaType.TOTAL_DNA);
    }

    /**
     * Tests WGA on risk
     */
    @Test
    public void testWGAOnRisk() {

        // Create one sample with WGA for material and one with non-WGA for material
        BSPSampleDTO hasWgaDummy = BSPSampleDTO.createMaterialTypeSampleDummy("DNA:DNA WGA Cleaned", "TST-1234");
        BSPSampleDTO nonWgaDummy = BSPSampleDTO.createMaterialTypeSampleDummy("DNA:DNA Genomic", "TST-1235");

        handleBooleanOnRisk(hasWgaDummy, nonWgaDummy, RiskCriterion.RiskCriteriaType.WGA);
    }

    /**
     * Tests FFPE on risk
     */
    @Test
    public void testFFPEOnRisk() {

        // Create one sample with FFPE status of true, and one FFPE status of false
        BSPSampleDTO hasWgaDummy = BSPSampleDTO.createMaterialTypeSampleDummy("DNA:DNA WGA Cleaned", "TST-1234");
        hasWgaDummy.setFfpeStatus(true);

        BSPSampleDTO nonWgaDummy = BSPSampleDTO.createMaterialTypeSampleDummy("DNA:DNA WGA Cleaned", "TST-1235");
        nonWgaDummy.setFfpeStatus(false);

        handleBooleanOnRisk(hasWgaDummy, nonWgaDummy, RiskCriterion.RiskCriteriaType.FFPE);
    }

    ////////////////////////////////////////////////////////////
    // Logic to run the actual tests
    ////////////////////////////////////////////////////////////

    /**
     * Handles the testing of Boolean based risk criteria
     *
     * @param onRiskSample     Sample DTO Which is expected to be on risk
     * @param notOnRiskSample  Sample DTO which is expected to NOT be on risk
     * @param riskCriteriaType Risk Criteria Type to test with.  Fails on non-boolean based
     */
    private void handleBooleanOnRisk(BSPSampleDTO onRiskSample, BSPSampleDTO notOnRiskSample,
                                     RiskCriterion.RiskCriteriaType riskCriteriaType) {

        Assert.assertEquals(riskCriteriaType.getOperatorType(), Operator.OperatorType.BOOLEAN);

        // create the PDO samples
        ProductOrderSample expectedOnRisk = new ProductOrderSample(onRiskSample.getSampleId(), onRiskSample);
        ProductOrderSample notOnRisk = new ProductOrderSample(notOnRiskSample.getSampleId(), notOnRiskSample);

        // Test with sample where expect on risk
        RiskCriterion wgaRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.IS, "true");
        boolean actual = wgaRiskCriterion.onRisk(expectedOnRisk);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a wga material.");

        // Test with sample where expected to not be on risk
        actual = wgaRiskCriterion.onRisk(notOnRisk);
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to  wga material.");

        // Create an invalid risk criteria of each type
        try {
            new RiskCriterion(riskCriteriaType, Operator.LESS_THAN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriterion(riskCriteriaType, Operator.EQUALS, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriterion(riskCriteriaType, Operator.EXACT_MATCH, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriterion(riskCriteriaType, Operator.IS_IN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
        try {
            new RiskCriterion(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, "true");
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
     * @param riskCriteriaType Risk Criteria Type to test with.  Fails on non-numeric based
     */
    private void handleNumericOnRisk(BSPSampleDTO lowNumSample, BSPSampleDTO highNumSample,
                                     RiskCriterion.RiskCriteriaType riskCriteriaType) {

        Assert.assertEquals(riskCriteriaType.getOperatorType(), Operator.OperatorType.NUMERIC);

        ProductOrderSample lowNumberPOSample = new ProductOrderSample(lowNumSample.getSampleId(), lowNumSample);
        ProductOrderSample highNumberPOSample = new ProductOrderSample(highNumSample.getSampleId(), highNumSample);

        //  EXPECT TO BE ON RISK START

        // Test less than with a high test value and the value being tested is low.
        RiskCriterion numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN, HIGH_NUMBER);
        boolean actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a low sample conc.");

        // Test greater than with a low test value and the value being tested is high.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should not have been on risk due to high conc.");

        // Test Greater than or equal to with a low test value and the value being testing being equal
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Test Greater than or equal to with a low test value and the value being testing being high
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Test Less than or Equal to with a high test value and the value being tested being equal
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Test Less than or Equal To with a high test value and the value being tested is low
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, HIGH_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Test equals with two values that are equal
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.EQUALS, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // EXPECT TO BE ON RISK END


        // EXPECT TO NOT BE ON RISK START

        // test Less than with a low test value and the value being tested is high
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to a low sample conc.");

        // test Greater than with a high test value and the value being tested is low
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN, HIGH_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to high conc.");

        // test Greater than or equal to with a high test value and the value being tested is low
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, HIGH_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // test Less than or equal to with the test value being low and the value being tested is high
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // test equals with different values
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.EQUALS, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // EXPECT TO NOT BE ON RISK END

        // Create an invalid risk criteria
        try {
            new RiskCriterion(riskCriteriaType, Operator.IS, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }

        // Create an invalid risk criteria
        try {
            new RiskCriterion(riskCriteriaType, Operator.IS_IN, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }

        // Create an invalid risk criteria
        try {
            new RiskCriterion(riskCriteriaType, Operator.EXACT_MATCH, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected
        }
    }
}
