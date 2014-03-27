package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import junit.framework.Assert;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ConcurrentProductOrderDoubleCreateTest;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
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

import javax.ejb.EJBTransactionRolledbackException;
import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
public class BillingEjbJiraDelayedTest extends Arquillian {

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

    @Inject
    private BillingAdaptor billingAdaptor;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    UserTransaction utx;

    private static final Log log = LogFactory.getLog(BillingEjbJiraDelayedTest.class);

    private static AtomicInteger failureTime = new AtomicInteger(0);

    @Alternative
    protected static class DelayedJiraService extends
            ConcurrentProductOrderDoubleCreateTest.ControlBusinessKeyJiraService {
        @Override
        public JiraIssue getIssue(String key) throws IOException {
            try {
                Thread.sleep(1000 * 16);
            } catch (InterruptedException e) {
            }

            UUID uuid = UUID.randomUUID();
            String jiraKey = "PDO-" + uuid;

            return new JiraIssue(jiraKey, this);
        }
    }

    @Alternative
    protected static class QuoteServiceStubWithWait implements QuoteService {

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
            try {
                failureTime.getAndAdd(1);
                log.error("Setting Sleep of " + failureTime.get());
                Thread.sleep(1000 * failureTime.get());
                log.error("Woke up from quote call");
            } catch (InterruptedException e) {
            }

            return workId;
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
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
     * @param orderSamples
     */
    protected BillingSession writeFixtureDataOneSamplePerProductOrder(String... orderSamples) {

        Set<LedgerEntry> billingSessionEntries = new HashSet<>(orderSamples.length);
        for (String sample : orderSamples) {
            ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, sample);

            Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

            for (ProductOrderSample ledgerSample : samplesByName.get(sample)) {

                UUID uuid = UUID.randomUUID();
                PriceItem replacementPriceItem =
                        new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category",
                                      "Replacement PriceItem Name " + uuid);
                billingSessionDao.persist(replacementPriceItem);

                billingSessionEntries.add(new LedgerEntry(ledgerSample, replacementPriceItem, new Date(), 5));
            }
        }
        BillingSession billingSession = new BillingSession(-1L, billingSessionEntries);
        billingSessionDao.persist(billingSession);

        billingSessionDao.flush();
        billingSessionDao.clear();

        return billingSession;
    }


    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV,
                DelayedJiraService.class, QuoteServiceStubWithWait.class);
    }

    public void testTransactionTimeout() throws Exception {
        String[] sampleNameList = {"SM-2342", "SM-9291", "SM-2349", "SM-9944", "SM-4444", "SM-4441", "SM-1112",
                "SM-4488"};

        BillingSession billingSession = writeFixtureDataOneSamplePerProductOrder(sampleNameList);

        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

//        utx.setTransactionTimeout(5);
//        utx.begin();

        try {
            billingAdaptor.billSessionItems("http://www.broadinstitute.org", billingSession.getBusinessKey());
        } catch (EJBTransactionRolledbackException e) {
            log.error("Exception thrown calling facade bill", e);
            e.printStackTrace();
        }

//        if(utx.getStatus() == Status.STATUS_NO_TRANSACTION) {
//            utx.begin();
//        }
        billingSessionDao.clear();
        billingSession = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        List<QuoteImportItem> quoteImportItems = billingSession.getUnBilledQuoteImportItems(priceListCache);
        Assert.assertTrue(quoteImportItems.isEmpty());
//        if(utx.getStatus() == Status.STATUS_ACTIVE) {
//            utx.commit();
//        }
    }
}
