package org.broadinstitute.gpinformatics.athena.entity.fixup;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingLedgerDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingLedger;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.persistence.Query;

import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class BillingLedgerFixupTest extends Arquillian {

    @Inject
    private BillingLedgerDao billingLedgerDao;

    // When you run this on prod, change to PROD and prod
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }


    @Test(enabled = false)
    public void testBackfillLedgerQuotes() {

        for (BillingLedger ledger : billingLedgerDao.findAll()) {
            ledger.getProductOrderSample().getProductOrder().getQuoteId();

        }
    }
}
