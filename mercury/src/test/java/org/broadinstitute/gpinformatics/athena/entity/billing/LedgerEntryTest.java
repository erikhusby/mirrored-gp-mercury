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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Test(groups = TestGroups.DATABASE_FREE)
public class LedgerEntryTest {

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

    public static LedgerEntry createOneLedgerEntry(String sampleName, String priceItemName,
                                                   double quantity) {
        return createOneLedgerEntry(sampleName, priceItemName, quantity, null);
    }

    public static LedgerEntry createOneLedgerEntry(String sampleName, String priceItemName, double quantity,
                                                   Date workCompleteDate) {
        return new LedgerEntry(createSample(sampleName),
                new PriceItem("quoteServerId", "platform", "category", priceItemName), workCompleteDate, quantity);
    }

    public static LedgerEntry createOneLedgerEntry(ProductOrderSample sample, String priceItemName, double quantity,
                                                   Date workCompleteDate) {
        return new LedgerEntry(sample,
                new PriceItem("quoteServerId", "platform", "category", priceItemName), workCompleteDate, quantity);
    }

    public static LedgerEntry createBilledLedgerEntry(ProductOrderSample sample) {
        LedgerEntry entry = new LedgerEntry(sample,
                new PriceItem("quoteServerId", "platform", "category", "priceItem"), new Date(), 1);
        entry.setBillingMessage(BillingSession.SUCCESS);
        return entry;
    }

    @DataProvider(name = "testIsBilled")
    public Object[][] createTestIsBilledData() {
        LedgerEntry billedEntry = createOneLedgerEntry("test", "priceItem", 1);
        BillingSession session = new BillingSession(0L, Collections.singleton(billedEntry));
        session.setBilledDate(new Date());
        return new Object[][] {
                {createBilledLedgerEntry(createSample("test")), true},
                {billedEntry, true},
                {createOneLedgerEntry("test", "price item", 1), false}
        };
    }

    @Test(dataProvider = "testIsBilled")
    public void testIsBilled(LedgerEntry entry, boolean isBilled) {
        Assert.assertEquals(entry.isBilled(), isBilled);
    }

    @DataProvider(name = "testEquals")
    public Object[][] createTestEqualsData() throws Exception {
        Date date1 = formatter.parse("12/5/12");
        Date date2 = formatter.parse("12/6/12");
        String priceItemName1 = "DNA Extract from Blood";
        String priceItemName2 = "DNA Extract from Tissue";
        ProductOrderSample sample = createSample("SM-3KBZD");
        LedgerEntry ledgerEntry1 = LedgerEntryTest.createOneLedgerEntry(sample, priceItemName1, 1, date1
        );
        ledgerEntry1.setBillingMessage("anything");

        LedgerEntry ledgerEntry2 = LedgerEntryTest.createOneLedgerEntry(sample, priceItemName1, 1, date2
        );
        ledgerEntry2.setBillingMessage("something else");

        return new Object[][] {
                // Different message, different date, should be equal.
                {ledgerEntry1, ledgerEntry2, true },
                // Different priceItem should be not equals.
                {ledgerEntry1, LedgerEntryTest.createOneLedgerEntry(sample, priceItemName2, 1, date1), false },
                // Different quantity, should equate since quantity is not used for comparison.
                {ledgerEntry1, LedgerEntryTest.createOneLedgerEntry(sample, priceItemName1, 2, date1), true },
        };
    }

    @Test(dataProvider = "testEquals")
    public void testEquals(LedgerEntry ledger1, LedgerEntry ledger2, boolean isEqual) throws Exception {
        if (isEqual) {
            Assert.assertEquals(ledger1, ledger2);
        } else {
            Assert.assertNotEquals(ledger1, ledger2);
        }
    }

    @Test
    public void testBean() {
        new BeanTester().testBean(LedgerEntry.class);
        new EqualsMethodTester().testEqualsMethod(LedgerEntry.class, "billingMessage", "quantity");
        new HashCodeMethodTester().testHashCodeMethod(LedgerEntry.class);
    }
}
