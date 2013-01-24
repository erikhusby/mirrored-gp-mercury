package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.products.*;
import org.broadinstitute.gpinformatics.athena.entity.samples.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.lang.EquivalentFactory;
import org.meanbean.test.*;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.*;

/**
 * A test.
 *
 * @author mccory
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ProductOrderSampleTest {

    @Test
    public void testIsInBspFormat() throws Exception {
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG"));
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG5"));
        Assert.assertTrue(ProductOrderSample.isInBspFormat("SM-2ACG6"));
        Assert.assertFalse(ProductOrderSample.isInBspFormat("Blahblahblah"));
        Assert.assertFalse(ProductOrderSample.isInBspFormat("12345"));

    }

    @Test
    public void testBeaniness() {
        Configuration configuration = new ConfigurationBuilder().ignoreProperty("productOrder").ignoreProperty("sampleComment")
                .ignoreProperty("bspDTO").ignoreProperty("billingStatus").ignoreProperty("deliveryStatus").build();
        new BeanTester().testBean(ProductOrderSample.class, configuration);

        class ProductOrderSampleFactory implements EquivalentFactory<ProductOrderSample> {
            @Override public ProductOrderSample create() {
                ProductOrderSample sample = new ProductOrderSample("SM-12345", BSPSampleDTO.DUMMY);
                sample.setSamplePosition(0);
                return sample;
            }
        }

        new EqualsMethodTester().testEqualsMethod(new ProductOrderSampleFactory(), configuration);
        new HashCodeMethodTester().testHashCodeMethod(new ProductOrderSampleFactory());
    }

    public static List<ProductOrderSample> createSampleList(String[] sampleArray,
                                                            Collection<BillingLedger> billableItems,
                                                            boolean dbFree) {
        List<ProductOrderSample> productOrderSamples = new ArrayList<ProductOrderSample>(sampleArray.length);
        for (String sampleName : sampleArray) {
            ProductOrderSample productOrderSample;
            if (dbFree) {
                productOrderSample = new ProductOrderSample(sampleName, BSPSampleDTO.DUMMY);
            } else {
                productOrderSample = new ProductOrderSample(sampleName);
            }
            productOrderSample.setSampleComment("athenaComment");
            productOrderSample.getLedgerItems().addAll( billableItems );
            productOrderSamples.add(productOrderSample);
        }
        return productOrderSamples;
    }

    static final org.broadinstitute.bsp.client.sample.MaterialType BSP_MATERIAL_TYPE =
            new org.broadinstitute.bsp.client.sample.MaterialType("Cells", "Red Blood Cells");

    static class TestPDOData {
        final Product product;
        final Product addOn;
        final ProductOrderSample sample1;
        final ProductOrderSample sample2;

        public TestPDOData() {
            ProductOrder order = AthenaClientServiceStub.createDummyProductOrder();
            product = order.getProduct();
            MaterialType materialType = new MaterialType(BSP_MATERIAL_TYPE.getCategory(), BSP_MATERIAL_TYPE.getName());
            addOn = AthenaClientServiceStub.createDummyProduct();
            addOn.addAllowableMaterialType(materialType);
            addOn.setPrimaryPriceItem(new PriceItem("A", "B", "C", "D"));
            product.addAddOn(addOn);
            sample1 = new ProductOrderSample("Sample1",
                    new BSPSampleDTO("", "", "", "", "", "", "", "", "", "", "", "", BSP_MATERIAL_TYPE.getFullName(), "", "", "", "", "", "",
                            ""));
            sample2 = new ProductOrderSample("Sample2",
                    new BSPSampleDTO("", "", "", "", "", "", "", "", "", "", "", "", "XXX:XXX", "", "", "", "", "", "", ""));
            order.setSamples(Collections.singletonList(sample1));
            List<ProductOrderSample> samples = new ArrayList<ProductOrderSample>();
            samples.add(sample1);
            samples.add(sample2);
            order.setSamples(samples);
        }
    }

    @DataProvider(name = "getBillablePriceItems")
    public static Object[][] makeGetBillablePriceItemsData() {
        TestPDOData data = new TestPDOData();
        Product product = data.product;
        Product addOn = data.addOn;

        List<PriceItem> expectedItems = new ArrayList<PriceItem>();
        expectedItems.add(product.getPrimaryPriceItem());
        expectedItems.add(addOn.getPrimaryPriceItem());

        return new Object[][] {
                new Object[] { data.sample1, expectedItems },
                new Object[] { data.sample2, Collections.singletonList(product.getPrimaryPriceItem()) }
        };
    }

    @Test(dataProvider = "getBillablePriceItems")
    public void testGetBillablePriceItems(ProductOrderSample sample, List<PriceItem> priceItems) {
        List<PriceItem> generatedItems = sample.getBillablePriceItems();
        Assert.assertEquals(generatedItems.size(), priceItems.size());
        generatedItems.removeAll(priceItems);
        Assert.assertTrue(generatedItems.isEmpty());
    }

    @DataProvider(name = "autoBillSample")
    public static Object[][] makeAutoBillSampleData() {
        TestPDOData data = new TestPDOData();
        Date completedDate = new Date();
        Set<BillingLedger> ledgers = new HashSet<BillingLedger>();
        ledgers.add(new BillingLedger(data.sample1, data.product.getPrimaryPriceItem(),  completedDate, 1));
        ledgers.add(new BillingLedger(data.sample1, data.addOn.getPrimaryPriceItem(),  completedDate, 1));

        data.sample2.addLedgerItem(completedDate, data.product.getPrimaryPriceItem(), 1);
        BillingLedger ledger = data.sample2.getLedgerItems().iterator().next();
        ledger.setBillingMessage(BillingSession.SUCCESS);
        ledger.setBillingSession(new BillingSession(0L, Collections.singleton(ledger)));

        return new Object[][] {
                new Object[] { data.sample1, completedDate, ledgers, BillingStatus.EligibleForBilling },
                new Object[] { data.sample1, completedDate, ledgers, BillingStatus.EligibleForBilling },
                new Object[] { data.sample2, completedDate, Collections.emptySet(), BillingStatus.EligibleForBilling }
        };
    }

    @DataProvider(name = "riskSample")
    public static Object[][] makeRiskSample() {
        TestPDOData data = new TestPDOData();

        // sample 1 has risk items and sample 2 does not
        RiskCriteria riskCriteria = new RiskCriteria(RiskCriteria.RiskCriteriaType.CONCENTRATION, Operator.LESS_THAN, "250.0");
        RiskItem riskItem = new RiskItem(riskCriteria, new Date(), "240.0");
        riskItem.setRemark("Bad Concentration found");

        data.sample1.setRiskItems(Collections.singletonList(riskItem));
        return new Object[][] {
            new Object[] { data.sample1 },
            new Object[] { data.sample2 }
        };
    }

    @Test(dataProvider = "autoBillSample")
    public void testAutoBillSample(ProductOrderSample sample, Date completedDate, Set<BillingLedger> billingLedgers, BillingStatus billingStatus) {
        sample.autoBillSample(completedDate, 1);
        Assert.assertEquals(sample.getBillableLedgerItems(), billingLedgers);
    }

    @Test(dataProvider = "riskSample")
    public void testRisk(ProductOrderSample sample) {
        if (sample.isOnRisk()) {
            Assert.assertTrue(!sample.getRiskString().isEmpty(), "Sample: " + sample.getSampleName() +
                    " is on risk but has no risk string");
        } else {
            Assert.assertTrue(sample.getRiskString().isEmpty(), "Sample: " + sample.getSampleName() +
                    " is not on risk but has a risk string: " + sample.getRiskString());
        }
    }
}
