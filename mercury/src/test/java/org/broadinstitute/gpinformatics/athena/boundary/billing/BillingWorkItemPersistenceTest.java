package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderSampleDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.PriceItemDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
public class BillingWorkItemPersistenceTest extends Arquillian {

    @Inject
    ProductOrderDao pdoDao;

    @Inject
    BillingSessionDao billingSessionDao;

    @Inject
    BillingAdaptor billingAdaptor;

    @Inject
    LedgerEntryDao ledgerEntryDao;

    @Inject
    PriceItemDao priceItemDao;

    @Inject
    ProductOrderSampleDao pdoSampleDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(AcceptsAllWorkRegistrationsQuoteServiceStub.class);
    }

    public void testThatBillingSavesTheWorkItemToDatabase() {
        ProductOrder pdo = pdoDao.findByBusinessKey("PDO-3510");
        Set<LedgerEntry> ledgerEntries = new HashSet<>();
        PriceItem priceItem = priceItemDao.findById(PriceItem.class,46L);
        for (ProductOrderSample pdoSample : pdo.getSamples()) {
            LedgerEntry ledgerEntry = new LedgerEntry(pdoSample,priceItem,new Date(),3);
            pdoSample.getLedgerItems().add(ledgerEntry);
            ledgerEntries.add(ledgerEntry);
            ledgerEntryDao.persist(ledgerEntry);
        }

        BillingSession billingSession = new BillingSession(-1L,ledgerEntries);
        billingSessionDao.persist(billingSession);
        billingSessionDao.flush();
        billingSessionDao.clear();

        billingAdaptor.billSessionItems("blah",billingSession.getBusinessKey());

        billingSessionDao.clear();
        BillingSession billingSessionReadFromDb = billingSessionDao.findByBusinessKey(billingSession.getBusinessKey());

        Assert.assertEquals(billingSessionReadFromDb.getLedgerEntryItems().size(),4);
        for (LedgerEntry ledgerEntry : billingSessionReadFromDb.getLedgerEntryItems()) {
            Assert.assertTrue(ledgerEntry.getWorkItem().contains(AcceptsAllWorkRegistrationsQuoteServiceStub.WORK_ITEM_PREPEND));
        }

    }

    // todo arz factory-ize billing session so you can create a billing session for pdoSamples
}
