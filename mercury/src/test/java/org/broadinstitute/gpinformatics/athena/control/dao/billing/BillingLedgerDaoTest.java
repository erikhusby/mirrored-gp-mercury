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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Tests for the billing ledger
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION,enabled=true)
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

        order = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao, "DRAFT-" + UUID.randomUUID());
        PriceItem priceItem = priceItemDao.findAll().get(0);

        // Make sure there is at least one ledger entry
        ledger = new BillingLedger(order.getSamples().get(0), priceItem, 2);

        billingLedgerDao.persist(ledger);

        utx.begin();
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
        List<BillingLedger> ledgerEntries = billingLedgerDao.findByOrderList(Collections.singletonList(order));
        Assert.assertTrue("The specified order should find the test ledger", ledgerEntries.get(0).equals(ledger));
    }
}
