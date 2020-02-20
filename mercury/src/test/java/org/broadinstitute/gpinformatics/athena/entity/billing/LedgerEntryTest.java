package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jetbrains.annotations.NotNull;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@Test(groups = TestGroups.DATABASE_FREE)
public class LedgerEntryTest {

    private static final FastDateFormat formatter = FastDateFormat.getInstance("MM/dd/yy");

    private static final Map<String, ProductOrderSample> sampleMap = new HashMap<>();

    // Use a factory to create samples, so identical samples map to the same object.
    private static ProductOrderSample createSample(String sampleName) {
        ProductOrderSample sample = sampleMap.get(sampleName);
        if (sample == null) {
            sample = new ProductOrderSample(sampleName);
            sample.setProductOrder(new ProductOrder());
            sampleMap.put(sampleName, sample);
        }
        return sample;
    }

    public void testCreditCalculation(){
        LedgerEntry ledger1 = createOneLedgerEntry("SM1", null, BigDecimal.TEN, null);
        BillingSession billingSession = new BillingSession();
        billingSession.setBilledDate(new Date());
        ledger1.setBillingSession(billingSession);
        ledger1.setBillingMessage(BillingSession.SUCCESS);
        LedgerEntry ledger2 = createOneLedgerEntry("SM1", null, BigDecimal.ONE.negate(), null);
        ledger2.setBillingMessage(BillingSession.SUCCESS);
        ledger2.setBillingSession(billingSession);
        ledger1.addCredit(ledger2, BigDecimal.ONE);
        assertThat(ledger1.calculateAvailableQuantity(), is(BigDecimal.valueOf(9)));

        ledger1.addCredit(createOneLedgerEntry("SM1", null, BigDecimal.ONE.negate(), null), BigDecimal.ONE);
        assertThat(ledger1.calculateAvailableQuantity(), is(BigDecimal.valueOf(8)));
        assertThat(ledger1.getQuantityCredited(), is(BigDecimal.valueOf(2)));
    }

    public void testCreditIsNegative() {
        LedgerEntry ledger1 = createOneLedgerEntry("SM1", null, BigDecimal.TEN, null);
        try {
            ledger1.addCredit(createOneLedgerEntry("SM1", null, BigDecimal.ONE, null), BigDecimal.ONE);
            Assert.fail("An Exception should have been thrown when a credit is a positive number");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Billing credits must have negative numbers"));
        }
    }

    public static LedgerEntry createOneLedgerEntry(String sampleName, String priceItemName,
                                                   BigDecimal quantity) {
        return createOneLedgerEntry(sampleName, priceItemName, quantity, null);
    }

    public static LedgerEntry createOneLedgerEntry(String sampleName, String priceItemName, BigDecimal quantity,
                                                   Date workCompleteDate) {
        ProductOrderSample sample = createSample(sampleName);
        return createOneLedgerEntry(sample, priceItemName, quantity, workCompleteDate);
    }

    public static LedgerEntry createOneLedgerEntry(ProductOrderSample sample, String priceItemName, BigDecimal quantity,
                                                   Date workCompleteDate) {
        Product product = new Product();
        return createOneLedgerEntry(sample, priceItemName, quantity, workCompleteDate, product);
    }

    @NotNull
    public static LedgerEntry createOneLedgerEntry(ProductOrderSample sample, String priceItemName, BigDecimal quantity,
                                                   Date workCompleteDate, Product product) {
        PriceItem priceItem = new PriceItem("quoteServerId", "platform", "category", priceItemName);
        return createOneLedgerEntry(sample, quantity, workCompleteDate, product, priceItem);
    }

    @NotNull
    public static LedgerEntry createOneLedgerEntry(ProductOrderSample sample, BigDecimal quantity, Date workCompleteDate,
                                                   Product product, PriceItem priceItem) {
        LedgerEntry ledgerEntry;
        if(priceItem == null) {
            sample.getProductOrder().setQuoteId("2700039");
            ledgerEntry = new LedgerEntry(sample, product, workCompleteDate, quantity);
        } else {
            ledgerEntry = new LedgerEntry(sample, priceItem, workCompleteDate, quantity);
        }
        return ledgerEntry;
    }

    public static LedgerEntry createBilledLedgerEntry(ProductOrderSample sample,
                                                      LedgerEntry.PriceItemType priceItemType, boolean forSap) {
        LedgerEntry entry;
        if(forSap) {
            entry = createOneLedgerEntry(sample, null, BigDecimal.ONE, new Date(), new Product());
        } else {
            entry = createOneLedgerEntry(sample, "priceItem", BigDecimal.ONE, new Date(), new Product());
        }
        entry.setPriceItemType(priceItemType);
        entry.setBillingMessage(BillingSession.SUCCESS);
        return entry;
    }

    @DataProvider(name = "testIsBilled")
    public Object[][] createTestIsBilledData() {
        LedgerEntry billedEntry = createOneLedgerEntry("test", "priceItem", BigDecimal.ONE);
        BillingSession session = new BillingSession(0L, Collections.singleton(billedEntry));
        session.setBilledDate(new Date());
        return new Object[][] {
                {createBilledLedgerEntry(createSample("test"), LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM,false ), true},
                {createBilledLedgerEntry(createSample("test"), LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM, false), true},
                {billedEntry, true},
                {createOneLedgerEntry("test", "price item", BigDecimal.ONE), false}
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

        LedgerEntry ledgerEntry1 = LedgerEntryTest.createOneLedgerEntry(sample, priceItemName1, BigDecimal.ONE, date1
        );
        ledgerEntry1.setBillingMessage("anything");

        LedgerEntry ledgerEntry2 = LedgerEntryTest.createOneLedgerEntry(sample, priceItemName1, BigDecimal.ONE, date2
        );
        ledgerEntry2.setBillingMessage("something else");

        LedgerEntry ledgerEntrySap1 = LedgerEntryTest.createOneLedgerEntry(sample, BigDecimal.ONE, date1, new Product(), null);
        ledgerEntrySap1.setBillingMessage("anything");

        LedgerEntry ledgerEntrySap2 = LedgerEntryTest.createOneLedgerEntry(sample, BigDecimal.ONE, date2,new Product(), null);
        ledgerEntrySap2.setBillingMessage("something else");

        return new Object[][] {
                // Different message, different date, should be equal.
                {ledgerEntry1, ledgerEntry2, true },
                {ledgerEntrySap1, ledgerEntrySap1, true },
                {ledgerEntrySap1, ledgerEntry2, false},
                {ledgerEntry1, ledgerEntrySap2, false},
                {ledgerEntry1, ledgerEntrySap1, false},
                {ledgerEntry2, ledgerEntrySap2, false},
                // Different priceItem should be not equals.
                {ledgerEntry1, LedgerEntryTest.createOneLedgerEntry(sample, priceItemName2, BigDecimal.ONE, date1), false },
                // Different quantity, should equate since quantity is not used for comparison.
                {ledgerEntry1, LedgerEntryTest.createOneLedgerEntry(sample, priceItemName1, BigDecimal.valueOf(2), date1), true },
                {ledgerEntry1, LedgerEntryTest.createOneLedgerEntry(sample, BigDecimal.valueOf(2), date1, new Product(), null), false },
                {ledgerEntrySap1, LedgerEntryTest.createOneLedgerEntry(sample, priceItemName2, BigDecimal.valueOf(1), date1), false },
                {ledgerEntrySap1, LedgerEntryTest.createOneLedgerEntry(sample, priceItemName1, BigDecimal.valueOf(2), date1), false },
                {ledgerEntrySap1, LedgerEntryTest.createOneLedgerEntry(sample, BigDecimal.valueOf(2), date1, new Product(), null), true}
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
        new EqualsMethodTester().testEqualsMethod(LedgerEntry.class, "autoLedgerTimestamp", "billingMessage", "quantity", "workItem", "workCompleteDate", "sapDeliveryDocumentId", "sapReturnOrderId", "sapOrderDetail", "sapReplacementPricing", "creditItems");
        new HashCodeMethodTester().testHashCodeMethod(LedgerEntry.class);
    }
}
