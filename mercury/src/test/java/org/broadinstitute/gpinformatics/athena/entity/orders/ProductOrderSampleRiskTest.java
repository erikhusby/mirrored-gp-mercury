package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.athena.entity.orders.RiskItemCriteriaTypeMatcher.forCriteriaType;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;

@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleRiskTest {

    private static final String RIN_MINIMUM = "6";
    private static final String FAILING_RIN = "1";
    private static final String PASSING_RIN = "8";

    private static final String RQS_MINIMUM = "5.5";
    private static final String FAILING_RQS = "1";
    private static final String PASSING_RQS = "7";

    public static final boolean RIN_RISK_EXPECTED = true;
    public static final boolean RIN_RISK_NOT_EXPECTED = false;
    public static final boolean RQS_RISK_EXPECTED = true;
    public static final boolean RQS_RISK_NOT_EXPECTED = false;

    private Product product;
    private Map<BSPSampleSearchColumn, String> bspData;
    private ProductOrderSample productOrderSample;

    @BeforeMethod
    public void setUp() throws Exception {
        product = ProductTestFactory.createTestProduct();
        ProductOrder productOrder = new ProductOrder();
        productOrder.setProduct(product);

        bspData = new HashMap<>();
        bspData.put(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234");
        SampleData bspSampleDto = new BSPSampleDTO(bspData);
        productOrderSample = new ProductOrderSample("SM-1234", bspSampleDto);

        productOrder.addSample(productOrderSample);
    }

    @Test
    public void testSampleOnRisk() {
        RiskCriterion riskCriterion =
                new RiskCriterion(RiskCriterion.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "250.0");
        product.addRiskCriteria(riskCriterion);
        bspData.put(BSPSampleSearchColumn.CONCENTRATION, "240.0");
        assertThat(productOrderSample.calculateRisk(), is(true));
        assertThat(productOrderSample.getRiskString(), containsString(riskCriterion.getCalculationString()));
    }

    @Test
    public void testSampleNotOnRisk() {
        RiskCriterion riskCriterion =
                new RiskCriterion(RiskCriterion.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "250.0");
        product.addRiskCriteria(riskCriterion);
        bspData.put(BSPSampleSearchColumn.CONCENTRATION, "251.0");
        assertThat(productOrderSample.calculateRisk(), is(false));
        assertThat(productOrderSample.getRiskString(), equalTo(""));
    }

    @DataProvider
    public Object[][] riskForRinAndRqsTestCases() {
        // @formatter:off
        return new Object[][]{
                new Object[] {"ON RISK with no RIN and no RQS",                 null,           null,           RIN_RISK_EXPECTED,      RQS_RISK_EXPECTED},
                new Object[] {"ON RISK with failing RIN and no RQS",            FAILING_RIN,    null,           RIN_RISK_EXPECTED,      RQS_RISK_NOT_EXPECTED},
                new Object[] {"ON RISK with no RIN and failing RQS",            null,           FAILING_RQS,    RIN_RISK_NOT_EXPECTED,  RQS_RISK_EXPECTED},
                new Object[] {"NOT ON RISK with passing RIN and no RQS",        PASSING_RIN,    null,           RIN_RISK_NOT_EXPECTED,  RQS_RISK_NOT_EXPECTED},
                new Object[] {"NOT ON RISK with no RIN and passing RQS",        null,           PASSING_RQS,    RIN_RISK_NOT_EXPECTED,  RQS_RISK_NOT_EXPECTED},
                new Object[] {"ON RISK with failing RIN and failing RQS",       FAILING_RIN,    FAILING_RQS,    RIN_RISK_EXPECTED,      RQS_RISK_EXPECTED},
                new Object[] {"ON RISK with failing RIN and passing RQS",       FAILING_RIN,    PASSING_RQS,    RIN_RISK_EXPECTED,      RQS_RISK_NOT_EXPECTED},
                new Object[] {"ON RISK with passing RIN_and failing RQS",       PASSING_RIN,    FAILING_RQS,    RIN_RISK_NOT_EXPECTED,  RQS_RISK_EXPECTED},
                new Object[] {"NOT ON RISK with passing RIN and passing RQS",   PASSING_RIN,    PASSING_RQS,    RIN_RISK_NOT_EXPECTED,  RQS_RISK_NOT_EXPECTED},
        };
        // @formatter:on
    }

    @Test(dataProvider = "riskForRinAndRqsTestCases")
    public void testRiskForRinAndRqs(String testCase, String rin, String rqs,
                                     boolean expectRinRiskItem, boolean expectRqsRiskItem) {

        assertThat("Test case must be used to describe the test scenario.", testCase, not(isEmptyString()));

        product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.RIN, Operator.LESS_THAN, RIN_MINIMUM));
        product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.RQS, Operator.LESS_THAN, RQS_MINIMUM));

        bspData.put(BSPSampleSearchColumn.RIN, rin);
        bspData.put(BSPSampleSearchColumn.RQS, rqs);

        assertThat(productOrderSample.calculateRisk(), is(expectRinRiskItem || expectRqsRiskItem));

        Collection<RiskItem> riskItems = productOrderSample.getRiskItems();
        if (expectRinRiskItem) {
            assertThat(String.format("\"%s\": Expected RIN risk item", testCase),
                    riskItems, hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RIN)));
        } else {
            assertThat(String.format("\"%s\": Expected no RIN risk item", testCase),
                    riskItems, not(hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RIN))));
        }
        if (expectRqsRiskItem) {
            assertThat(String.format("\"%s\": Expected RQS risk item", testCase),
                    riskItems, hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RQS)));
        } else {
            assertThat(String.format("\"%s\": Expected no RQS risk item", testCase),
                    riskItems, not(hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RQS))));
        }
    }

    @Test
    public void testOnRiskWithMissingRin() {
        product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.RIN, Operator.LESS_THAN, RIN_MINIMUM));

        assertThat(productOrderSample.calculateRisk(), is(true));
        assertThat(productOrderSample.getRiskItems(), hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RIN)));
    }

    @Test
    public void testOnRiskWithLowRin() {
        product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.RIN, Operator.LESS_THAN, RIN_MINIMUM));
        bspData.put(BSPSampleSearchColumn.RIN, FAILING_RIN);

        assertThat(productOrderSample.calculateRisk(), is(true));
        assertThat(productOrderSample.getRiskItems(), hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RIN)));
    }

    @Test
    public void testNotOnRiskWithHighRin() {
        product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.RIN, Operator.LESS_THAN, RIN_MINIMUM));
        bspData.put(BSPSampleSearchColumn.RIN, PASSING_RIN);

        assertThat(productOrderSample.calculateRisk(), is(false));
        assertThat(productOrderSample.getRiskItems(),
                not(hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RIN))));
    }

    @Test
    public void testOnRiskWithMissingRqs() {
        product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.RQS, Operator.LESS_THAN, RQS_MINIMUM));

        assertThat(productOrderSample.calculateRisk(), is(true));
        assertThat(productOrderSample.getRiskItems(), hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RQS)));
    }

    @Test
    public void testOnRiskWithLowRqs() {
        product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.RQS, Operator.LESS_THAN, RQS_MINIMUM));
        bspData.put(BSPSampleSearchColumn.RQS, FAILING_RQS);

        assertThat(productOrderSample.calculateRisk(), is(true));
        assertThat(productOrderSample.getRiskItems(), hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RQS)));
    }

    @Test
    public void testNotOnRiskWithHighRqs() {
        product.addRiskCriteria(new RiskCriterion(RiskCriterion.RiskCriteriaType.RQS, Operator.LESS_THAN, RQS_MINIMUM));
        bspData.put(BSPSampleSearchColumn.RQS, PASSING_RQS);

        assertThat(productOrderSample.calculateRisk(), is(false));
        assertThat(productOrderSample.getRiskItems(),
                not(hasItem(forCriteriaType(RiskCriterion.RiskCriteriaType.RQS))));
    }
}
