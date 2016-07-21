package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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

    private static final double PDO1_AMOUNT_PER_LEDGER_ITEM = 7.6;

    private static final String PDO2 = "PDO-2";

    private static final String WORK_ITEM2 = "workItem2";

    private static final double PDO2_AMOUNT_PER_LEDGER_ITEM = 9.30000001299;

    private static final double PDO2_ROUNDED_AMOUNT_PER_LEDGER_ITEM = 9.3;

    private static final String PDO3 = "PDO-3";

    private static final String WORK_ITEM3 = "workItem3";

    private static final double PDO3_AMOUNT_PER_LEDGER_ITEM = 19.000020000001;

    ProductOrder pdo1;

    ProductOrder pdo2;

    ProductOrder pdo3;

    QuoteImportItem quoteImportItem;

    @BeforeMethod
    private void setUp() {
        pdo1 = createProductOrderWithLedgerEntry(PDO1,2,PDO1_AMOUNT_PER_LEDGER_ITEM,WORK_ITEM1);
        pdo2 = createProductOrderWithLedgerEntry(PDO2,1,PDO2_AMOUNT_PER_LEDGER_ITEM,WORK_ITEM2);
        pdo3 = createProductOrderWithLedgerEntry(PDO3,1,PDO3_AMOUNT_PER_LEDGER_ITEM,WORK_ITEM3);
        quoteImportItem = new QuoteImportItem("Blah",new PriceItem(),"blah",getAllLedgerItems(pdo1,pdo2,pdo3),new Date());
    }

    private ProductOrder createProductOrderWithLedgerEntry(String pdoJiraKey,int numSamples,double amountPerLedgerEntry,String workItem) {
        ProductOrder pdo = new ProductOrder();
        pdo.setProduct(new Product());
        pdo.setJiraTicketKey(pdoJiraKey);

        for (int i = 0; i < numSamples; i++) {
            ProductOrderSample sample = new ProductOrderSample("sam" + System.currentTimeMillis() + "." + i);
            pdo.addSample(sample);
            LedgerEntry ledgerEntry = new LedgerEntry(sample,new PriceItem(),new Date(), pdo.getProduct(),amountPerLedgerEntry);
            ledgerEntry.setWorkItem(workItem);
            sample.getLedgerItems().add(ledgerEntry);
        }
        return pdo;
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
        assertThat(errorText,quoteImportItem.getNumberOfSamples(PDO1), is(pdo1.getSamples().size()));
        assertThat(errorText,quoteImportItem.getNumberOfSamples(PDO2), is(pdo2.getSamples().size()));
    }

    public void testGetWorkItems() {
        assertThat("Billing session UI is probably not showing the right work item links to the quote server.",quoteImportItem.getWorkItems(),containsInAnyOrder(new String[]{WORK_ITEM1, WORK_ITEM2,WORK_ITEM3}));
    }

    public void testGetPdoBusinessKeys() {
        assertThat("Billing session UI is probably not displaying the right PDOs.",quoteImportItem.getOrderKeys(),containsInAnyOrder(new String[] {PDO1,PDO2,PDO3}));
    }

    public void testGetChargedAmountForPdo() {
        assertThat("Per-PDO rollup of quantity is broken.",quoteImportItem.getChargedAmountForPdo(PDO1), is(Double.toString(pdo1.getSamples().size() * PDO1_AMOUNT_PER_LEDGER_ITEM)));
        assertThat("Rounding/formatting of per-PDO quantity seems to have changed.",quoteImportItem.getChargedAmountForPdo(PDO2), is(Double.toString(PDO2_ROUNDED_AMOUNT_PER_LEDGER_ITEM)));
    }

    public void testGetRoundedQuantity() {
        double totalQuantity = quoteImportItem.getQuantity();
        assertThat("Total precision is not high enough for this test.",Double.toString(totalQuantity).length(),greaterThan(10));
        assertThat("Rounding/formatting of quantity seems to have changed.",quoteImportItem.getRoundedQuantity().toString().length(), lessThan(
                5));
    }

    public void testSingleWorkItemReturnsNullWhenThereAreMultipleWorkItems() {
        try {
            assertThat(quoteImportItem.getSingleWorkItem(),is(nullValue()));
        }
        catch(RuntimeException e) {}
    }

    public void testNoWorkItems() {
        QuoteImportItem item = new QuoteImportItem(null,null,null,null,null);
        assertThat(item.getSingleWorkItem(),is(nullValue()));
    }

    public void testSingleWorkItem() {
        LedgerEntry ledgerEntry = new LedgerEntry(new ProductOrderSample("blah"),new PriceItem(),new Date(), new Product(),2);
        ledgerEntry.setWorkItem(WORK_ITEM2);
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        ledgerEntries.add(ledgerEntry);
        QuoteImportItem item = new QuoteImportItem(null,null,null,ledgerEntries,null);
        assertThat(item.getSingleWorkItem(),equalTo(WORK_ITEM2));
    }
}
