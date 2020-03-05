package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ConcurrentProductOrderDoubleCreateTest;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePlatformType;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ejb.EJBTransactionRolledbackException;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class BillingEjbJiraDelayedTest extends Arquillian {

    public BillingEjbJiraDelayedTest(){}

    private static boolean failQuoteCall = false;
    private static boolean inContainer = true;
    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

    @Inject
    private BillingAdaptor billingAdaptor;

    @Inject
    private BillingSessionAccessEjb billingSessionAccessEjb;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private ProductOrderEjb pdoEjb;

    private static final Log log = LogFactory.getLog(BillingEjbJiraDelayedTest.class);

    private static AtomicInteger failureTime = new AtomicInteger(0);

    private static AtomicInteger failureIncrement = new AtomicInteger(0);

    private static boolean forceSleep = true;
    private String[] sampleNameList = null;
    private String billingSessionBusinessKey;

    private static Map<String, Integer> dpCallCount = new HashMap<>();

    @Alternative
    @Dependent
    protected static class DelayedJiraService extends
            ConcurrentProductOrderDoubleCreateTest.ControlBusinessKeyJiraService {

        public DelayedJiraService(){}

        @Override
        public JiraIssue getIssue(String key) throws IOException {
            try {
                if (forceSleep) {
                    Thread.sleep(1000 * 6);
                }
            } catch (InterruptedException e) {
            }

            UUID uuid = UUID.randomUUID();
            String jiraKey = "PDO-" + uuid;

            return new JiraIssue(jiraKey, this);
        }
    }

    @Alternative
    @ApplicationScoped
    protected static class QuoteServiceStubWithWait implements QuoteService {

        public QuoteServiceStubWithWait(){}

        private static final long serialVersionUID = 6093273925949722169L;


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
                                      BigDecimal numWorkUnits,
                                      String callbackUrl, String callbackParameterName, String callbackParameterValue,
                                      BigDecimal priceAdjustment) {
            // Simulate failure only for one particular PriceItem.
            log.debug("In register New work");
            String workId = "workItemId\t1000";
            try {
                failureTime.getAndAdd(failureIncrement.get());
                if (failQuoteCall) {
                    log.info("Configuration is set to fail quote");
                    throw new RuntimeException("Error registering quote");
                } else {
                    log.info("Setting Sleep of " + failureTime.get());
                    Thread.sleep(1000 * failureTime.get());
                }
                log.info("Woke up from quote call");
            } catch (InterruptedException e) {
                // do nothing with this error.
            }

            return workId;
        }

        @Override
        public String registerNewSAPWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                         Date reportedCompletionDate, BigDecimal numWorkUnits, String callbackUrl,
                                         String callbackParameterName, String callbackParameterValue,
                                         BigDecimal priceAdjustment) {
            // Simulate failure only for one particular PriceItem.
            log.debug("In register New work");
            String workId = "workItemId\t1000";
            try {
                failureTime.getAndAdd(failureIncrement.get());
                if (failQuoteCall) {
                    log.info("Configuration is set to fail quote");
                    throw new RuntimeException("Error registering quote");
                } else {
                    log.info("Setting Sleep of " + failureTime.get());
                    Thread.sleep(1000 * failureTime.get());
                }
                log.info("Woke up from quote call");
            } catch (InterruptedException e) {
                // do nothing with this error.
            }

            return workId;
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            return getQuoteByAlphaId(alphaId, false);
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId, boolean forceDevQuoteRefresh) throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }

        @Override
        public Quote getQuoteWithPriceItems(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            return getQuoteByAlphaId(alphaId);
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
            return null;
        }

        @Override
        public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType)
                throws QuoteServerException, QuoteNotFoundException {
            return getAllPriceItems();
        }
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
     * @param billingSessionDao1 A dao for the session
     * @param orderSamples The samples
     */
    public static BillingSession writeFixtureDataOneSamplePerProductOrder(BillingSessionDao billingSessionDao1,
                                                                   String... orderSamples) {

        Set<LedgerEntry> billingSessionEntries = new HashSet<>(orderSamples.length);
        for (String sample : orderSamples) {
            ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao1, sample);

            Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

            for (ProductOrderSample ledgerSample : samplesByName.get(sample)) {

                UUID uuid = UUID.randomUUID();
                PriceItem replacementPriceItem =
                        new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category",
                                      "Replacement PriceItem Name " + uuid);
                billingSessionDao1.persist(replacementPriceItem);

                if(productOrder.hasSapQuote()) {
                    billingSessionEntries.add(new LedgerEntry(ledgerSample, productOrder.getProduct(), new Date(), BigDecimal.valueOf(5)));
                } else {
                    billingSessionEntries.add(new LedgerEntry(ledgerSample, replacementPriceItem, new Date(), BigDecimal.valueOf(5)));
                }
            }
        }
        BillingSession billingSession = new BillingSession(-1L, billingSessionEntries);
        billingSessionDao1.persist(billingSession);

        billingSessionDao1.flush();
        billingSessionDao1.clear();

        return billingSession;
    }


    @Deployment
    public static WebArchive buildMercuryWar() {
        inContainer = false;
        return DeploymentBuilder.buildMercuryWarWithAlternatives(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV,
                DelayedJiraService.class, QuoteServiceStubWithWait.class);
    }

    @BeforeMethod
    public void setUp() throws Exception {

        if (billingSessionDao == null) {
            return;
        }
        failureTime.set(0);

        sampleNameList = new String[]{"SM-2342", "SM-9291", "SM-2349", "SM-9944", "SM-4444", "SM-4441", "SM-1112",
                "SM-4488"};

        BillingSession billingSession = writeFixtureDataOneSamplePerProductOrder(billingSessionDao, sampleNameList);

        billingSessionBusinessKey = billingSession.getBusinessKey();
    }


    @Test(groups = TestGroups.ALTERNATIVES, dataProvider = "timeoutCases", enabled = false)
    public void testTransactionTimeout(String testScenario, Integer timeoutIncrement,
                                       Integer expectedSuccessfulLedgerEntries, Boolean mockProductOrderEjb,
                                       Boolean forceJiraSvcSleepParam, Boolean failQuote)
            throws Exception {

        log.info("[[[ The scenario is " + testScenario + "]]]");
        log.info("[[[ timeout increment is " + timeoutIncrement + "]]]");
        log.info("[[[ Expected number of successful entries is " + expectedSuccessfulLedgerEntries + "]]]");
        log.info("[[[ Failure time is " + failureTime.get() + "]]]");
        log.info("[[[ Failure time increment is " + failureIncrement.get() + "]]]");
        log.info("[[[ Fail quote is set for " + failQuote + "]]]");

        failureIncrement.set(timeoutIncrement);
        forceSleep = forceJiraSvcSleepParam;
        failQuoteCall = failQuote;
        log.info("[[[ Static fail quote is set for " + failQuoteCall + "]]]");
        if (mockProductOrderEjb) {
            ProductOrderEjb mockPDOEjb = Mockito.mock(ProductOrderEjb.class);
            Mockito.doThrow(new EJBTransactionRolledbackException()).when(mockPDOEjb).updateOrderStatusNoRollback(
                    Mockito.anyString());
            billingAdaptor.setProductOrderEjb(mockPDOEjb);
        } else {
            billingAdaptor.setProductOrderEjb(pdoEjb);
        }

        BillingSession billingSession = billingSessionDao.findByBusinessKey(billingSessionBusinessKey);

        billingAdaptor.billSessionItems("http://www.broadinstitute.org", billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSessionBusinessKey);

        List<QuoteImportItem> quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);
        Assert.assertEquals(quoteImportItems.size(), sampleNameList.length - expectedSuccessfulLedgerEntries);
        Assert.assertTrue(!billingSessionAccessEjb.isSessionLocked(billingSession.getBusinessKey()));
    }

    @DataProvider(name = "timeoutCases")
    public Object[][] timeoutData(Method method) {

        List<Object[]> dataList = new ArrayList<>();

        //These data cases were tested with a transaction timeout set to 5 seconds

        /*
         * Parameters for Data Provider Scenarios
         *
         * Test        Timeout       Expected Successful    Mock Product    Force Jira
         * Scenario,   Increment,    Ledger Entries,        Order Ejb,      Svc Sleep,     Fail Quote
         *
         */

        dataList.add(new Object[]{"8 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         8,      0,      true,       false,      true});
        dataList.add(new Object[]{"8 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "fail quote",         8,      0,      false,      true,       true});
        dataList.add(new Object[]{"8 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         8,      0,      false,      false,      true});
        dataList.add(new Object[]{"8 sec interval " +
                                  "mock PDOEjb jira " +
                                  "no sleep " +
                                  "don't fail quote",   8,      8,      true,       false,      false});
        dataList.add(new Object[]{"8 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "don't fail quote",   8,      8,      false,      true,       false});
        dataList.add(new Object[]{"8 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   8,      8,      false,      false,      false});

        dataList.add(new Object[]{"4 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         4,      0,      true,       false,      true});
        dataList.add(new Object[]{"4 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "fail quote",         4,      0,      false,      true,       true});
        dataList.add(new Object[]{"4 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         4,      0,      false,      false,      true});
        dataList.add(new Object[]{"4 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   4,      8,      true,       false,      false});
        dataList.add(new Object[]{"4 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "don't fail quote",   4,      8,      false,      true,       false});
        dataList.add(new Object[]{"4 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   4,      8,      false,      false,      false});

        dataList.add(new Object[]{"2 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         2,      0,      true,       false,      true});
        dataList.add(new Object[]{"2 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "fail quote",         2,      0,      false,      true,       true});
        dataList.add(new Object[]{"2 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         2,      0,      false,      false,      true});
        dataList.add(new Object[]{"2 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   2,      8,      true,       false,      false});
        dataList.add(new Object[]{"2 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "don't fail quote",   2,      8,      false,      true,       false});
        dataList.add(new Object[]{"2 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   2,      8,      false,      false,      false});

        dataList.add(new Object[]{"1 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         1,      0,      true,       false,      true});
        dataList.add(new Object[]{"1 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "fail quote",         1,      0,      false,      false,      true});
        dataList.add(new Object[]{"1 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         1,      0,      false,      true,       true});
        dataList.add(new Object[]{"1 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   1,      8,      true,       false,      false});
        dataList.add(new Object[]{"1 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "don't fail quote",   1,      8,      false,      false,      false});
        dataList.add(new Object[]{"1 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   1,      8,      false,      true,       false});

        dataList.add(new Object[]{"0 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         0,      0,      true,       false,      true});
        dataList.add(new Object[]{"0 sec interval " +
                                  "mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   0,      8,      true,       false,      false});
        dataList.add(new Object[]{"0 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "fail quote",         0,      0,      false,      true,       true});
        dataList.add(new Object[]{"0 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira sleep " +
                                  "don't fail quote",   0,      8,      false,      true,       false});
        dataList.add(new Object[]{"0 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "fail quote",         0,      0,      false,      false,      true});
        dataList.add(new Object[]{"0 sec interval " +
                                  "don't mock PDOEjb " +
                                  "jira no sleep " +
                                  "don't fail quote",   0,      8,      false,      false,      false});

        Object[][] output = null;

        if(!dpCallCount.containsKey(method.getName())) {
            dpCallCount.put(method.getName(), 0);
        }

        if (inContainer) {
            output = new Object[][]{
                    dataList.get(dpCallCount.get(method.getName()))
            };
            dpCallCount.put (method.getName(), (dpCallCount.get(method.getName()) + 1) % dataList.size());
        } else {
            output = dataList.toArray(new Object[dataList.size()][]);
        }

        return output;
    }
}
