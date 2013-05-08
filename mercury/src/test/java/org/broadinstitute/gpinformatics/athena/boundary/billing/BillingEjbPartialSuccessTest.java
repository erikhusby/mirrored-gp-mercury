package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
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
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.NullOrEmptyCollection.nullOrEmptyCollection;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.SuccessfullyBilled.successfullyBilled;
import static org.broadinstitute.gpinformatics.infrastructure.matchers.UnsuccessfullyBilled.unsuccessfullyBilled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class BillingEjbPartialSuccessTest extends Arquillian {

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

    private static String FAILING_PRICE_ITEM_NAME;
    public static final String SM_1234 = "SM-1234";
    public static final String SM_5678 = "SM-5678";

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
            if (FAILING_PRICE_ITEM_NAME.equals(quotePriceItem.getName())) {
                throw new RuntimeException("Intentional Work Registration Failure!");
            }

            return "workItemId\t1000";
        }

        @Override
        public Quote getQuoteByAlphaId(String alphaId) throws QuoteServerException, QuoteNotFoundException {
            throw new NotImplementedException();
        }
    }


    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.TEST,
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
     */
    private BillingSession writeFixtureData() {
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, SM_1234, SM_5678);

        UUID uuid = UUID.randomUUID();
        PriceItem replacementPriceItem = new PriceItem(uuid.toString(), "Genomics Platform", "Testing Category",
                "Replacement PriceItem Name " + uuid);
        FAILING_PRICE_ITEM_NAME = replacementPriceItem.getName();
        billingSessionDao.persist(replacementPriceItem);

        Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

        ProductOrderSample sm1234 = samplesByName.get(SM_1234).iterator().next();
        ProductOrderSample sm5678 = samplesByName.get(SM_5678).iterator().next();

        LedgerEntry ledgerEntry1234 =
                new LedgerEntry(sm1234, productOrder.getProduct().getPrimaryPriceItem(), new Date(), 3);

        LedgerEntry ledgerEntry5678 = new LedgerEntry(sm5678, replacementPriceItem, new Date(), 5);

        BillingSession billingSession = new BillingSession(-1L, Sets.newHashSet(ledgerEntry1234, ledgerEntry5678));
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
    public void testPositive() {

        BillingSession billingSession = writeFixtureData();
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
            if (SM_1234.equals(ledgerEntry.getProductOrderSample().getSampleName())) {
                assertThat(ledgerEntry, is(successfullyBilled()));
            }
            if (SM_5678.equals(ledgerEntry.getProductOrderSample().getSampleName())) {
                assertThat(ledgerEntry, is(unsuccessfullyBilled()));
            }
        }
    }
}
