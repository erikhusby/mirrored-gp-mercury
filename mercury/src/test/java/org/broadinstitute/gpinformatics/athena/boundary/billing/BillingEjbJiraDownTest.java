package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.test.withdb.ProductOrderDBTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.matchers.SuccessfullyBilled.successfullyBilled;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

@Test(groups = TestGroups.ALTERNATIVES, enabled = true)
public class BillingEjbJiraDownTest extends Arquillian {

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingAdaptor billingAdaptor;

    @Deployment
    public static WebArchive buildMercuryDeployment() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(AcceptsAllWorkRegistrationsQuoteServiceStub.class, AlwaysThrowsRuntimeExceptionsJiraStub.class);
    }


    private String writeFixtureData() {

        final String SM_A = "SM-1234A";
        final String SM_B = "SM-1234B";
        ProductOrder productOrder = ProductOrderDBTestFactory.createProductOrder(billingSessionDao, SM_A, SM_B);

        Multimap<String, ProductOrderSample> samplesByName = ProductOrderTestFactory.groupBySampleId(productOrder);

        final ProductOrderSample sampleA = samplesByName.get(SM_A).iterator().next();
        final ProductOrderSample sampleB = samplesByName.get(SM_B).iterator().next();

        LedgerEntry ledgerEntryA =
                new LedgerEntry(sampleA, productOrder.getProduct().getPrimaryPriceItem(), new Date(), 3);
        LedgerEntry ledgerEntryB =
                new LedgerEntry(sampleB, productOrder.getProduct().getPrimaryPriceItem(), new Date(), 3);

        BillingSession
                billingSession = new BillingSession(-1L, Sets.newHashSet(ledgerEntryA, ledgerEntryB));
        billingSessionDao.persist(billingSession);

        billingSessionDao.flush();
        billingSessionDao.clear();

        return billingSession.getBusinessKey();
    }


    public void test() {

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
