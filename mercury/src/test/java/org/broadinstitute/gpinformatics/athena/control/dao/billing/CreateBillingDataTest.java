package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class CreateBillingDataTest extends Arquillian {

    @Inject
    private ProductOrderDao productOrderDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction userTransaction;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    @Test(enabled = false)
    public void createData() {

        try {

            userTransaction.begin();

            String pdoBusinessKey = "PDO-72";
            ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoBusinessKey);

            Set<LedgerEntry> ledgerEntries = new HashSet<>();
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                if (productOrderSample.getName().contains("A")) {
                    if(productOrder.hasSapQuote()) {
                        ledgerEntries.add(new LedgerEntry(productOrderSample,
                                productOrder.getProduct(),
                                new Date(), 0.5));
                    } else {
                        ledgerEntries.add(new LedgerEntry(productOrderSample,
                                productOrder.getProduct().getPrimaryPriceItem(), new Date(), 0.5));
                    }
                }
            }

            BillingSession billingSession = new BillingSession(11144L, ledgerEntries);
            productOrderDao.persist(billingSession);

            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                if (productOrderSample.getName().contains("B")) {
                    if(productOrder.hasSapQuote()) {
                        productOrderDao.persist(
                                new LedgerEntry(productOrderSample, productOrder.getProduct(),
                                        new Date(), 1.1));
                    } else {
                        productOrderDao.persist(
                                new LedgerEntry(productOrderSample, productOrder.getProduct().getPrimaryPriceItem(),
                                        new Date(), 1.1));
                    }
                }
            }

            userTransaction.commit();

        } catch (NotSupportedException | HeuristicMixedException | RollbackException | HeuristicRollbackException | SystemException e) {
            throw new RuntimeException(e);
        }

    }

}
