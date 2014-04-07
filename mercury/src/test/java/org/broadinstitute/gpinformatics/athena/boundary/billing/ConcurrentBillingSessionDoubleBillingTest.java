package org.broadinstitute.gpinformatics.athena.boundary.billing;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.PMBQuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceList;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteNotFoundException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePlatformType;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteServerException;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.quote.Quotes;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import javax.xml.parsers.ParserConfigurationException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Regression test to verify that it's not possible to double bill.
 * If this test fails, we're at risk for double billing, which is
 * a very, very bad thing.  See https://gpinfojira.broadinstitute.org:8443/jira/browse/GPLIM-2501
 * for an example of double billing.
 */
public class ConcurrentBillingSessionDoubleBillingTest extends ConcurrentBaseTest {

    private static final Log logger = LogFactory.getLog(ConcurrentBillingSessionDoubleBillingTest.class);

    private static final String BILLING_SESSION_ID = "BILL-2489";

    @Inject
    BillingSessionDao billingSessionDao;

    @Inject
    BillingSessionAccessEjb billingSessionAccessEjb;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction utx;

    private static AtomicInteger numBillingThreadsRun = new AtomicInteger();

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DummyPMBQuoteService.class,
                RegisterWorkAlwaysWorks.class);
    }

    /**
     * Take the hardcoded billing session and make it look like it's ready for billing
     */
    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container.
        if (utx == null) {
            return;
        }

        utx.begin();
        BillingSession billingSession = billingSessionDao.findByBusinessKey(BILLING_SESSION_ID);
        for (LedgerEntry ledgerEntry : billingSession.getLedgerEntryItems()) {
            ledgerEntry.setBillingMessage("test flameout");
            billingSession.setBilledDate(null);
            Assert.assertFalse(ledgerEntry.isBilled(),
                    "If ledger entry is considered billed, this test will not try to bill it, which means we may be at risk of double billing.");
        }
        billingSessionDao.persist(billingSession);
        utx.commit();
    }

    /**
     * Attempts to bill for the same billing session at the same time
     * using two threads.  One thread should succeed, the other should
     * fail.
     */
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testMultithreaded() throws Exception {
        Throwable billingError = null;
        PDOLookupThread pdoLookupThread = new PDOLookupThread();
        PDOLookupThread pdoLookupThread2 = new PDOLookupThread();
        Thread thread1 = new Thread(pdoLookupThread);
        Thread thread2 = new Thread(pdoLookupThread2);

        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();

        int numErrors = 0;
        if (pdoLookupThread.getError() != null) {
            billingError = pdoLookupThread.getError();
            numErrors++;
        }
        if (pdoLookupThread2.getError() != null) {
            billingError = pdoLookupThread2.getError();
            numErrors++;
        }

        Assert.assertNotEquals(numErrors,2,"Only one of the two billing sessions should have errored out.  Is the jndi lookup not working?  Check the server side logs.");
        Assert.assertEquals(numErrors, 1, "Only one of the two billing session should have completed without error.  We may have re-introduced a double billing bug.");
        Assert.assertEquals(RegisterWorkAlwaysWorks.workItemNumber,1,"Only one session should have hit the quote server.  We are at risk of double billing in the quote server.");
        Assert.assertFalse(billingSessionAccessEjb.isSessionLocked(BILLING_SESSION_ID));

        utx.begin();
        billingSessionDao.clear();
        try {
            BillingSession billingSession = billingSessionDao.findByBusinessKey(BILLING_SESSION_ID);
            for (LedgerEntry ledgerEntry : billingSession.getLedgerEntryItems()) {
                Assert.assertTrue(ledgerEntry.isSuccessfullyBilled(),BILLING_SESSION_ID + " should have been billed properly and committed as a result of this test.");
            }
        }
        finally {
            utx.rollback();
        }
        Assert.assertTrue(billingError.getClass().equals(BillingException.class),"The session that error'd out should have thrown a BillingException");
        Assert.assertEquals(billingError.getMessage(),BillingEjb.LOCKED_SESSION_TEXT);
        Assert.assertEquals(numBillingThreadsRun.get(), 2, "Both billing threads should be run to verify the fix for double billing.  We are at risk for double billing.");

    }

    public class PDOLookupThread implements Runnable {

        private Throwable error;

        @Override
        public void run() {
            BillingAdaptor billingAdaptor = null;
            ContextControl ctxCtrl = BeanProvider.getContextualReference(ContextControl.class);
            ctxCtrl.startContext(RequestScoped.class);
            ctxCtrl.startContext(SessionScoped.class);
            try {
                billingAdaptor = getBeanFromJNDI(BillingAdaptor.class);
                numBillingThreadsRun.incrementAndGet();
                billingAdaptor.billSessionItems("whatever", BILLING_SESSION_ID);
            } catch(RuntimeException e) {
                error = e;
                throw e;
            } finally {
                try {
                    ctxCtrl.stopContext(SessionScoped.class);
                    ctxCtrl.stopContext(RequestScoped.class);
                }
                catch(Throwable t) {
                    // not much to be done about this!
                    logger.error("Failed to stop context",t);
                }
            }
        }

        public Throwable getError() {
            return error;
        }
    }

    @Alternative
    private static class RegisterWorkAlwaysWorks implements QuoteService {

        public static int workItemNumber;

        @Override
        public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
            return new PriceList();
        }

        @Override
        public Quotes getAllSequencingPlatformQuotes() throws QuoteServerException, QuoteNotFoundException {
            return null;
        }

        @Override
        public String registerNewWork(Quote quote, QuotePriceItem quotePriceItem, QuotePriceItem itemIsReplacing,
                                      Date reportedCompletionDate, double numWorkUnits, String callbackUrl,
                                      String callbackParameterName, String callbackParameterValue) {

            try {
                // sleep here for a while to increase the likelihood that the vm really does try to call bill() at the same time
                Thread.sleep(1000);
            }
            catch(InterruptedException e) {}
            return Integer.toString(++workItemNumber);
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            return null;
        }
    }

    @Alternative
    private static class DummyPMBQuoteService implements PMBQuoteService {
        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            return new Quote();
        }

        @Override
        public Set<Funding> getAllFundingSources()
                throws QuoteServerException, QuoteNotFoundException, ParserConfigurationException {
            return new HashSet<>();
        }

        @Override
        public PriceList getPlatformPriceItems(QuotePlatformType quotePlatformType)
                throws QuoteServerException, QuoteNotFoundException {
            return new PriceList();
        }

        @Override
        public PriceList getAllPriceItems() throws QuoteServerException, QuoteNotFoundException {
            return new PriceList();
        }

        @Override
        public Quotes getAllQuotes() throws QuoteServerException, QuoteNotFoundException {
            return new Quotes();
        }
    }

}