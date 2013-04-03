package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.athena.entity.products.PriceItem.*;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.NullOrEmptyCollection.nullOrEmptyCollection;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class BillingEjbPartialSuccessTest extends Arquillian {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private PriceItemDao priceItemDao;

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private Log logger;


    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.rollback();
    }


    /**
     * This will succeed in billing some but not all work to make sure our Billing Session is left in the state we
     * expect.
     */
    @Alternative
    private static class PartiallySuccessfulQuoteServiceStub implements QuoteService {

        private void newNotImplementedException(String methodName) {
            throw new NotImplementedException(
                    "PartiallySuccessfulQuoteServiceStub does not implement '" + methodName + "'.");
        }

        @Override
        public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
            newNotImplementedException("getAllPriceItems");
            return null;
        }

        @Override
        public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
            newNotImplementedException("getAllSequencingPlatformQuotes");
            return null;
        }

        @Override
        public String registerNewWork(Quote quote, org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem priceItem,
                                      Date reportedCompletionDate,
                                      double numWorkUnits,
                                      String callbackUrl, String callbackParameterName, String callbackParameterValue) {
            // Simulate failure only for ExEx Price Items.
            if (NAME_EXOME_EXPRESS.equals(priceItem.getName())) {
                throw new RuntimeException("Intentional Work Registration Failure!");
            }

            return "workItemId\t1000";
        }

        @Override
        public Quote getQuoteByNumericId(String numericId) throws QuoteServerException, QuoteNotFoundException {
            newNotImplementedException("getQuoteByNumericId");
            return null;
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            newNotImplementedException("getQuoteByAlphaId");
            return null;
        }
    }


    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(PartiallySuccessfulQuoteServiceStub.class);
    }


    public void test() {

        // We want a ProductOrder with multiple ProductOrderSamples for a Product with a Primary and at least one
        // Optional PriceItem.  Specifically, this test requires there to be one ExEx ProductOrder with more than one
        // PDO Sample.
        Collection<ProductOrder> productOrderList = Collections2.filter(
                productOrderDao.findByProductName(NAME_EXOME_EXPRESS),
                new Predicate<ProductOrder>() {
                    @Override
                    public boolean apply(ProductOrder productOrder) {
                        @SuppressWarnings("ConstantConditions")
                        List<ProductOrderSample> samples = productOrder.getSamples();
                        return samples.size() > 1;
                    }
                });

        assertThat(productOrderList, is(not(nullOrEmptyCollection())));
        ProductOrder productOrder = productOrderList.iterator().next();

        PriceItem exExPriceItem =
                priceItemDao.find(PLATFORM_GENOMICS, CATEGORY_EXOME_SEQUENCING_ANALYSIS, NAME_EXOME_EXPRESS);

        PriceItem standardExomePriceItem =
                priceItemDao.find(PLATFORM_GENOMICS, CATEGORY_EXOME_SEQUENCING_ANALYSIS, NAME_STANDARD_WHOLE_EXOME);

        @SuppressWarnings("ConstantConditions")
        List<ProductOrderSample> productOrderSamples = productOrder.getSamples();

        final LedgerEntry exExLedgerEntry =
                new LedgerEntry(productOrderSamples.get(0), exExPriceItem, new Date(), 5000);
        final LedgerEntry standardExLedgerEntry =
                new LedgerEntry(productOrderSamples.get(1), standardExomePriceItem, new Date(), 8000);

        Set<LedgerEntry> ledgerEntries = new HashSet<LedgerEntry>() {{
            add(exExLedgerEntry);
            add(standardExLedgerEntry);
        }};

        for (LedgerEntry ledgerEntry : ledgerEntries) {
            ledgerEntryDao.persist(ledgerEntry);
        }
        ledgerEntryDao.flush();

        BillingSession billingSession = new BillingSession(-1L, ledgerEntries);
        billingSessionDao.persist(billingSession);
        billingSessionDao.flush();

        // TODO Check that the results of the #bill call are consistent with what's going into the DB as these results
        // TODO are used to render the confirmation page.
        billingEjb.bill("http://www.broadinstitute.org", billingSession.getBusinessKey());

        // Re-fetch the updated BillingSession from the database.
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        // The BillingSession should exist as this billing attempt should have been partially successful.
        assertThat(billingSession, is(not(nullValue())));

        List<LedgerEntry> ledgerEntryItems = billingSession.getLedgerEntryItems();
        assertThat(ledgerEntryItems, is(not(nullOrEmptyCollection())));
        assertThat(ledgerEntryItems, hasSize(2));

        // Partition the persisted LedgerEntries by whether they represent the Exome Express Price Item or not.
        ImmutableListMultimap<Boolean,LedgerEntry> exExPartition =
                Multimaps.index(ledgerEntryItems, new Function<LedgerEntry, Boolean>() {
                    @Override
                    public Boolean apply(LedgerEntry ledgerEntry) {
                        @SuppressWarnings("ConstantConditions")
                        PriceItem priceItem = ledgerEntry.getPriceItem();
                        return NAME_EXOME_EXPRESS.equals(priceItem.getName());
                    }
                });

        LedgerEntry persistedExExLedgerEntry = exExPartition.get(true).get(0);
        LedgerEntry persistedStandardExLedgerEntry = exExPartition.get(false).get(0);

        assertThat(persistedStandardExLedgerEntry.getBillingMessage(), is(equalTo(BillingSession.SUCCESS)));
        assertThat(persistedExExLedgerEntry.getBillingMessage(), is(not(equalTo(BillingSession.SUCCESS))));

    }
}
