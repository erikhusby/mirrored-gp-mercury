package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderAddOn;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.AbstractContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Test(groups = TestGroups.ALTERNATIVES, enabled = true)
@Dependent
public class BillingWorkItemPersistenceTest extends AbstractContainerTest {

    public BillingWorkItemPersistenceTest(){}

    @Inject
    ProductOrderDao pdoDao;

    @Inject
    BillingSessionDao billingSessionDao;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private BillingEjb billingEjb;

    @Inject
    private QuoteService quoteService;

    @Inject
    private BillingSessionAccessEjb billingSessionAccessEjb;

    // Stub implementation
    private SapIntegrationService sapService = new SapIntegrationServiceStub();

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment deployment;

    private BillingAdaptor billingAdaptor;
    @Inject
    LedgerEntryDao ledgerEntryDao;

    @Inject
    PriceItemDao priceItemDao;

    @Inject
    ProductOrderSampleDao pdoSampleDao;

    private static final String PDO_BUSINESS_KEY = "PDO-3510";

    private String billingSessionKey;

    private ProductOrder pdo;

    @Inject
    private SAPProductPriceCache productPriceCache;

    @Inject
    private SAPAccessControlEjb accessControlEjb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(AcceptsAllWorkRegistrationsQuoteServiceStub.class);
    }

    /**
     * Add a single ledger entry to each sample in {@link #PDO_BUSINESS_KEY},
     * flushes and clears so that subsequent lookups will go back to the database.
     */
    @BeforeMethod
    public void setUp() {
        if (!isRunningInContainer()) {return;}

        pdo = pdoDao.findByBusinessKey(PDO_BUSINESS_KEY);

        final Collection<QuotePriceItem> quotePriceItems = priceListCache.getQuotePriceItems();

        Set<PriceItem> primaryPriceItems = new HashSet<>();

        primaryPriceItems.add(pdo.getProduct().getPrimaryPriceItem());
        for (ProductOrderAddOn productOrderAddOn : pdo.getAddOns()) {
            primaryPriceItems.add(productOrderAddOn.getAddOn().getPrimaryPriceItem());
        }

        Assert.assertNotNull(pdo, "Can't find " + PDO_BUSINESS_KEY
                                  + ".  No way to verify that work item is being persisted properly.");
        Set<LedgerEntry> ledgerEntries = new HashSet<>();
        PriceItem priceItem = priceItemDao.findById(PriceItem.class, 46L);
        primaryPriceItems.add(priceItem);
        for (ProductOrderSample pdoSample : pdo.getSamples()) {
            LedgerEntry ledgerEntry = new LedgerEntry(pdoSample, priceItem, new Date(), 3);
            pdoSample.getLedgerItems().add(ledgerEntry);
            ledgerEntries.add(ledgerEntry);
            ledgerEntryDao.persist(ledgerEntry);
        }

        for(PriceItem primaryPriceItem: primaryPriceItems) {
            quotePriceItems.add(new QuotePriceItem(primaryPriceItem.getCategory(),
                    primaryPriceItem.getName() + "_id", primaryPriceItem.getName(),
                    "250", "each", primaryPriceItem.getPlatform()));
        }

        PriceListCache tempPriceListCache = new PriceListCache(quotePriceItems);
        billingAdaptor = new BillingAdaptor(billingEjb, tempPriceListCache, quoteService,
                billingSessionAccessEjb, sapService, productPriceCache, accessControlEjb, null);
        billingAdaptor.setProductOrderEjb(productOrderEjb);

        BillingSession billingSession = new BillingSession(-1L, ledgerEntries);
        billingSessionDao.persist(billingSession);
        billingSessionDao.flush();
        billingSessionDao.clear();
        billingSessionKey = billingSession.getBusinessKey();
    }

    @Test
    public void testThatBillingSavesTheWorkItemToDatabase() {
        billingAdaptor.billSessionItems("blah", billingSessionKey);

        BillingSession billingSession = billingSessionDao.findByBusinessKey(billingSessionKey);
        Assert.assertEquals(billingSession.getLedgerEntryItems().size(), pdo.getSamples().size(),
                            "It looks like the setup has failed to make a ledger item for each pdo sample.");
        for (LedgerEntry ledgerEntry : billingSession.getLedgerEntryItems()) {
            Assert.assertTrue(
                    ledgerEntry.getWorkItem().contains(AcceptsAllWorkRegistrationsQuoteServiceStub.WORK_ITEM_PREPEND),
                    "It appears that billing is not persisting the work item to the database, which means that we cannot reliably audit our finances.");
        }
    }

}
