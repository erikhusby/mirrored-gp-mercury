package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class BillingLedgerTest {

    private static final DateFormat formatter = new SimpleDateFormat("MM/dd/yy");

    private static final Map<String, ProductOrderSample> sampleMap = new HashMap<String, ProductOrderSample>();

    // Use a factory to create samples, so identical samples map to the same object.
    private static ProductOrderSample createSample(String sampleName) {
        ProductOrderSample sample = sampleMap.get(sampleName);
        if (sample == null) {
            sample = new ProductOrderSample(sampleName);
            sampleMap.put(sampleName, sample);
        }
        return sample;
    }

    public static BillingLedger createOneBillingLedger(String sampleName, String priceItemName,
                                                       double quantity) {
        return createOneBillingLedger(sampleName, priceItemName, quantity, null);
    }

    public static BillingLedger createOneBillingLedger(String sampleName, String priceItemName, double quantity,
                                                       Date workCompleteDate) {
        return new BillingLedger(createSample(sampleName),
                new PriceItem("quoteServerId", "platform", "category", priceItemName), workCompleteDate, quantity);
    }

    public static BillingLedger createOneBillingLedger(ProductOrderSample sample, String priceItemName, double quantity,
                                                       Date workCompleteDate) {
        return new BillingLedger(sample,
                new PriceItem("quoteServerId", "platform", "category", priceItemName), workCompleteDate, quantity);
    }

    @DataProvider(name = "testEquals")
    public Object[][] createTestEqualsData() throws Exception {
        Date date1 = formatter.parse("12/5/12");
        Date date2 = formatter.parse("12/6/12");
        String priceItemName1 = "DNA Extract from Blood";
        String priceItemName2 = "DNA Extract from Tissue";
        ProductOrderSample sample = createSample("SM-3KBZD");
        BillingLedger billingLedger1 = BillingLedgerTest.createOneBillingLedger(sample, priceItemName1, 1, date1
        );
        billingLedger1.setBillingMessage("anything");

        BillingLedger billingLedger2 = BillingLedgerTest.createOneBillingLedger(sample, priceItemName1, 1, date2
        );
        billingLedger2.setBillingMessage("something else");

        return new Object[][] {
                // Different message, different date, should be equal.
                { billingLedger1, billingLedger2, true },
                // Different priceItem should be not equals.
                { billingLedger1, BillingLedgerTest.createOneBillingLedger(sample, priceItemName2, 1, date1), false },
                // Different quantity, should equate since quantity is not used for comparison.
                { billingLedger1, BillingLedgerTest.createOneBillingLedger(sample, priceItemName1, 2, date1), true },
        };
    }

    @Test(dataProvider = "testEquals")
    public void testEquals(BillingLedger ledger1, BillingLedger ledger2, boolean isEqual) throws Exception {
        if (isEqual) {
            Assert.assertEquals(ledger1, ledger2);
        } else {
            Assert.assertNotEquals(ledger1, ledger2);
        }
    }

    @Test
    public void testBean() {
        new BeanTester().testBean(BillingLedger.class);
        new EqualsMethodTester().testEqualsMethod(BillingLedger.class, "billingMessage", "quantity");
        new HashCodeMethodTester().testHashCodeMethod(BillingLedger.class);
    }
}
