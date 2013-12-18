package org.broadinstitute.gpinformatics.athena.presentation.orders;


import net.sourceforge.stripes.action.ActionBeanContext;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ProductOrderActionBeanTest {

    private ProductOrderActionBean actionBean;

    private JSONObject jsonObject;

    private double expectedNumericValue = 9.3;

    private String expectedNonNumericRinScore = "I'm not a number";

    private ProductOrder pdo;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    private void setUp() {
        actionBean = new ProductOrderActionBean();
        actionBean.setContext(new CoreActionBeanContext());
        jsonObject = new JSONObject();
        pdo = newPdo();
    }

    /**
     * Creates a basic PDO with a few samples.
     * Setting the product is left to individual tests.
     * @return
     */
    private ProductOrder newPdo() {
        ProductOrder pdo = new ProductOrder();
        pdo.setSamples(createPdoSamples());
        return pdo;
    }

    /**
     * Creates a list with two samples: one with a good
     * rin score and one with a bad rin score
     * @return
     */
    private Collection<ProductOrderSample> createPdoSamples() {
        List<ProductOrderSample> pdoSamples = new ArrayList();
        BSPSampleDTO sampleWithGoodRin = getSampleDTOWithGoodRinScore();
        BSPSampleDTO sampleWithBadRin = getSamplDTOWithBadRinScore();
        pdoSamples.add(new ProductOrderSample(sampleWithGoodRin.getSampleId(),sampleWithGoodRin));
        pdoSamples.add(new ProductOrderSample(sampleWithBadRin.getSampleId(),sampleWithBadRin));
        pdoSamples.add(new ProductOrderSample("123.0")); // throw in a gssr sample
        return pdoSamples;
    }

    /**
     * Sets the product for given PDO such that
     * there's rin risk.
     */
    private void setRinRiskProduct(ProductOrder pdo) {
        Product productThatHasRinRisk = new Product();
        productThatHasRinRisk.addRiskCriteria(
                new RiskCriterion(RiskCriterion.RiskCriteriaType.RIN, Operator.LESS_THAN,"6.0")
        );
        pdo.setProduct(productThatHasRinRisk);
    }

    private void setNonRinRiskProduct(ProductOrder pdo) {
        pdo.setProduct(new Product());
    }

    /**
     * Tests that non-numeric RIN scores
     * are turned into "N/A" by the action bean
     * @throws JSONException
     */
    @Test(groups = TestGroups.DATABASE_FREE)
    public void testNonNumericRinScore() throws JSONException {
        actionBean.putRinScore(jsonObject,getSamplDTOWithBadRinScore());
        Assert.assertEquals(jsonObject.get(ProductOrderActionBean.JSON_RIN_KEY),expectedNonNumericRinScore);
    }

    /**
     * Tests that numeric RIN scores
     * are handled as real numbers by the action bean
     * @throws JSONException
     */
    @Test(groups = TestGroups.DATABASE_FREE)
    public void testNumericRinScore() throws JSONException {
        actionBean.putRinScore(jsonObject,getSampleDTOWithGoodRinScore());
        Assert.assertEquals(Double.parseDouble((String)jsonObject.get(ProductOrderActionBean.JSON_RIN_KEY)),expectedNumericValue);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testValidateRinScoresWhenProductHasRinRisk() {
        setRinRiskProduct(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        actionBean.validateRinScores(pdo);
        Assert.assertEquals(actionBean.getContext().getValidationErrors().size(),1);
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testValidateRinScoresWhenProductHasNoRinRisk() {
        setNonRinRiskProduct(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
        actionBean.validateRinScores(pdo);
        Assert.assertTrue(actionBean.getContext().getValidationErrors().isEmpty());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testIsRinScoreValidationRequired() {
        setRinRiskProduct(pdo);
        Assert.assertTrue(actionBean.isRinScoreValidationRequired(pdo));
        setNonRinRiskProduct(pdo);
        Assert.assertFalse(actionBean.isRinScoreValidationRequired(pdo));
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testCanBadRinScoreBeUsedForOnRiskCalculation() {
        BSPSampleDTO badRinScoreSample = getSamplDTOWithBadRinScore();
        Assert.assertFalse(actionBean.canRinScoreBeUsedForOnRiskCalculation(badRinScoreSample));
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testCanGoodRinScoreBeUsedForOnRiskCalculation() {
        BSPSampleDTO goodRinScoreSample = getSampleDTOWithGoodRinScore();
        Assert.assertTrue(actionBean.canRinScoreBeUsedForOnRiskCalculation(goodRinScoreSample));
    }

    private BSPSampleDTO getSamplDTOWithBadRinScore() {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.RIN, expectedNonNumericRinScore);
            put(BSPSampleSearchColumn.SAMPLE_ID,"SM-49M5N");
        }};
        return new BSPSampleDTO(dataMap);
    }

    private BSPSampleDTO getSampleDTOWithGoodRinScore() {
        Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
            put(BSPSampleSearchColumn.RIN, String.valueOf(expectedNumericValue));
            put(BSPSampleSearchColumn.SAMPLE_ID,"SM-99D2A");
        }};
        return new BSPSampleDTO(dataMap);
    }
}
