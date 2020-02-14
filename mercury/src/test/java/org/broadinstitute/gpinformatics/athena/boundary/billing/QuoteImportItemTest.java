package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.DATABASE_FREE, enabled = true)
public class QuoteImportItemTest {

    private static final String PDO1 = "PDO-1";

    private static final String WORK_ITEM1 = "workItem1";

    private static final BigDecimal PDO1_AMOUNT_PER_LEDGER_ITEM = new BigDecimal("7.6");

    private static final String PDO2 = "PDO-2";

    private static final String WORK_ITEM2 = "workItem2";

    private static final BigDecimal PDO2_AMOUNT_PER_LEDGER_ITEM = new BigDecimal("9.30000001299");

    private static final BigDecimal PDO2_ROUNDED_AMOUNT_PER_LEDGER_ITEM = BigDecimal.valueOf(9.3);

    private static final String PDO3 = "PDO-3";

    private static final BigDecimal PDO3_AMOUNT_PER_LEDGER_ITEM = new BigDecimal("19.000020000001");

    private static final String WORK_ITEM3 = "workItem3";

    ProductOrder pdo1;

    ProductOrder pdo2;

    ProductOrder pdo3;

    QuoteImportItem quoteImportItem;

    @BeforeMethod
    private void setUp() {
        pdo1 = createProductOrderWithLedgerEntry(PDO1,2,PDO1_AMOUNT_PER_LEDGER_ITEM,WORK_ITEM1);
        updateProductOrderWithLedgerEntry(pdo1, 1, PDO2_AMOUNT_PER_LEDGER_ITEM, WORK_ITEM2, pdo1.getProduct().getPrimaryPriceItem());
        updateProductOrderWithLedgerEntry(pdo1, 1, PDO3_AMOUNT_PER_LEDGER_ITEM, WORK_ITEM3, pdo1.getProduct().getPrimaryPriceItem());
        pdo2 = createProductOrderWithLedgerEntry(PDO2,1,PDO2_AMOUNT_PER_LEDGER_ITEM,WORK_ITEM2);
        pdo3 = createProductOrderWithLedgerEntry(PDO3,1,PDO3_AMOUNT_PER_LEDGER_ITEM,WORK_ITEM3);

        quoteImportItem = new QuoteImportItem("Blah",new PriceItem(),"blah",getAllLedgerItems(pdo1),new Date(),
                pdo1.getProduct(), pdo1);
    }

    private ProductOrder createProductOrderWithLedgerEntry(String pdoJiraKey, int numSamples,
                                                           BigDecimal amountPerLedgerEntry, String workItem) {
        ProductOrder pdo = new ProductOrder();
        PriceItem priceItem = new PriceItem();
        Product product = new Product();
        product.setPrimaryPriceItem(priceItem);
        try {
            pdo.setProduct(product);
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        pdo.setJiraTicketKey(pdoJiraKey);
        updateProductOrderWithLedgerEntry(pdo, numSamples, amountPerLedgerEntry, workItem, priceItem);
        return pdo;
    }

    private void updateProductOrderWithLedgerEntry(ProductOrder pdo, int numSamples, BigDecimal amountPerLedgerEntry,
                                                   String workItem, PriceItem priceItem) {
        for (int i = 0; i < numSamples; i++) {
            ProductOrderSample sample = new ProductOrderSample("sam" + System.currentTimeMillis() + "." + i + pdo.getSamples().size());
            pdo.addSample(sample);
            LedgerEntry ledgerEntry;
            if(pdo.hasSapQuote()) {
                ledgerEntry = new LedgerEntry(sample, pdo.getProduct(), new Date(), amountPerLedgerEntry);
            } else {
                ledgerEntry = new LedgerEntry(sample, priceItem, new Date(), amountPerLedgerEntry);
            }
            ledgerEntry.setWorkItem(workItem);
            sample.getLedgerItems().add(ledgerEntry);
        }

    }

    private List<LedgerEntry> getAllLedgerItems(ProductOrder... pdos) {
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        for (ProductOrder pdo : pdos) {
            for (ProductOrderSample sam : pdo.getSamples()) {
                ledgerEntries.addAll(sam.getLedgerItems());
            }
        }
        return ledgerEntries;
    }

    public void testGetNumberOfSamples() {
        String errorText = "Billing session UI is probably not showing the right number of samples in the billing transaction";
        assertThat(errorText, quoteImportItem.getNumberOfSamples(PDO1), is(pdo1.getSamples().size()));
    }

    public void testGetWorkItems() {
        assertThat("Billing session UI is probably not showing the right work item links to the quote server.",
                quoteImportItem.getWorkItems(),containsInAnyOrder(WORK_ITEM1,WORK_ITEM2,WORK_ITEM3));
    }

    public void testGetPdoBusinessKeys() {
        assertThat("Billing session UI is probably not displaying the right PDOs.",quoteImportItem.getOrderKeys(),containsInAnyOrder(
                PDO1));
    }

    public void testGetChargedAmountForPdo() {
        assertThat("Per-PDO rollup of quantity is broken.",quoteImportItem.getChargedAmountForPdo(PDO1),
                is(
                        new DecimalFormat(QuoteImportItem.PDO_QUANTITY_FORMAT)
                                .format(BigDecimal.valueOf(2).multiply( PDO1_AMOUNT_PER_LEDGER_ITEM)
                                        .add(BigDecimal.valueOf(1).multiply( PDO2_AMOUNT_PER_LEDGER_ITEM))
                                        .add(BigDecimal.valueOf(1).multiply(PDO3_AMOUNT_PER_LEDGER_ITEM))
                                )
                )
        );
    }

    public void testGetRoundedQuantity() {
        BigDecimal totalQuantity = quoteImportItem.getQuantity();
        assertThat("Total precision is not high enough for this test.",totalQuantity.toString().length(),greaterThan(7));
        assertThat("Rounding/formatting of quantity seems to have changed.",quoteImportItem.getRoundedQuantity().length(), lessThan(
                5));
    }

    /*
    Revisit how to test this
     */
    @Test()
    public void testSingleWorkItemReturnsNullWhenThereAreMultipleWorkItems() {
        try {
            assertThat(quoteImportItem.getSingleWorkItem(),is(nullValue()));
        }
        catch(RuntimeException ignored) {}
    }

    public void testNoWorkItems() {
        QuoteImportItem item = new QuoteImportItem(null,null,null,null,null, new Product(), new ProductOrder());
        assertThat(item.getSingleWorkItem(),is(nullValue()));
    }

    public void testSingleWorkItem() {
        ProductOrderSample blah = new ProductOrderSample("blah");
        ProductOrder pdo = new ProductOrder();
        Product product = new Product();
        try {
            pdo.setProduct(product);
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        pdo.addSample(blah);
        PriceItem priceItem = new PriceItem();
        product.setPrimaryPriceItem(priceItem);
        LedgerEntry ledgerEntry;
        if(pdo.hasSapQuote()) {
            ledgerEntry = new LedgerEntry(blah, product, new Date(), BigDecimal.valueOf(2));
        } else {
            ledgerEntry = new LedgerEntry(blah, priceItem, new Date(), BigDecimal.valueOf(2));
        }
        ledgerEntry.setWorkItem(WORK_ITEM2);
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        ledgerEntries.add(ledgerEntry);
        QuoteImportItem item = new QuoteImportItem(null,null,null,ledgerEntries,null, blah.getProductOrder().getProduct(), blah.getProductOrder());
        assertThat(item.getSingleWorkItem(),equalTo(WORK_ITEM2));
    }
}
