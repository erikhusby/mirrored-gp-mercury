package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.broadinstitute.gpinformatics.athena.entity.orders.BillingStatus;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.meanbean.test.BeanTester;
import org.meanbean.test.EqualsMethodTester;
import org.meanbean.test.HashCodeMethodTester;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 12/7/12
 * Time: 12:51 PM
 */
public class BillingLedgerTest {


    private DateFormat formatter = new SimpleDateFormat("MM/dd/yy");

    public static BillingLedger createOneBillingLedger(String sampleName, String priceItemName, double quantity ) {
        return createOneBillingLedger( sampleName, priceItemName, quantity, null );
    }

    public static BillingLedger createOneBillingLedger(String sampleName, String priceItemName, double quantity, Date workCompleteDate ) {

        BillingLedger billingLedger = new BillingLedger( new ProductOrderSample(sampleName),
                new PriceItem("quoteServerId", "platform", "category", priceItemName), workCompleteDate, quantity );
        billingLedger.getProductOrderSample().setBillingStatus(BillingStatus.EligibleForBilling);
        return billingLedger;
    }

    @Test
    public void testEquals() throws Exception {

        final Date date1 = formatter.parse("12/5/12");
        final Date date2 = formatter.parse("12/6/12");
        final String priceItemName1 = "DNA Extract from Blood";
        final String priceItemName2 = "DNA Extract from Tissue";

        BillingLedger billingLedger1 = BillingLedgerTest.createOneBillingLedger("SM-3KBZD",
                priceItemName1, 1, date1);
        billingLedger1.setBillingMessage("anything");

        // Create a new ledger Item with different message, different date
        BillingLedger billingLedger2 = BillingLedgerTest.createOneBillingLedger("SM-3KBZD",
                priceItemName1, 1, date2);
        billingLedger2.setBillingMessage( "something else");
        Assert.assertEquals(billingLedger1, billingLedger2 );

        // changing the priceItem
        billingLedger2 = BillingLedgerTest.createOneBillingLedger("SM-3KBZD",
                priceItemName2, 1, date1);
        Assert.assertNotEquals(billingLedger1, billingLedger2);

        // change the quantity, should equate since quantity is not used for comparison
        billingLedger2 = BillingLedgerTest.createOneBillingLedger("SM-3KBZD",
                priceItemName1, 2, date1);
        Assert.assertEquals(billingLedger1, billingLedger2);

    }

    @Test
    public void testBean() {
        new BeanTester().testBean(BillingLedger.class);
        new EqualsMethodTester().testEqualsMethod(BillingLedger.class, "billingMessage" );
        new HashCodeMethodTester().testHashCodeMethod(BillingLedger.class);

    }


}
