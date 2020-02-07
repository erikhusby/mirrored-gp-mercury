package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.apache.commons.lang3.time.FastDateFormat;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Set;


@Test(groups = TestGroups.STUBBY)
@Dependent
public class LedgerEntryDaoTest extends StubbyContainerTest {

    public LedgerEntryDaoTest(){}

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private PriceItemDao priceItemDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    private static final FastDateFormat formatter = FastDateFormat.getInstance("MM/dd/yy");
    private final ProductOrder[] orders = new ProductOrder[1];
    private final ProductOrder[] dupeOrders = new ProductOrder[1];


    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, it means we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();

        PriceItem priceItem = priceItemDao.findAll().get(0);

        // Create an order and add samples to it.
        ProductOrder order = ProductOrderDBTestFactory.createTestProductOrder(researchProjectDao, productDao);
        ProductOrderSample productOrderSample1 = order.getSamples().get(0);

        // Create first ledger item.
        Date date1 = formatter.parse("12/5/12");
        LedgerEntry unBilledLedger1;
        if(order.hasSapQuote()) {
            unBilledLedger1 = new LedgerEntry(productOrderSample1, order.getProduct(), date1, BigDecimal.ONE);
        } else {
            unBilledLedger1 = new LedgerEntry(productOrderSample1, priceItem, date1,BigDecimal.ONE);
        }

        // Add one ledger item to sample 1.
        productOrderSample1.getLedgerItems().add(unBilledLedger1);

        productOrderDao.persist(order);
        productOrderDao.flush();

        orders[0] = order;

        // Create an order that contains duplicate sample names.
        ProductOrder orderWithDupes = ProductOrderDBTestFactory.createTestProductOrder(researchProjectDao, productDao,
                ProductOrderDBTestFactory.MS_1111 + "X", ProductOrderDBTestFactory.MS_1111 + "X");
        ProductOrderSample productOrderSample1ForDupes = orderWithDupes.getSamples().get(0);
        ProductOrderSample productOrderSample2ForDupes = orderWithDupes.getSamples().get(1);

        // Create two ledger items for the new order one for each productOrderSample different quantities.
        Date date2 = formatter.parse("12/6/12");
        LedgerEntry unBilledLedger1ForDupes;
        LedgerEntry unBilledLedger2ForDupes;
        if(orderWithDupes.hasSapQuote()) {

            unBilledLedger1ForDupes = new LedgerEntry(productOrderSample1ForDupes, orderWithDupes.getProduct(), date2,
                    BigDecimal.valueOf(8));
            unBilledLedger2ForDupes = new LedgerEntry(productOrderSample2ForDupes, orderWithDupes.getProduct(), date2, BigDecimal.valueOf(16));
        } else {
            unBilledLedger1ForDupes = new LedgerEntry(productOrderSample1ForDupes, priceItem, date2, BigDecimal.valueOf(8));
            unBilledLedger2ForDupes = new LedgerEntry(productOrderSample2ForDupes, priceItem, date2, BigDecimal.valueOf(16));
        }

        // Add the ledger items one to each of the dupes samples.
        productOrderSample1ForDupes.getLedgerItems().add(unBilledLedger1ForDupes);
        productOrderSample2ForDupes.getLedgerItems().add(unBilledLedger2ForDupes);

        // Now persist the duplicate Order.
        productOrderDao.persist(orderWithDupes);
        productOrderDao.flush();

        // Add it to the array for convenience.
        dupeOrders[0] = orderWithDupes;

    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, it means we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    public void testFindLedgerEntriesForPDOs() {
        Set<LedgerEntry> ledgerEntries = ledgerEntryDao.findByOrderList(orders);
        Assert.assertEquals(1, ledgerEntries.size(), "The specified order should find one test ledger") ;
    }

    public void testFindLedgerEntriesForPDOsWithNoBillingSessions() {
        Set<LedgerEntry> ledgerEntries = ledgerEntryDao.findWithoutBillingSessionByOrderList(orders);
        Assert.assertEquals(1, ledgerEntries.size(), "The specified order should find one test ledger") ;
    }

    public void testFindBilledLedgerEntriesForPDOs() {
        Set<LedgerEntry> ledgerEntries = ledgerEntryDao.findBilledByOrderList(orders);
        Assert.assertTrue(ledgerEntries.isEmpty(), "The specified order should not find any ledger items");
    }

    public void testFindLockedOutByOrderList() {
        Set<LedgerEntry> ledgerEntries = ledgerEntryDao.findLockedOutByOrderList(orders);
        // No session started yet so should be none for this order.
        Assert.assertEquals(0, ledgerEntries.size(), "The specified order should find one test ledger") ;
    }

    public void testRemoveLedgerUpdates() {
        // Test an order.
        ledgerEntryDao.removeLedgerItemsWithoutBillingSession(orders);
        ledgerEntryDao.flush();
        // Verify by trying to retrieve what is pending but not yet billed, there should be one left.
        Set<LedgerEntry> ledgerEntries = ledgerEntryDao.findWithoutBillingSessionByOrderList(orders);
        Assert.assertEquals(1, ledgerEntries.size(), "The specified order should find one test ledger") ;

        // Test an order with dupes.
        ledgerEntryDao.removeLedgerItemsWithoutBillingSession(dupeOrders);
        ledgerEntryDao.flush();
        // Verify by trying to retrieve what is pending but not yet billed, there should be two left.
        Set<LedgerEntry> dupeLedgerEntries = ledgerEntryDao.findWithoutBillingSessionByOrderList(dupeOrders);
        Assert.assertEquals(2, dupeLedgerEntries.size(), "The specified order should find two test ledgers") ;
    }

}
