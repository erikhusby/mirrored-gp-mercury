package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Test(groups = TestGroups.STANDARD)
public class CreateBillingData extends ContainerTest {

    @Inject
    private ProductOrderDao productOrderDao;

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private UserTransaction userTransaction;

    @Test(enabled = false)
    public void createData() {

        try {

            userTransaction.begin();

            String pdoBusinessKey = "PDO-72";
            ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoBusinessKey);

            Set<LedgerEntry> ledgerEntries = new HashSet<>();
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                if (productOrderSample.getName().contains("A")) {
                    ledgerEntries.add(new LedgerEntry(productOrderSample, productOrder.getProduct().getPrimaryPriceItem(),
                            new Date(), 0.5));
                }
            }

            BillingSession billingSession = new BillingSession(11144L, ledgerEntries);
            productOrderDao.persist(billingSession);

            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                if (productOrderSample.getName().contains("B")) {
                    productOrderDao.persist(new LedgerEntry(productOrderSample, productOrder.getProduct().getPrimaryPriceItem(),
                            new Date(), 1.1));
                }
            }

            userTransaction.commit();

        } catch (NotSupportedException | HeuristicMixedException | RollbackException | HeuristicRollbackException | SystemException e) {
            throw new RuntimeException(e);
        }

    }

}
