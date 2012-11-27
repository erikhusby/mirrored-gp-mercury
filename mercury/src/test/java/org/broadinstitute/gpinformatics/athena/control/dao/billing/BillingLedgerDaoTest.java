package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDaoTest;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Tests for the billing ledger (need to set up all the appropriate data so that the different cases can be tested
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled=false)
public class BillingLedgerDaoTest {

    @Inject
    private BillingLedgerDao billingLedgerDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private UserTransaction utx;

    private ProductOrder order;
    private BillingLedger ledger;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        order = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao);
        PriceItem priceItem = priceItemDao.findAll().get(0);

        // Make sure there is at least one ledger entry
        ledger = new BillingLedger(order.getSamples().get(0), priceItem, new Date(), 2);

        billingLedgerDao.persist(ledger);
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testFindBillingLedgers() {
        List<BillingLedger> ledgerEntries = billingLedgerDao.findAll();

        Assert.assertTrue("There must be at least one ledger entry", ledgerEntries.size() > 0);
    }

    public void testFindLedgerEntriesForPDOs() {
        ProductOrder[] orders = new ProductOrder[1];
        orders[0] = order;
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findByOrderList(orders);

        Assert.assertTrue("The specified order should find the test ledger", !ledgerEntries.isEmpty());
    }

    public void testFindLedgerEntriesForWithNoBillingSessionPDOs() {
        ProductOrder[] orders = new ProductOrder[1];
        orders[0] = order;
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findWithoutBillingSessionByOrderList(orders);

        Assert.assertTrue("The specified order should find the test ledger", !ledgerEntries.isEmpty());
    }

    public void testFindBilledLedgerEntriesForPDOs() {
        ProductOrder[] orders = new ProductOrder[1];
        orders[0] = order;
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findBilledByOrderList(orders);

        Assert.assertTrue("The specified order should find the test ledger", !ledgerEntries.isEmpty());
    }

    public void testFindUnbilledLedgerEntriesForPDOs() {
        ProductOrder[] orders = new ProductOrder[1];
        orders[0] = order;
        Set<BillingLedger> ledgerEntries = billingLedgerDao.findLockedOutByOrderList(orders);

        Assert.assertTrue("The specified order should find the test ledger", !ledgerEntries.isEmpty());
    }

    public void testRemoveLedgerUpdates() {
        ProductOrder[] orders = new ProductOrder[1];
        orders[0] = order;
        billingLedgerDao.removeLedgerItemsWithoutBillingSession(orders);

        Set<BillingLedger> ledgerEntries = billingLedgerDao.findWithoutBillingSessionByOrderList(orders);

        Assert.assertTrue("The specified orders should not have any ledger entries without sessions", ledgerEntries.isEmpty());
    }
}
