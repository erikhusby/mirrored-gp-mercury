package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDaoTest;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Test the billing ledger dao
 * User: mccrory
 * Date: 12/7/12
 */

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled=true)
public class BillingLedgerDaoTest  extends ContainerTest {

    @Inject
    private BillingLedgerDao billingLedgerDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private UserTransaction utx;

    private ProductOrder order;
    private ProductOrder orderWithDupes;

    private BillingLedger unBilledLedger1;

    private String priceItemName;
    private ProductOrderSample productOrderSample1;

    private DateFormat formatter = new SimpleDateFormat("MM/dd/yy");
    private ProductOrder[] orders = new ProductOrder[1];
    private ProductOrder[] dupeOrders = new ProductOrder[1];


    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        PriceItem priceItem = priceItemDao.findAll().get(0);
        priceItemName = priceItem.getName();

        //Create an order and add samples to it.
        order = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao);
        productOrderSample1 = order.getSamples().get(0);

        //Create first ledger item
        Date date1 = formatter.parse("12/5/12");
        unBilledLedger1 = new BillingLedger(productOrderSample1, priceItem, date1, 1);

        //Add one ledger item to sample 1
        productOrderSample1.getLedgerItems().add(unBilledLedger1);

        productOrderDao.persist(order);
        productOrderDao.flush();

        orders[0] = order;

        //Create an order (with dupes) with a list of samples containing duplicate sample names
        List<String> sampleNames = new ArrayList<String>();
                        sampleNames.add(ProductOrderDaoTest.MS_1111+"X");
                        sampleNames.add(ProductOrderDaoTest.MS_1111+"X");
        orderWithDupes = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao, sampleNames);
        ProductOrderSample productOrderSample1ForDupes = orderWithDupes.getSamples().get(0);
        ProductOrderSample productOrderSample2ForDupes = orderWithDupes.getSamples().get(1);

        //Create two ledger items for the new order one for each productOrderSample different quantities
        Date date2 = formatter.parse("12/6/12");
        BillingLedger unBilledLedger1ForDupes = new BillingLedger(productOrderSample1ForDupes, priceItem, date2, 8);
        BillingLedger unBilledLedger2ForDupes = new BillingLedger(productOrderSample2ForDupes, priceItem, date2, 16);

        //Add the ledger items one to each of the dupes samples
        productOrderSample1ForDupes.getLedgerItems().add(unBilledLedger1ForDupes);
        productOrderSample2ForDupes.getLedgerItems().add(unBilledLedger2ForDupes);

        // now persist the dupe Order
        productOrderDao.persist(orderWithDupes);
        productOrderDao.flush();

        // addit to the array for convenience
        dupeOrders[0] = orderWithDupes;

    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    //
    public void testFindBillingLedgers() {
        List<BillingLedger> ledgerEntries = billingLedgerDao.findAll();
        Assert.assertTrue("The specified order should find at one test ledger", ledgerEntries.size() > 0 ) ;
    }

    //
    public void testFindLedgerEntriesForPDOs() {
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findByOrderList(orders);
        Assert.assertEquals("The specified order should find one test ledger", 1, ledgerEntries.size()) ;
    }

    public void testFindLedgerEntriesForPDOsWithNoBillingSessions() {
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findWithoutBillingSessionByOrderList(orders);
        Assert.assertEquals("The specified order should find one test ledger", 1, ledgerEntries.size()) ;
    }

    public void testFindBilledLedgerEntriesForPDOs() {
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findBilledByOrderList(orders);
        Assert.assertTrue("The specified order should not find any ledger items", ledgerEntries.isEmpty());
    }

    public void testFindLockedOutByOrderList() {
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findLockedOutByOrderList(orders);
        // No session started yet so should be none for this order
        Assert.assertEquals("The specified order should find one test ledger", 0, ledgerEntries.size()) ;
    }

    public void testRemoveLedgerUpdates() {
        {
        // test an order
        billingLedgerDao.removeLedgerItemsWithoutBillingSession(orders);
        billingLedgerDao.flush();
        //verify by trying to retieve what is pending but not yet billed should be none left
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findWithoutBillingSessionByOrderList(orders);
        Assert.assertEquals("The specified order should find one test ledger", 1, ledgerEntries.size()) ;
        }

        {
        // test an order with dupes
        billingLedgerDao.removeLedgerItemsWithoutBillingSession(dupeOrders);
        billingLedgerDao.flush();
        //verify by trying to retieve what is pending but not yet billed should be none left
        Set<BillingLedger> dupeLedgerEntries = billingLedgerDao.findWithoutBillingSessionByOrderList(dupeOrders);
        Assert.assertEquals("The specified order should find one test ledger", 2, dupeLedgerEntries.size()) ;
        }

    }

}
