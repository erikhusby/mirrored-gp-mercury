package org.broadinstitute.gpinformatics.athena.boundary.billing;


import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Test(groups = TestGroups.DATABASE_FREE, enabled = true)
public class BillingResultTest {

    private static final String WORK_ITEM = "blah";

    private QuoteImportItem quoteImportItem;

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    private void setUp() {
        quoteImportItem = createQuoteImportItem(2);
    }

    private QuoteImportItem createQuoteImportItem(int numEntries) {
        List<LedgerEntry> ledgerEntries = new ArrayList<>();
        for (int i = 0; i < numEntries; i++) {
            ProductOrderSample pdoSample = new ProductOrderSample("SM-123" + i);
            LedgerEntry ledgerEntry = new LedgerEntry(pdoSample,new PriceItem(),new Date(),3);
            ledgerEntries.add(ledgerEntry);
        }
        return new QuoteImportItem(null,null,null,ledgerEntries,null);
    }

    @Test
    public void testWorkItemIsPassedThroughToLedgerItems() {
        quoteImportItem.updateLedgerEntries(new QuotePriceItem(),"something",WORK_ITEM, new ArrayList<String>());
        Assert.assertFalse(quoteImportItem.getLedgerItems().isEmpty(),"No ledger items were included in this test.  Who knows if the work items were saved?");
        for (LedgerEntry ledgerEntry : quoteImportItem.getLedgerItems()) {
            Assert.assertEquals(ledgerEntry.getWorkItem(),WORK_ITEM,"Work item from the quote server was not propagated to ledger entries.  The ability to compare quote server data with mercury may be broken.");
        }
    }

    // TODO: fill in tests for getPriceItemType() here!
}
