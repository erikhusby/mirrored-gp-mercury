package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.broadinstitute.gpinformatics.athena.boundary.infrastructure.SAPAccessControlEjb;
import org.broadinstitute.gpinformatics.athena.boundary.orders.ProductOrderEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuotePriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteService;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapConfig;
import org.broadinstitute.gpinformatics.infrastructure.sap.SapIntegrationServiceImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.broadinstitute.sap.services.SAPIntegrationException;
import org.broadinstitute.sap.services.SapIntegrationClientImpl;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.mockito.Mockito;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.SuccessfullyBilled.successfullyBilled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.ALTERNATIVES)
@Dependent
public class BillingEjbJiraDownTest extends Arquillian {

    public BillingEjbJiraDownTest(){}

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

    @Inject
    private PriceListCache priceListCache;

    @Inject
    private QuoteService quoteService;

    @Inject
    private BillingSessionAccessEjb billingSessionAccessEjb;

    private SapIntegrationServiceImpl sapService;

    @Inject
    private ProductOrderEjb productOrderEjb;

    @Inject
    private org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment deployment;

    private BillingAdaptor billingAdaptor;

    private SAPProductPriceCache productPriceCache;

    @Inject
    private SAPAccessControlEjb accessControlEjb;

    @Deployment
    public static WebArchive buildMercuryDeployment() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(AcceptsAllWorkRegistrationsQuoteServiceStub.class,
            AlwaysThrowsRuntimeExceptionsJiraStub.class);
    }

    private String writeFixtureData() throws SAPIntegrationException {

        final String SM_A = "SM-" + (new Date()).getTime();
        final String SM_B = "SM-" + ((new Date()).getTime() + 1);
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, SM_A, SM_B);

        Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

        final ProductOrderSample sampleA = samplesByName.get(SM_A).iterator().next();
        final ProductOrderSample sampleB = samplesByName.get(SM_B).iterator().next();

        LedgerEntry ledgerEntryA;
        LedgerEntry ledgerEntryB;
        if(productOrder.hasSapQuote()) {
            ledgerEntryA = new LedgerEntry(sampleA, productOrder.getProduct(), new Date(), 3);
            ledgerEntryB = new LedgerEntry(sampleB, productOrder.getProduct(), new Date(), 3);
        } else {
            ledgerEntryA = new LedgerEntry(sampleA, productOrder.getProduct().getPrimaryPriceItem(), new Date(),
                    3);
            ledgerEntryB = new LedgerEntry(sampleB, productOrder.getProduct().getPrimaryPriceItem(), new Date(),
                    3);
        }

        final Collection<QuotePriceItem> quotePriceItems = priceListCache.getQuotePriceItems();

        final Set<PriceItem> primaryPriceItems = Collections.singleton(productOrder.getProduct().getPrimaryPriceItem());
        for(PriceItem primaryPriceItem: primaryPriceItems) {
            quotePriceItems.add(new QuotePriceItem(primaryPriceItem.getCategory(),
                    primaryPriceItem.getName() + "_id", primaryPriceItem.getName(),
                    "250", "each", primaryPriceItem.getPlatform()));
        }

        PriceListCache tempPriceListCache = new PriceListCache(quotePriceItems);

        QuoteService mockQuoteService = Mockito.mock(QuoteService.class);
        SAPAccessControlEjb mockAccessController = Mockito.mock(SAPAccessControlEjb.class);
        sapService = new SapIntegrationServiceImpl(
            SapConfig.produce(org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV),
                Mockito.mock(BSPUserList.class), Mockito.mock(PriceListCache.class),
            productPriceCache, mockAccessController);
        productPriceCache = new SAPProductPriceCache(sapService);
        sapService.setProductPriceCache(productPriceCache);
        SapIntegrationClientImpl sapIntegrationClient = Mockito.mock(SapIntegrationClientImpl.class);
        sapService.setWrappedClient(sapIntegrationClient);

        billingAdaptor = new BillingAdaptor(billingEjb, tempPriceListCache, quoteService,
                billingSessionAccessEjb, sapService, productPriceCache, accessControlEjb);

        billingAdaptor.setProductOrderEjb(productOrderEjb);

        BillingSession
                billingSession = new BillingSession(-1L, Sets.newHashSet(ledgerEntryA, ledgerEntryB));
        billingSessionDao.persist(billingSession);

        billingSessionDao.flush();
        billingSessionDao.clear();

        return billingSession.getBusinessKey();
    }

    public void test() throws SAPIntegrationException {

        String businessKey = writeFixtureData();

        billingAdaptor.billSessionItems("http://www.broadinstitute.org", businessKey);

        billingSessionDao.clear();

        // Re-fetch the updated BillingSession from the database.
        BillingSession billingSession = billingSessionDao.findByBusinessKey(businessKey);

        assertThat(billingSession, is(not(nullValue())));

        List<LedgerEntry> ledgerEntryItems = billingSession.getLedgerEntryItems();
        assertThat(ledgerEntryItems, is(not(nullValue())));
        assertThat(ledgerEntryItems, hasSize(2));

        assertThat(ledgerEntryItems, everyItem(is(successfullyBilled())));

        // Make sure our angry JIRA was in fact angered by what we have done and has therefore thrown an exception that
        // threatened to roll back our transaction.
        assertThat(AlwaysThrowsRuntimeExceptionsJiraStub.getInvocationCount(), is(greaterThan(0)));
    }
}
