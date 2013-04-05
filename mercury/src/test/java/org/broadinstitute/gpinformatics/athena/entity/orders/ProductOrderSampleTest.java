package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.Operator;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.RiskCriterion;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductTestFactory;
import org.hamcrest.Matcher;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.InBspFormat.inBspFormat;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.NullOrEmptyString.nullOrEmptyString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleTest {

    @DataProvider(name = "testIsInBspFormat")
    public Object[][] createIsInBspFormatData() {
        return new Object[][] {
                {"SM-2ACG", inBspFormat()},
                {"SM-2ACG5", inBspFormat()},
                {"SM-2ACG6", inBspFormat()},
                {"Blahblahblah", not(inBspFormat())},
                {"12345", not(inBspFormat())},
                {"SM-ABC0", not(inBspFormat())},
                {"SM-ABCO", inBspFormat()}
        };
    }

    @Test(dataProvider = "testIsInBspFormat")
    public void testIsInBspFormat(String sampleName, Matcher<String> matcher) throws Exception {
        assertThat(sampleName, is(matcher));
    }

    static final org.broadinstitute.bsp.client.sample.MaterialType BSP_MATERIAL_TYPE =
            new org.broadinstitute.bsp.client.sample.MaterialType("Cells", "Red Blood Cells");

    static class TestPDOData {
        final Product product;
        final Product addOn;
        final ProductOrderSample sample1;
        final ProductOrderSample sample2;

        public TestPDOData(String quoteId) {
            ProductOrder order = ProductOrderTestFactory.createDummyProductOrder();

            order.setQuoteId(quoteId);

            product = order.getProduct();
            MaterialType materialType = new MaterialType(BSP_MATERIAL_TYPE.getCategory(), BSP_MATERIAL_TYPE.getName());
            addOn = ProductTestFactory.createDummyProduct("Exome Express", "partNumber");
            addOn.addAllowableMaterialType(materialType);
            addOn.setPrimaryPriceItem(new PriceItem("A", "B", "C", "D"));
            product.addAddOn(addOn);

            Map<BSPSampleSearchColumn, String> dataMap = new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                put(BSPSampleSearchColumn.MATERIAL_TYPE, BSP_MATERIAL_TYPE.getFullName());
            }};
            sample1 = new ProductOrderSample("Sample1", new BSPSampleDTO(dataMap));

            dataMap = new EnumMap<BSPSampleSearchColumn, String>(BSPSampleSearchColumn.class) {{
                put(BSPSampleSearchColumn.MATERIAL_TYPE, "XXX:XXX");
            }};
            sample2 = new ProductOrderSample("Sample2", new BSPSampleDTO(dataMap));

            List<ProductOrderSample> samples = new ArrayList<ProductOrderSample>();
            samples.add(sample1);
            samples.add(sample2);
            order.setSamples(samples);
        }
    }

    @DataProvider(name = "getBillablePriceItems")
    public static Object[][] makeGetBillablePriceItemsData() {
        TestPDOData data = new TestPDOData("GSP-123");
        Product product = data.product;
        Product addOn = data.addOn;

        List<PriceItem> expectedItems = new ArrayList<PriceItem>();
        expectedItems.add(product.getPrimaryPriceItem());
        expectedItems.add(addOn.getPrimaryPriceItem());

        return new Object[][]{
                new Object[]{data.sample1, expectedItems},
                new Object[]{data.sample2, Collections.singletonList(product.getPrimaryPriceItem())}
        };
    }

    @Test(dataProvider = "getBillablePriceItems")
    public void testGetBillablePriceItems(ProductOrderSample sample, List<PriceItem> priceItems) {
        List<PriceItem> generatedItems = sample.getBillablePriceItems();
        assertThat(generatedItems.size(), is(equalTo(priceItems.size())));

        generatedItems.removeAll(priceItems);
        assertThat(generatedItems, is(empty()));
    }

    @DataProvider(name = "autoBillSample")
    public static Object[][] makeAutoBillSampleData() {
        TestPDOData data = new TestPDOData("GSP-123");
        Date completedDate = new Date();
        Set<LedgerEntry> ledgers = new HashSet<LedgerEntry>();
        ledgers.add(new LedgerEntry(data.sample1, data.product.getPrimaryPriceItem(), completedDate, 1));
        ledgers.add(new LedgerEntry(data.sample1, data.addOn.getPrimaryPriceItem(), completedDate, 1));

        data.sample2.addLedgerItem(completedDate, data.product.getPrimaryPriceItem(), 1);
        LedgerEntry ledger = data.sample2.getLedgerItems().iterator().next();
        ledger.setBillingMessage(BillingSession.SUCCESS);
        ledger.setBillingSession(new BillingSession(0L, Collections.singleton(ledger)));

        return new Object[][]{
                // Create ledger items from a single sample.
                new Object[]{data.sample1, completedDate, ledgers},
                // Update existing ledger items with "new" bill count.
                new Object[]{data.sample1, completedDate, ledgers},
                // If sample is already billed, don't create any ledger items.
                new Object[]{data.sample2, completedDate, Collections.emptySet()}
        };
    }

    @DataProvider(name = "riskSample")
    public static Object[][] makeRiskSample() {
        TestPDOData data = new TestPDOData("GSP-123");

        // sample 1 has risk items and sample 2 does not
        RiskCriterion riskCriterion =
                new RiskCriterion(RiskCriterion.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "250.0");
        RiskItem riskItem = new RiskItem(riskCriterion, "240.0");
        riskItem.setRemark("Bad Concentration found");

        data.sample1.setRiskItems(Collections.singletonList(riskItem));
        return new Object[][]{
                new Object[]{data.sample1},
                new Object[]{data.sample2}
        };
    }

    @Test(dataProvider = "autoBillSample")
    public void testAutoBillSample(ProductOrderSample sample, Date completedDate, Set<LedgerEntry> ledgerEntries) {
        sample.autoBillSample(completedDate, 1);
        assertThat(sample.getBillableLedgerItems(), is(equalTo(ledgerEntries)));
    }

    @Test(dataProvider = "riskSample")
    public void testRisk(ProductOrderSample sample) {
        if (sample.isOnRisk()) {
            String message =
                    MessageFormat.format("Sample {0} is on risk but has no risk string.", sample.getSampleName());

            assertThat(message, sample.getRiskString(), is(not(nullOrEmptyString())));
        } else {
            String message =
                    MessageFormat.format("Sample {0} is not on risk but has a risk string.", sample.getSampleName());

            assertThat(message, sample.getRiskString(), is(nullOrEmptyString()));
        }
    }
}
