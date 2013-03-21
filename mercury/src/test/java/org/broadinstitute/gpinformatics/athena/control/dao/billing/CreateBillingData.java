package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.LedgerEntry;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Test(enabled = false)
public class CreateBillingData extends ContainerTest {

    @Inject
    private ProductOrderDao productOrderDao;

    @Inject
    private UserTransaction userTransaction;

    public void createData() {

        try {

            userTransaction.begin();

            String pdoBusinessKey = "PDO-72";
            ProductOrder productOrder = productOrderDao.findByBusinessKey(pdoBusinessKey);

            Set<LedgerEntry> ledgerEntries = new HashSet<LedgerEntry>();
            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                if (productOrderSample.getSampleName().contains("A")) {
                    ledgerEntries.add(new LedgerEntry(productOrderSample, productOrder.getProduct().getPrimaryPriceItem(),
                            new Date(), 0.5));
                }
            }

            BillingSession billingSession = new BillingSession(11144L, ledgerEntries);
            productOrderDao.persist(billingSession);

            for (ProductOrderSample productOrderSample : productOrder.getSamples()) {
                if (productOrderSample.getSampleName().contains("B")) {
                    productOrderDao.persist(new LedgerEntry(productOrderSample, productOrder.getProduct().getPrimaryPriceItem(),
                            new Date(), 1.1));
                }
            }

            userTransaction.commit();

        } catch (NotSupportedException e) {
            throw new RuntimeException(e);
        } catch (SystemException e) {
            throw new RuntimeException(e);
        } catch (HeuristicRollbackException e) {
            throw new RuntimeException(e);
        } catch (RollbackException e) {
            throw new RuntimeException(e);
        } catch (HeuristicMixedException e) {
            throw new RuntimeException(e);
        }

    }

}
