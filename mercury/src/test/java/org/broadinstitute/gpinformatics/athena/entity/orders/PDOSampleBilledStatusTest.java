package org.broadinstitute.gpinformatics.athena.entity.orders;


import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.util.Date;

import static org.hamcrest.MatcherAssert.assertThat;

@Test(groups = TestGroups.DATABASE_FREE)
public class PDOSampleBilledStatusTest {

    private PriceItem primaryPriceItem = new PriceItem("1","Genomics Platform","Super Awesome Genomic Doodad","Misc");

    private PriceItem notPrimaryPriceItem = new PriceItem("2","Burrito Making Platform","Chicken Burrito","Misc");

    private ProductOrderSample pdoSample;

    @BeforeMethod
    public void setUp() {
        pdoSample = new ProductOrderSample("A Sample");
        ProductOrder pdo = new ProductOrder();
        try {
            pdo.setProduct(new Product());
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        pdo.addSample(pdoSample);
        pdo.getProduct().setPrimaryPriceItem(primaryPriceItem);
        if(pdo.hasSapQuote()) {
            pdoSample.addLedgerItem(new Date(System.currentTimeMillis()), pdo.getProduct(), BigDecimal.valueOf(3), null);
        } else {
            pdoSample.addLedgerItem(new Date(System.currentTimeMillis()), primaryPriceItem,BigDecimal.valueOf(3));
        }

        BillingSession billingSession = new BillingSession(3L,pdoSample.getLedgerItems());
        billingSession.setBilledDate(new Date(System.currentTimeMillis()));
        pdoSample.getLedgerItems().iterator().next().setBillingSession(billingSession);
    }

    private void setBilledPriceItem(PriceItem priceItem,LedgerEntry.PriceItemType priceItemType) {
        if (pdoSample.getLedgerItems().size() != 1) {
            Assert.fail("This test is only setup to run with a single ledger item.");
        }
        LedgerEntry ledgerEntry = pdoSample.getLedgerItems().iterator().next();
        ledgerEntry.setPriceItemType(priceItemType);
        pdoSample.getLedgerItems().iterator().next().setPriceItem(priceItem);
    }

    public void testPrimaryPriceItemIsBilled() {
        // setup the billing session so its price item matches the primary price item
        setBilledPriceItem(primaryPriceItem, LedgerEntry.PriceItemType.PRIMARY_PRICE_ITEM);
        String message = MessageFormat
                .format("Sample \"{0}\" should have its primary price item billed already.", pdoSample.getName());
        assertThat(message, pdoSample.isCompletelyBilled());
    }

    public void testNonPrimaryPriceItemIsBilled() {
        // setup the billing session so that its price item is *not* the primary price item
        setBilledPriceItem(notPrimaryPriceItem, LedgerEntry.PriceItemType.ADD_ON_PRICE_ITEM);
        String message = MessageFormat
                .format("Sample \"{0}\" should not have its primary price item billed already.", pdoSample.getName());
        assertThat(message, !pdoSample.isCompletelyBilled());
    }

}
