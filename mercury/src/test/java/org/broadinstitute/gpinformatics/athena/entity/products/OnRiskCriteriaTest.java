package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class OnRiskCriteriaTest {

    private static final String LOW_NUMBER = "25.0";
    private static final String HIGH_NUMBER = "50.0";
    private static final String SM_1234 = "SM-1234";
    private static final String SM_1235 = "SM-1235";

    /**
     * Tests Concentration on risk.
     */
    @Test
    public void testConcentrationOnRisk() {
        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.CONCENTRATION, LOW_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};

        SampleData lowNumSample = new BspSampleData(dataMap);
        dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.CONCENTRATION, HIGH_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};
        SampleData highNumSample = new BspSampleData(dataMap);

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriterion.RiskCriteriaType.CONCENTRATION);
    }

    /**
     * Tests Volume on risk.
     */
    @Test
    public void testVolumeOnRisk() {

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.VOLUME, LOW_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};
        SampleData lowNumSample = new BspSampleData(dataMap);

        dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.VOLUME, HIGH_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1235);
        }};

        SampleData highNumSample =  new BspSampleData(dataMap);
        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriterion.RiskCriteriaType.VOLUME);
    }

    /**
     * test RIN risk
     */
    @Test
    public void testRin() {
        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.RIN, LOW_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};
        SampleData lowNumSample = new BspSampleData(dataMap);

        dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.RIN, HIGH_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1235);
        }};
        SampleData highNumSample =  new BspSampleData(dataMap);

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriterion.RiskCriteriaType.RIN);

        // Test RIN ranges.  This first instance should effectively give the low number.
        dataMap = new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.RIN, LOW_NUMBER + " - " + HIGH_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};
        lowNumSample = new BspSampleData(dataMap);

        // Putting the high number in twice should effectively give the high number.
        dataMap = new HashMap<BSPSampleSearchColumn, String>() {{
            put(BSPSampleSearchColumn.RIN, HIGH_NUMBER + " - " + HIGH_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};
        highNumSample = new BspSampleData(dataMap);

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriterion.RiskCriteriaType.RIN);

    }

    @Test
    public void testOnRiskWhenRqsIsNotSet() {
        ProductOrderSample productOrderSample = makeProductOrderSampleWithRQS(null);
        RiskCriterion riskCriterion = new RiskCriterion(RiskCriterion.RiskCriteriaType.RQS, Operator.LESS_THAN, "5.5");
        Assert.assertTrue(riskCriterion.onRisk(productOrderSample));
    }

    @Test
    public void testOnRiskWhenRQSIsTooLow() {
        ProductOrderSample productOrderSample = makeProductOrderSampleWithRQS("5");
        RiskCriterion riskCriterion = new RiskCriterion(RiskCriterion.RiskCriteriaType.RQS, Operator.LESS_THAN, "5.5");
        Assert.assertTrue(riskCriterion.onRisk(productOrderSample));
    }

    @Test
    public void testNotOnRiskWhenRQSIsHighEnough() {
        ProductOrderSample productOrderSample = makeProductOrderSampleWithRQS("6");
        RiskCriterion riskCriterion = new RiskCriterion(RiskCriterion.RiskCriteriaType.RQS, Operator.LESS_THAN, "5.5");
        Assert.assertFalse(riskCriterion.onRisk(productOrderSample));
    }

    private ProductOrderSample makeProductOrderSampleWithRQS(String rqs) {
        HashMap<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234");
        if (rqs != null) {
            data.put(BSPSampleSearchColumn.RQS, rqs);
        }
        SampleData sampleData = new BspSampleData(data);
        return new ProductOrderSample("SM-1234", sampleData);
    }

    /**
     * test Total DNA On risk.
     */
    @Test
    public void testTotalDnaOnRisk() {
        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.TOTAL_DNA, LOW_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};
        SampleData lowNumSample = new BspSampleData(dataMap);

        dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.TOTAL_DNA, HIGH_NUMBER);
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1235);
        }};
        SampleData highNumSample =  new BspSampleData(dataMap);

        handleNumericOnRisk(lowNumSample, highNumSample, RiskCriterion.RiskCriteriaType.TOTAL_DNA);
    }

    /**
     * Tests WGA on risk.
     */
    @Test
    public void testWGAOnRisk() {

        // Create one sample with WGA for material and one with non-WGA for material
        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA WGA Cleaned");
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};
        SampleData hasWgaDummy = new BspSampleData(dataMap);

        dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic");
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1235);
        }};
        SampleData nonWgaDummy = new BspSampleData(dataMap);

        handleBooleanOnRisk(hasWgaDummy, nonWgaDummy, RiskCriterion.RiskCriteriaType.WGA);
    }

    @Test
    public void testWGAOnRiskMercurySampleDoesNotThrowNPE() {
        SampleData hasWgaDummy = new MercurySampleData(SM_1234, Collections.<Metadata>emptySet());

        ProductOrderSample productOrderSample = new ProductOrderSample(SM_1234, hasWgaDummy);
        ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(1);
        productOrder.getProduct().addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.WGA, Operator.IS, null));
        productOrder.addSample(productOrderSample);
        Assert.assertTrue(productOrderSample.calculateRisk());
    }

    /**
     * Tests FFPE on risk.
     */
    @Test
    public void testFFPEOnRisk() {

        Map<BSPSampleSearchColumn, String> dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA WGA Cleaned");
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1234);
        }};
        BspSampleData hasWgaDummy = new BspSampleData(dataMap);
        hasWgaDummy.setFfpeStatus(true);

        dataMap = new HashMap<BSPSampleSearchColumn, String>(){{
            put(BSPSampleSearchColumn.TOTAL_DNA, "DNA:DNA WGA Cleaned");
            put(BSPSampleSearchColumn.SAMPLE_ID, SM_1235);
        }};
        BspSampleData nonWgaDummy =  new BspSampleData(dataMap);
        nonWgaDummy.setFfpeStatus(false);

        // Create one sample with FFPE status of true, and one FFPE status of false
        handleBooleanOnRisk(hasWgaDummy, nonWgaDummy, RiskCriterion.RiskCriteriaType.FFPE);
    }

    ////////////////////////////////////////////////////////////
    // Logic to run the actual tests.
    ////////////////////////////////////////////////////////////

    /**
     * Handles the testing of Boolean based risk criteria.
     *
     * @param onRiskSample     Sample DTO Which is expected to be on risk.
     * @param notOnRiskSample  Sample DTO which is expected to NOT be on risk.
     * @param riskCriteriaType Risk Criteria Type to test with.  Fails on non-boolean based.
     */
    private void handleBooleanOnRisk(SampleData onRiskSample, SampleData notOnRiskSample,
                                     RiskCriterion.RiskCriteriaType riskCriteriaType) {

        Assert.assertEquals(riskCriteriaType.getOperatorType(), Operator.OperatorType.BOOLEAN);

        // create the PDO samples.
        ProductOrderSample expectedOnRisk = new ProductOrderSample(onRiskSample.getSampleId(), onRiskSample);
        ProductOrderSample notOnRisk = new ProductOrderSample(notOnRiskSample.getSampleId(), notOnRiskSample);

        // Test with sample where expect on risk.
        RiskCriterion wgaRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.IS, "true");
        boolean actual = wgaRiskCriterion.onRisk(expectedOnRisk);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a wga material.");

        // Test with sample where expected to not be on risk.
        actual = wgaRiskCriterion.onRisk(notOnRisk);
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to  wga material.");

        // Create an invalid risk criteria of each type.
        try {
            new RiskCriterion(riskCriteriaType, Operator.LESS_THAN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }

        try {
            new RiskCriterion(riskCriteriaType, Operator.EQUALS, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }

        try {
            new RiskCriterion(riskCriteriaType, Operator.EXACT_MATCH, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }

        try {
            new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }

        try {
            new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }

        try {
            new RiskCriterion(riskCriteriaType, Operator.IS_IN, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }

        try {
            new RiskCriterion(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, "true");
            Assert.fail("Can't create a wga Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }
    }

    /**
     * Handles the testing of numeric on risk.
     *
     * @param lowNumSample     sample with a low number of whatever is being tested.
     * @param highNumSample    sample with a high number of whatever is being tested.
     * @param riskCriteriaType Risk Criteria Type to test with.  Fails on non-numeric based.
     */
    private void handleNumericOnRisk(SampleData lowNumSample, SampleData highNumSample,
                                     RiskCriterion.RiskCriteriaType riskCriteriaType) {

        Assert.assertEquals(riskCriteriaType.getOperatorType(), Operator.OperatorType.NUMERIC);

        ProductOrderSample lowNumberPOSample = new ProductOrderSample(lowNumSample.getSampleId(), lowNumSample);
        ProductOrderSample highNumberPOSample = new ProductOrderSample(highNumSample.getSampleId(), highNumSample);

        Map<BSPSampleSearchColumn, String> data = new HashMap<>();
        data.put(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234");
        SampleData missingNumSample = new BspSampleData(data);
        ProductOrderSample missingNumberPOSample = new ProductOrderSample("SM-1234", missingNumSample);

        //  EXPECT TO BE ON RISK START.

        // Test where the value being tested is missing.
        RiskCriterion numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN, "1");
        boolean actual = numericRiskCriterion.onRisk(missingNumberPOSample);
        Assert.assertTrue(actual,
                "Sample should have been on risk due to having no data for the criteria being checked.");

        // Test less than with a high test value and the value being tested is low.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN, HIGH_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to a low sample conc.");

        // Test greater than with a low test value and the value being tested is high.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should not have been on risk due to high conc.");

        // Test Greater than or equal to with a low test value and the value being testing being equal.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Test Greater than or equal to with a low test value and the value being testing being high.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Test Less than or Equal to with a high test value and the value being tested being equal.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Test Less than or Equal To with a high test value and the value being tested is low.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, HIGH_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // Test equals with two values that are equal.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.EQUALS, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, true, "Sample should have been on risk due to high conc.");

        // test Less than with a low test value and the value being tested is high.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to a low sample conc.");

        // test Greater than with a high test value and the value being tested is low.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN, HIGH_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should not have been on risk due to high conc.");

        // test Greater than or equal to with a high test value and the value being tested is low.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.GREATER_THAN_OR_EQUAL_TO, HIGH_NUMBER);
        actual = numericRiskCriterion.onRisk(lowNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // test Less than or equal to with the test value being low and the value being tested is high.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.LESS_THAN_OR_EQUAL_TO, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // test equals with different values.
        numericRiskCriterion = new RiskCriterion(riskCriteriaType, Operator.EQUALS, LOW_NUMBER);
        actual = numericRiskCriterion.onRisk(highNumberPOSample);
        Assert.assertEquals(actual, false, "Sample should have been on risk due to high conc.");

        // Create an invalid risk criteria.
        try {
            new RiskCriterion(riskCriteriaType, Operator.IS, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }

        // Create an invalid risk criteria.
        try {
            new RiskCriterion(riskCriteriaType, Operator.IS_IN, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }

        // Create an invalid risk criteria.
        try {
            new RiskCriterion(riskCriteriaType, Operator.EXACT_MATCH, LOW_NUMBER);
            Assert.fail("Can't create a concentration Risk criterion with a boolean operator.");
        } catch (Exception e) {
            // npe exception expected.
        }
    }
}
