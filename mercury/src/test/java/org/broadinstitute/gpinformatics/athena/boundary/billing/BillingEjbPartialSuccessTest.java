package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.common.TestLogHandler;
import org.broadinstitute.gpinformatics.infrastructure.common.TestUtils;
import org.broadinstitute.gpinformatics.infrastructure.quote.ApprovalStatus;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.FundingLevel;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFunding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePlatformType;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.NullOrEmptyCollection.nullOrEmptyCollection;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.SuccessfullyBilled.successfullyBilled;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.UnsuccessfullyBilled.unsuccessfullyBilled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.ALTERNATIVES, enabled = true)
@Dependent
public class BillingEjbPartialSuccessTest extends Arquillian {

    public BillingEjbPartialSuccessTest() {
        super();
        Logger billingAdaptorLogger = Logger.getLogger(BillingAdaptor.class.getName());
        billingAdaptorLogger.setLevel(Level.ALL);
        testLogHandler = new TestLogHandler();
        billingAdaptorLogger.addHandler(testLogHandler);
        testLogHandler.setLevel(Level.ALL);
    }

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private PriceListCache priceListCache;

    public static final String GOOD_WORK_ID = "workItemId\t1000";
    final long time = (new Date()).getTime();

    public static final String SM_1234 = "SM-"+(new Date()).getTime();
    public static final String SM_5678 = "SM-"+(new Date()).getTime()+1;
    private static String FAILING_PRICE_ITEM_NAME = "";
    private static String FAILING_PRICE_ITEM_SAMPLE = "";

    protected static int quoteCount;
    protected static int totalItems;

    protected static boolean cycleFails = true;
    protected static Result lastResult = Result.FAIL;

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
    private SAPProductPriceCache productPriceCache;

    @Inject
    private SAPAccessControlEjb accessControlEjb;

    public enum Result {FAIL, SUCCESS}

    public static final Log log = LogFactory.getLog(BillingEjbPartialSuccessTest.class);
    protected static final Object lockBox = new Object();

    private TestLogHandler testLogHandler;

    private void setBillingAdaptor(Collection<PriceItem> primaryPriceItems) {
        final Collection<QuotePriceItem> quotePriceItems = priceListCache.getQuotePriceItems();
        for(PriceItem primaryPriceItem: primaryPriceItems) {
            quotePriceItems.add(new QuotePriceItem(primaryPriceItem.getCategory(),
                    primaryPriceItem.getName() + "_id", primaryPriceItem.getName(),
                    "250", "each", primaryPriceItem.getPlatform()));
        }

        PriceListCache tempPriceListCache = new PriceListCache(quotePriceItems);

        billingAdaptor = new BillingAdaptor(billingEjb, billingSessionDao, tempPriceListCache, quoteService,
                billingSessionAccessEjb, sapService, productPriceCache, accessControlEjb);
        billingAdaptor.setProductOrderEjb(productOrderEjb);
    }

    /**
     * This will succeed in billing some but not all work to make sure our Billing Session is left in the state we
     * expect.
     */
    @Alternative
    @ApplicationScoped
    protected static class PartiallySuccessfulQuoteServiceStub implements QuoteService {

        public PartiallySuccessfulQuoteServiceStub(){}

        private static final long serialVersionUID = 6093273925949722169L;
        private Log log = LogFactory.getLog(QuoteFundingList.class);

        @Override
        public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
            return new PriceList();
        }

        @Override
        public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
            return new Quotes();
        }

        @Override
        public String registerNewWork(Quote quote,
                                      QuotePriceItem quotePriceItem,
                                      QuotePriceItem itemIsReplacing,
                                      Date reportedCompletionDate,
                                      double numWorkUnits,
                                      String callbackUrl, String callbackParameterName, String callbackParameterValue,
                                      BigDecimal priceAdjustment) {
            // Simulate failure only for one particular PriceItem.
            log.debug("In register New work");
            if (FAILING_PRICE_ITEM_NAME.equals(quotePriceItem.getName())) {
                throw new RuntimeException("Intentional Work Registration Failure!");
            }
            String workId = GOOD_WORK_ID;

            if (cycleFails) {
                switch (lastResult) {
                case FAIL:
                    lastResult = Result.SUCCESS;
                    workId = "workItemID" + (new Date()).getTime();
                    break;

                case SUCCESS:
                    lastResult = Result.FAIL;
                    throw new RuntimeException("Intentional Work Registration Failure");
                }
            }
            synchronized (lockBox) {
                quoteCount++;
                /*log.debug*/
                log.debug("Quote count is now " + quoteCount);
                assertThat(quoteCount, is(lessThanOrEqualTo(totalItems)));
            }
            return workId;
        }

        @Override
        public String registerNewSAPWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                         Date reportedCompletionDate, double numWorkUnits, String callbackUrl,
                                         String callbackParameterName, String callbackParameterValue,
                                         BigDecimal priceAdjustment) {
            // Simulate failure only for one particular PriceItem.
            log.debug("In register New work");
            if (FAILING_PRICE_ITEM_NAME.equals(quotePriceItem.getName())) {
                throw new RuntimeException("Intentional Work Registration Failure!");
            }
            String workId = GOOD_WORK_ID;

            if (cycleFails) {
                switch (lastResult) {
                case FAIL:
                    lastResult = Result.SUCCESS;
                    workId = "workItemID" + (new Date()).getTime();
                    break;

                case SUCCESS:
                    lastResult = Result.FAIL;
                    throw new RuntimeException("Intentional Work Registration Failure");
                }
            }
            synchronized (lockBox) {
                quoteCount++;
                /*log.debug*/
                log.debug("Quote count is now " + quoteCount);
                assertThat(quoteCount, is(lessThanOrEqualTo(totalItems)));
            }
            return workId;
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            FundingLevel level = new FundingLevel("100", Collections.singleton(new Funding(Funding.PURCHASE_ORDER,null, null)));
            QuoteFunding funding = new QuoteFunding(Collections.singleton(level));
            final Quote quote = new Quote("test1", funding, ApprovalStatus.FUNDED);

            return quote;
        }

        @Override
        public Quote getQuoteWithPriceItems(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            final Quote quoteByAlphaId = getQuoteByAlphaId(alphaId);

            quoteByAlphaId.setQuoteItems(Collections.singleton(new QuoteItem("test1", "priceitem1","Price Item", "10",
                    "1000", "each", "Genomics Platform", "testing")));
            return quoteByAlphaId;
        }

        @Override
        public Set<Funding> getAllFundingSources() throws QuoteServerException, QuoteNotFoundException{
            return null;
        }

        @Override
        public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {
            return null;
        }

        @Override
        public PriceList getPriceItemsForDate(List<QuoteImportItem> targetedPriceItemCriteria)
                throws QuoteServerException, QuoteNotFoundException {
            PriceList testPriceList = new PriceList();

            for (QuoteImportItem targetedPriceItemCriterion : targetedPriceItemCriteria) {
                final QuotePriceItem quotePriceItem =
                        QuotePriceItem.convertMercuryPriceItem(targetedPriceItemCriterion.getPriceItem());
                quotePriceItem.setPrice("50.00");
                testPriceList.add(quotePriceItem);
            }

            return testPriceList;
        }

        @Override
        public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType)
                throws QuoteServerException, QuoteNotFoundException {
            return getAllPriceItems();
        }
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(PartiallySuccessfulQuoteServiceStub.class);
    }

    /**
     * <ul>
     * <li>Create a Product, including its ProductFamily, ResearchProject, and primary PriceItem.</li>
     * <li>Create a ProductOrder that references the Product and has two ProductOrderSamples.</li>
     * <li>Create a replacement PriceItem.</li>
     * <li>Create two LedgerEntries referencing each of the ProductOrderSamples in this PDO, one with the primary
     * PriceItem from the Product, the other with the replacement PriceItem.</li>
     * <li>Create a BillingSession containing these two LedgerEntries.</li>
     * <li>Persist all of this data, outside of a transaction, flush and clear the entity manager.</li>
     * </ul>
     *
     * @param orderSamples The sampels for the order
     */
    private BillingSession writeFixtureData(String... orderSamples) {

        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(productOrderDao, orderSamples);
        quoteCount = 0;
        totalItems = 1;
        Set<LedgerEntry> billingSessionEntries = new HashSet<>(productOrder.getSamples().size());

        Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

        Set<PriceItem> reusablePriceItems = new HashSet<>();

        for (String sampleName : orderSamples) {
            for (ProductOrderSample ledgerSample : samplesByName.get(sampleName)) {

                reusablePriceItems.add(productOrder.getProduct().getPrimaryPriceItem());
                if (FAILING_PRICE_ITEM_SAMPLE.equals(sampleName)) {
                    UUID uuid = UUID.randomUUID();
                    PriceItem replacementPriceItem =
                            new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category",
                                          "Replacement PriceItem Name " + uuid);

                    reusablePriceItems.add(replacementPriceItem);
                    FAILING_PRICE_ITEM_NAME = replacementPriceItem.getName();
                    billingSessionDao.persist(replacementPriceItem);

                    billingSessionEntries.add(new LedgerEntry(ledgerSample, replacementPriceItem, new Date(), 5));
                } else {
                    billingSessionEntries.add(new LedgerEntry(ledgerSample,
                                                              productOrder.getProduct().getPrimaryPriceItem(),
                                                              new Date(), 3));
                }
            }
        }

        setBillingAdaptor(reusablePriceItems);
        BillingSession billingSession = new BillingSession(-1L, billingSessionEntries);
        billingSessionDao.persist(billingSession);

        billingSessionDao.flush();
        billingSessionDao.clear();

        return billingSession;
    }

    /**
     * <ul>
     * <li>Create a Product, including its ProductFamily, ResearchProject, and primary PriceItem.</li>
     * <li>Create a ProductOrder that references the Product and has two ProductOrderSamples.</li>
     * <li>Create a replacement PriceItem.</li>
     * <li>Create two LedgerEntries referencing each of the ProductOrderSamples in this PDO, one with the primary
     * PriceItem from the Product, the other with the replacement PriceItem.</li>
     * <li>Create a BillingSession containing these two LedgerEntries.</li>
     * <li>Persist all of this data, outside of a transaction, flush and clear the entity manager.</li>
     * </ul>
     *
     * @param orderSamples
     */
    protected BillingSession writeFixtureDataOneSamplePerProductOrder(String... orderSamples) {

        Set<LedgerEntry> billingSessionEntries = new HashSet<>(orderSamples.length);
        Set<PriceItem> reusablePriceItems = new HashSet<>();


        for (String sample : orderSamples) {
            ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, sample);

            Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

            for (ProductOrderSample ledgerSample : samplesByName.get(sample)) {
                reusablePriceItems.add(productOrder.getProduct().getPrimaryPriceItem());
                if (FAILING_PRICE_ITEM_SAMPLE.equals(sample)) {
                    UUID uuid = UUID.randomUUID();
                    PriceItem replacementPriceItem =
                            new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category",
                                          "Replacement PriceItem Name " + uuid);
                    reusablePriceItems.add(replacementPriceItem);
                    FAILING_PRICE_ITEM_NAME = replacementPriceItem.getName();
                    billingSessionDao.persist(replacementPriceItem);

                    billingSessionEntries.add(new LedgerEntry(ledgerSample, replacementPriceItem, new Date(), 5));
                } else {
                    billingSessionEntries.add(new LedgerEntry(ledgerSample,
                                                              productOrder.getProduct().getPrimaryPriceItem(),
                                                              new Date(), 3));
                }
            }
        }
        setBillingAdaptor(reusablePriceItems);

        BillingSession billingSession = new BillingSession(-1L, billingSessionEntries);
        billingSessionDao.persist(billingSession);

        billingSessionDao.flush();
        billingSessionDao.clear();

        return billingSession;
    }

    /**
     * <ul>
     * <li>Create the fixture data per {@link #writeFixtureData(String...)}.</li>
     * <li>Load the BillingSession and call a version of the BillingEjb method that <b>does</b> call DAO methods
     * inside a transactionally demarcated method, which should cause the EntityManager to be enrolled in the
     * transaction.</li>
     * <li>Clear the entity manager again and load the BillingSession, confirm that the billing messages were
     * persisted.</li>
     * </ul>
     */
    @Test(groups = TestGroups.ALTERNATIVES, enabled = true)
      public void testPositive() {

        cycleFails = false;

        FAILING_PRICE_ITEM_SAMPLE = SM_5678;
        BillingSession billingSession = writeFixtureData(new String[]{SM_1234, SM_5678});
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        billingAdaptor.billSessionItems("http://www.broadinstitute.org", billingSession.getBusinessKey());

        billingSessionDao.clear();
        // Re-fetch the updated BillingSession from the database.
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        // The BillingSession should exist as this billing attempt should have been partially successful.
        assertThat(billingSession, is(not(nullValue())));

        List<LedgerEntry> ledgerEntryItems = billingSession.getLedgerEntryItems();
        assertThat(ledgerEntryItems, is(not(nullOrEmptyCollection())));
        assertThat(ledgerEntryItems, hasSize(2));

        String failMessage = null;
        for (LedgerEntry ledgerEntry : billingSession.getLedgerEntryItems()) {
            if (SM_1234.equals(ledgerEntry.getProductOrderSample().getName())) {
                assertThat(ledgerEntry, is(successfullyBilled()));
                assertThat(ledgerEntry.getWorkItem(), is(GOOD_WORK_ID));
            }
            if (SM_5678.equals(ledgerEntry.getProductOrderSample().getName())) {
                assertThat(ledgerEntry, is(unsuccessfullyBilled()));
                assertThat(ledgerEntry.getWorkItem(), is(nullValue()));
                failMessage = "A problem occurred attempting to post to the quote server for " +
                              ledgerEntry.getBillingSession().getBusinessKey() +
                              ".java.lang.RuntimeException: Intentional Work Registration Failure!";
            }
        }
        String successMessagePattern = "Work item \'" + GOOD_WORK_ID + "\' and SAP Document 'null' with completion date .*";
        assertThat(failMessage, notNullValue());

        assertThat(testLogHandler.messageMatches(failMessage), is(true));
        Collection<LogRecord> successLogs = testLogHandler.findLogs(successMessagePattern);
        assertThat(successLogs.isEmpty(), is(false));
        for (LogRecord successLog : successLogs) {
            assertThat(successLog.getLevel(), is(Level.INFO));
        }
    }

    @Test(groups = TestGroups.ALTERNATIVES, enabled = true)
    public void testMultipleFailure() throws Exception{

        final long time = (new Date()).getTime();
        String[] sampleNameList = {"SM-"+time, "SM-"+time+1, "SM-"+time+2, "SM-"+time+3, "SM-"+time+4, "SM-"+time+5, "SM-"+time+6,
                "SM-"+time+7};
        cycleFails = true;
        lastResult = Result.FAIL;
        quoteCount = 0;
        totalItems = sampleNameList.length;

        BillingSession billingSession = writeFixtureDataOneSamplePerProductOrder(sampleNameList);
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        Collection<BillingEjb.BillingResult> billingResults = billingAdaptor.billSessionItems("http://www.broadinstitute.org",
                                                                        billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        List<QuoteImportItem> quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);

        assertThat("the size of the unBilled items should equal half the size of the total items to be billed",
                   quoteImportItems.size(), is(equalTo(sampleNameList.length / 2)));
        Collection<BillingEjb.BillingResult> billingResults1 = billingAdaptor.billSessionItems("http://www.broadinstitute.org",
                                                                               billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);

        assertThat("the size of the unBilled items should equal half the size of the total items to be billed",
                   quoteImportItems.size(), is(equalTo(sampleNameList.length / 4)));

        cycleFails = false;
        Collection<BillingEjb.BillingResult> billingResults3 = billingAdaptor.billSessionItems("http://www.broadinstitute.org",
                                                                         billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);

        assertThat("the size of the unBilled items should be zero",
                   quoteImportItems.size(), is(equalTo(0)));
    }

    @Test(groups = TestGroups.ALTERNATIVES, enabled = true)
    public void testNoForcedFailures() throws Exception {

        log.debug("Running no forced failures threaded");

        final long time = (new Date()).getTime();
        String[] sampleNameList = {"SM-"+time, "SM-9291"+time+1, "SM-"+time+2, "SM-"+time+3, "SM-"+time+4, "SM-"+time+5, "SM-"+time+6,
                "SM-"+time+7};
        cycleFails = false;
        lastResult = Result.FAIL;
        quoteCount = 0;
        totalItems = sampleNameList.length;


        BillingSession billingSession = writeFixtureDataOneSamplePerProductOrder(sampleNameList);
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        Collection<BillingEjb.BillingResult> billingResults = billingAdaptor.billSessionItems("http://www.broadinstitute.org",
                                                                        billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        List<QuoteImportItem> quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);
    }

    @Test(groups = TestGroups.ALTERNATIVES, enabled = true)
    public void testBillingAdaptorLog() {
        BillingAdaptor adaptor = new BillingAdaptor();
        PriceItem priceItem = new PriceItem("quoteServerId", "myPlatform", "myCategory", "importItemName");
        Product product = new Product();
        LedgerEntry ledgerEntry1 = new LedgerEntry(new ProductOrderSample("SM-1234"), priceItem, new Date(), 5);
        LedgerEntry ledgerEntry2 = new LedgerEntry(new ProductOrderSample("SM-5678"), priceItem, new Date(), 5);
        List<LedgerEntry> ledgerItems = Arrays.asList(ledgerEntry1, ledgerEntry2);
        ProductOrder productOrder = new ProductOrder();
        QuoteImportItem quoteImportItem =
                new QuoteImportItem("QUOTE-1", priceItem, "priceType", ledgerItems, new Date(), product, productOrder);
        QuotePriceItem quotePriceItem = new QuotePriceItem();

        adaptor.logBilling("1243", quoteImportItem, quotePriceItem, new HashSet<>(Arrays.asList("PDO-1", "PDO-2")),
                "SAP123");
        Assert.assertEquals(testLogHandler.getLogs().size(), 1);
        Assert.assertEquals(TestUtils.getFirst(testLogHandler.getLogs()).getLevel(), Level.INFO);
    }
}
