package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.apache.commons.lang.NotImplementedException;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
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
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.net.URL;
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

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;

public class BillingEjbDoubleBillingTest extends Arquillian {

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

    private static int quoteCount;
    private static int totalItems;

    private static boolean cycleFails = true;
    private static BillingEjbPartialSuccessTest.Result lastResult = BillingEjbPartialSuccessTest.Result.FAIL;
    private ClientConfig clientConfig;
    private BillingSession billingSession;


//    @Override
//    protected String getResourcePath() {
//        return "billing/session.action?bill=&";
//    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV,
                PartiallySuccessfulQuoteServiceStub.class);
    }


    @Alternative
    protected static class PartiallySuccessfulQuoteServiceStub implements QuoteService {

        private static final long serialVersionUID = 6093273925949722169L;

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
            String workId = "workItemId\t1000";

            if (cycleFails) {
                switch (lastResult) {
                case FAIL:
                    lastResult = BillingEjbPartialSuccessTest.Result.SUCCESS;
                    workId = "workItemID" + (new Date()).getTime();
                    break;

                case SUCCESS:
                    lastResult = BillingEjbPartialSuccessTest.Result.FAIL;
                    throw new RuntimeException("Intentional Work Registration Failure");
                }
            }
                quoteCount++;
                /*log.debug*/System.out.println("Quote count is now " + quoteCount);
                assertThat(quoteCount, is(lessThanOrEqualTo(totalItems)));
            return workId;
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }
    }

    private BillingSession writeFixtureDataOneSamplePerProductOrder(String... orderSamples) {

        Set<LedgerEntry> billingSessionEntries = new HashSet<>(orderSamples.length);
        for (String sample : orderSamples) {
            ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, sample);

            Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

            for (ProductOrderSample ledgerSample : samplesByName.get(sample)) {

                    billingSessionEntries.add(new LedgerEntry(ledgerSample,
                            productOrder.getProduct().getPrimaryPriceItem(), new Date(), 3));
            }
        }
        BillingSession billingSession = new BillingSession(-1L, billingSessionEntries);
        billingSessionDao.persist(billingSession);

        billingSessionDao.flush();
        billingSessionDao.clear();

        return billingSession;
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void initiationTest() throws Exception {
        clientConfig = new DefaultClientConfig();

        String[] sampleNameList = {"SM-2342", "SM-9291", "SM-2349", "SM-9944", "SM-4444", "SM-4441", "SM-1112",
                "SM-4488"};

        cycleFails = false;

        quoteCount = 0;
        totalItems = sampleNameList.length;


        /*log.debug*/System.out.println("Building Billing session from samples");
        billingSession = writeFixtureDataOneSamplePerProductOrder(sampleNameList);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, dataProvider = ARQUILLIAN_DATA_PROVIDER, enabled = true,
            dependsOnMethods = "initiationTest")
    @RunAsClient
    public void testMultipleThreadFailure(@ArquillianResource final URL baseUrl) throws Exception {

        final int threadExecutionCount = 2;
        System.out.println("Executing Url of: " +baseUrl + "Mercury/" + "billing/session.action?bill=&" +
                           "/" + billingSession.getBusinessKey());

        Callable<String> billingTask = new Callable<String>() {
            @Override
            public String call() throws Exception {
//                Thread.sleep(1000);
                WebResource webResource =
                        Client.create(clientConfig).resource(baseUrl + "Mercury/" + "billing/session.action?bill=&" +
                                                             "/" + billingSession.getBusinessKey());
                return webResource .type(
                        MediaType.TEXT_HTML)
                        .accept(MediaType.TEXT_HTML).post(String.class);
            }
        };

        /*log.debug*/System.out.println("Creating copies of tasks");
        List<Callable<String>> tasks = Collections.nCopies(threadExecutionCount, billingTask);

        /*log.debug*/System.out.println("creating Executors for pool of threads");
        ExecutorService executorService = Executors.newFixedThreadPool(threadExecutionCount);

        /*log.debug*/System.out.println("Invoking all thread processes");
        List<Future<String>> futures = executorService.invokeAll(tasks);
        /*log.debug*/System.out.println("Threads should be complete");
//        List<BillingEjb.BillingResult> failingResults = new ArrayList<>(sampleNameList.length);
//        List<BillingEjb.BillingResult> passingResults = new ArrayList<>(sampleNameList.length);
        for (Future<String> futureItem : futures) {
//            futureItem.get();

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
