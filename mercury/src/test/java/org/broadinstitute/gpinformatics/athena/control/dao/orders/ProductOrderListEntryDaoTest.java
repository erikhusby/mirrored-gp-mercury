package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.apache.commons.collections15.CollectionUtils;
import org.apache.commons.collections15.Predicate;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.projects.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderListEntry;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.*;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class ProductOrderListEntryDaoTest extends ContainerTest {

    @Inject
    private ProductOrderListEntryDao productOrderListEntryDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private BillingLedgerDao billingLedgerDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private UserTransaction utx;

    private ProductOrder order;


    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.begin();

        order = ProductOrderDaoTest.createTestProductOrder(researchProjectDao, productDao);

        // need to initialize the price items here as we will be exercising their hashCode methods when we create
        // BillingLedger entities in some of our tests
        order.getProduct().getPrimaryPriceItem().hashCode();
        for (PriceItem priceItem : order.getProduct().getOptionalPriceItems()) {
            priceItem.hashCode();
        }
        productOrderDao.persist(order);

        productOrderDao.flush();
        productOrderDao.clear();

    }


    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }


    /**
     * Sanity check: should not see any PDOs represented more than once
     *
     * @param productOrderListEntries
     */
    private static void assertPDOUniqueness(Collection<ProductOrderListEntry> productOrderListEntries) {

        Map<String, Long> countsMap = new HashMap<String, Long>();
        for (ProductOrderListEntry productOrderListEntry : productOrderListEntries) {
            if (! countsMap.containsKey(productOrderListEntry.getJiraTicketKey())) {
                countsMap.put(productOrderListEntry.getJiraTicketKey(), 1L);
            }
            else {
                countsMap.put(productOrderListEntry.getJiraTicketKey(), countsMap.get(productOrderListEntry.getJiraTicketKey()) + 1);
            }
        }

        Set<Map.Entry<String,Long>> countsEntries = countsMap.entrySet();
        CollectionUtils.filter(countsEntries, new Predicate<Map.Entry<String, Long>>() {
            @Override
            public boolean evaluate(Map.Entry<String, Long> stringLongEntry) {
                String key = stringLongEntry.getKey();
                return stringLongEntry.getValue() > 1 && key != null;
            }
        });

        if ( ! countsEntries.isEmpty()) {
            List<String> strings = new ArrayList<String>();
            for (Map.Entry<String, Long> countEntry : countsEntries) {
                strings.add(countEntry.getKey() + ": " + countEntry.getValue());
                Assert.fail("Found PDOs represented more than once: " + StringUtils.join(strings, ", "));
            }
        }
    }


    private ProductOrderListEntry sanityCheckAndGetTestOrderListEntry() {
        List<ProductOrderListEntry> productOrderListEntries = productOrderListEntryDao.findProductOrderListEntries();

        Assert.assertNotNull(productOrderListEntries);

        // this is a container test so there is data in there beyond our test data.  if that turns up bugs for us, great!
        assertPDOUniqueness(productOrderListEntries);

        CollectionUtils.filter(productOrderListEntries, new Predicate<ProductOrderListEntry>() {
            @Override
            public boolean evaluate(ProductOrderListEntry productOrderListEntry) {
                return productOrderListEntry.getJiraTicketKey() != null &&
                       productOrderListEntry.getJiraTicketKey().equals(ProductOrderListEntryDaoTest.this.order.getJiraTicketKey());
            }
        });

        Assert.assertEquals(productOrderListEntries.size(), 1);

        return productOrderListEntries.iterator().next();
    }



    public void testNoLedgerEntries() {

        ProductOrderListEntry productOrderListEntry = sanityCheckAndGetTestOrderListEntry();

        Assert.assertEquals(productOrderListEntry.getUnbilledLedgerEntryCount(), Long.valueOf(0L));
        Assert.assertNull(productOrderListEntry.getBillingSessionBusinessKey());
    }



    public void testOneLedgerEntryNoBillingSession() {

        BillingLedger billingLedger =
                new BillingLedger(order.getSamples().iterator().next(), order.getProduct().getPrimaryPriceItem(), new Date(), 2);

        billingLedgerDao.persist(billingLedger);
        billingLedgerDao.flush();
        billingLedgerDao.clear();

        ProductOrderListEntry productOrderListEntry = sanityCheckAndGetTestOrderListEntry();

        Assert.assertEquals(productOrderListEntry.getUnbilledLedgerEntryCount(), Long.valueOf(1L));
        Assert.assertNull(productOrderListEntry.getBillingSessionBusinessKey());

    }


    public void testOneLedgerEntryWithBillingSession() {
        BillingLedger billingLedger =
                new BillingLedger(order.getSamples().iterator().next(), order.getProduct().getPrimaryPriceItem(), new Date(), 2);

        BillingSession billingSession = new BillingSession(1L, Collections.singleton(billingLedger));

        billingSessionDao.persist(billingSession);
        billingSessionDao.flush();
        billingSessionDao.clear();

        ProductOrderListEntry productOrderListEntry = sanityCheckAndGetTestOrderListEntry();

        Assert.assertEquals(productOrderListEntry.getUnbilledLedgerEntryCount(), Long.valueOf(1L));
        Assert.assertEquals(productOrderListEntry.getBillingSessionBusinessKey(), billingSession.getBusinessKey());

    }

    // it would be nice to have tests for multiple ledger entries but that would require us to know that a given order
    // has either more than one sample or more than one price item associated with its product (either through add-ons
    // or optional price items on the main product).  we're currently picking product orders completely at random from
    // the db so this isn't currently possible.  we should ideally generate our own rich product / product order test
    // fixture data instead.

}
