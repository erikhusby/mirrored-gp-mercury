package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
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
import org.testng.annotations.Test;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.NullOrEmptyCollection.nullOrEmptyCollection;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.SuccessfullyBilled.successfullyBilled;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.UnsuccessfullyBilled.unsuccessfullyBilled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class BillingEjbPartialSuccessTest extends Arquillian {

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

    @Inject
    private PriceListCache priceListCache;

    private static String FAILING_PRICE_ITEM_NAME = "";
    private static String FAILING_PRICE_ITEM_SAMPLE = "";
    public static final String SM_1234 = "SM-1234";
    public static final String SM_5678 = "SM-5678";

    private static int quoteCount;
    private static int totalItems;

    private static boolean cycleFails = true;
    private static Result lastResult = Result.FAIL;

    public enum Result {FAIL, SUCCESS}

    public static final Log log = LogFactory.getLog(BillingEjbPartialSuccessTest.class);
    private static final Object lockBox = new Object();

    /**
     * This will succeed in billing some but not all work to make sure our Billing Session is left in the state we
     * expect.
     */
    @Alternative
    private static class PartiallySuccessfulQuoteServiceStub implements QuoteService {

        @Override
        public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }

        @Override
        public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }

        @Override
        public String registerNewWork(Quote quote,
                                      QuotePriceItem quotePriceItem,
                                      QuotePriceItem itemIsReplacing,
                                      Date reportedCompletionDate,
                                      double numWorkUnits,
                                      String callbackUrl, String callbackParameterName, String callbackParameterValue) {
            // Simulate failure only for one particular PriceItem.
            System.out.println("In register New work");
            if (FAILING_PRICE_ITEM_NAME.equals(quotePriceItem.getName())) {
                throw new RuntimeException("Intentional Work Registration Failure!");
            }
            String workId = "workItemId\t1000";

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
                /*log.debug*/System.out.println("Quote count is now " + quoteCount);
                assertThat(quoteCount, is(lessThanOrEqualTo(totalItems)));
            }
            return workId;
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV,
                PartiallySuccessfulQuoteServiceStub.class);
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
    private BillingSession writeFixtureData(String... orderSamples) {

        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, orderSamples);
        quoteCount = 0;
        totalItems = 1;
        Set<LedgerEntry> billingSessionEntries = new HashSet<>(productOrder.getSamples().size());

        Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

        for (String sampleName : orderSamples) {
            for (ProductOrderSample ledgerSample : samplesByName.get(sampleName)) {

                if (FAILING_PRICE_ITEM_SAMPLE.equals(sampleName)) {
                    UUID uuid = UUID.randomUUID();
                    PriceItem replacementPriceItem =
                            new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category",
                                    "Replacement PriceItem Name " + uuid);
                    FAILING_PRICE_ITEM_NAME = replacementPriceItem.getName();
                    billingSessionDao.persist(replacementPriceItem);

                    billingSessionEntries.add(new LedgerEntry(ledgerSample, replacementPriceItem, new Date(), 5));
                } else {
                    billingSessionEntries.add(new LedgerEntry(ledgerSample,
                            productOrder.getProduct().getPrimaryPriceItem(), new Date(), 3));
                }
            }
        }

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
    private BillingSession writeFixtureDataOneSamplePerProductOrder(String... orderSamples) {

        Set<LedgerEntry> billingSessionEntries = new HashSet<>(orderSamples.length);
        for (String sample : orderSamples) {
            ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, sample);

            Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

            for (ProductOrderSample ledgerSample : samplesByName.get(sample)) {

                if (FAILING_PRICE_ITEM_SAMPLE.equals(sample)) {
                    UUID uuid = UUID.randomUUID();
                    PriceItem replacementPriceItem =
                            new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category",
                                    "Replacement PriceItem Name " + uuid);
                    FAILING_PRICE_ITEM_NAME = replacementPriceItem.getName();
                    billingSessionDao.persist(replacementPriceItem);

                    billingSessionEntries.add(new LedgerEntry(ledgerSample, replacementPriceItem, new Date(), 5));
                } else {
                    billingSessionEntries.add(new LedgerEntry(ledgerSample,
                            productOrder.getProduct().getPrimaryPriceItem(), new Date(), 3));
                }
            }
        }
        BillingSession billingSession = new BillingSession(-1L, billingSessionEntries);
        billingSessionDao.persist(billingSession);

        billingSessionDao.flush();
        billingSessionDao.clear();

        return billingSession;
    }

    /**
     * <ul>
     * <li>Create the fixture data per {@link #writeFixtureData()}.</li>
     * <li>Load the BillingSession and call a version of the BillingEjb method that <b>does</b> call DAO methods
     * inside a transactionally demarcated method, which should cause the EntityManager to be enrolled in the
     * transaction.</li>
     * <li>Clear the entity manager again and load the BillingSession, confirm that the billing messages were
     * persisted.</li>
     * </ul>
     */
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testPositive() {

        cycleFails = false;

        FAILING_PRICE_ITEM_SAMPLE = SM_5678;
        BillingSession billingSession = writeFixtureData(new String[]{SM_1234, SM_5678});
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        billingEjb.bill("http://www.broadinstitute.org", billingSession.getBusinessKey());

        billingSessionDao.clear();
        // Re-fetch the updated BillingSession from the database.
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        // The BillingSession should exist as this billing attempt should have been partially successful.
        assertThat(billingSession, is(not(nullValue())));

        List<LedgerEntry> ledgerEntryItems = billingSession.getLedgerEntryItems();
        assertThat(ledgerEntryItems, is(not(nullOrEmptyCollection())));
        assertThat(ledgerEntryItems, hasSize(2));

        for (LedgerEntry ledgerEntry : billingSession.getLedgerEntryItems()) {
            if (SM_1234.equals(ledgerEntry.getProductOrderSample().getName())) {
                assertThat(ledgerEntry, is(successfullyBilled()));
            }
            if (SM_5678.equals(ledgerEntry.getProductOrderSample().getName())) {
                assertThat(ledgerEntry, is(unsuccessfullyBilled()));
            }
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testMultipleFailure() {

        String[] sampleNameList = {"SM-2342", "SM-9291", "SM-2349", "SM-9944", "SM-4444", "SM-4441", "SM-1112",
                "SM-4488"};
        cycleFails = true;
        lastResult = Result.FAIL;
        quoteCount = 0;
        totalItems = sampleNameList.length;

        BillingSession billingSession = writeFixtureDataOneSamplePerProductOrder(sampleNameList);
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        billingEjb.bill("http://www.broadinstitute.org", billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        List<QuoteImportItem> quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);

        assertThat("the size of the unBilled items should equal half the size of the total items to be billed",
                quoteImportItems.size(), is(equalTo(sampleNameList.length / 2)));
        billingEjb.bill("http://www.broadinstitute.org", billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);

        assertThat("the size of the unBilled items should equal half the size of the total items to be billed",
                quoteImportItems.size(), is(equalTo(sampleNameList.length / 4)));

        cycleFails = false;
        billingEjb.bill("http://www.broadinstitute.org", billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);

        assertThat("the size of the unBilled items should be zero",
                quoteImportItems.size(), is(equalTo(0)));
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void testNoForcedFailures() {

        System.out.println("Running no forced failures threaded");

        String[] sampleNameList = {"SM-2342", "SM-9291", "SM-2349", "SM-9944", "SM-4444", "SM-4441", "SM-1112",
                "SM-4488"};
        cycleFails = false;
        lastResult = Result.FAIL;
        quoteCount = 0;
        totalItems = sampleNameList.length;


        BillingSession billingSession = writeFixtureDataOneSamplePerProductOrder(sampleNameList);
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        billingEjb.bill("http://www.broadinstitute.org", billingSession.getBusinessKey());

        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        List<QuoteImportItem> quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void testMultipleThreadFailure() throws Exception {

        String[] sampleNameList = {"SM-2342", "SM-9291", "SM-2349", "SM-9944", "SM-4444", "SM-4441", "SM-1112",
                "SM-4488"};

        cycleFails = false;

        quoteCount = 0;
        totalItems = sampleNameList.length;

        /*log.debug*/System.out.println("Building Billing session from samples");
        final BillingSession billingSession = writeFixtureDataOneSamplePerProductOrder(sampleNameList);
        final int threadExecutionCount = 2;

        Callable<List<BillingEjb.BillingResult>> billingTask = new Callable<List<BillingEjb.BillingResult>>() {
            @Override
            public List<BillingEjb.BillingResult> call() throws Exception {
                Thread.sleep(1000);
                return billingEjb.bill("http://www.broadinstitute.org", billingSession.getBusinessKey());
            }
        };

        /*log.debug*/System.out.println("Creating copies of tasks");
        List<Callable<List<BillingEjb.BillingResult>>> tasks = Collections.nCopies(threadExecutionCount, billingTask);

        /*log.debug*/System.out.println("creating Executors for pool of threads");
        ExecutorService executorService = Executors.newFixedThreadPool(threadExecutionCount);

        /*log.debug*/System.out.println("Invoking all thread processes");
        List<Future<List<BillingEjb.BillingResult>>> futures = executorService.invokeAll(tasks);
        /*log.debug*/System.out.println("Threads should be complete");
//        List<BillingEjb.BillingResult> failingResults = new ArrayList<>(sampleNameList.length);
//        List<BillingEjb.BillingResult> passingResults = new ArrayList<>(sampleNameList.length);
        for (Future<List<BillingEjb.BillingResult>> futureItem : futures) {
            futureItem.get();
//            for (BillingEjb.BillingResult billingResult : futureItem.get()) {
//                if (billingResult.isError()) {
//                    failingResults.add(billingResult);
//                } else {
//                    passingResults.add(billingResult);
//                }
//            }
        }
//
//        assertThat(failingResults.size(), is(equalTo(sampleNameList.length)));
//        assertThat(passingResults.size(), is(equalTo(sampleNameList.length)));
    }
}