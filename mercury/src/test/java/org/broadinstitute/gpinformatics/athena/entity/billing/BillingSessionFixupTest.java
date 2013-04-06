/**
 * Using this package for the fixup so that we can access the package protected constructor for setting the ID.
 */
package org.broadinstitute.gpinformatics.athena.entity.billing;

import clover.org.jfree.date.DateUtilities;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class BillingSessionFixupTest extends Arquillian {

    private static final Log logger = LogFactory.getLog(BillingSessionFixupTest.class);

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private BSPUserList userList;

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Largely copy/pasted from #backfillLedgerQuotes above, used to null out ledger quote IDs when we
     * discover we didn't assign them properly before and need to revise the assignments.
     */
    @Test(enabled = false)
    public void createCompleteBillingSession() {

        // Setting the date will end the session.
        Date billedDate = DateUtilities.createDate(2013, 3, 31);

        // The user is needed for annotating the session
        BspUser user = userList.getByUsername("hrafal");

        // Add the ledger entries that we want to this billing session
        Set<LedgerEntry> ledgerItems =
            ledgerEntryDao.findWithoutBillingSessionByOrderList(Collections.singletonList("PDO-222"));

        // Trying to fix up exactly 9 items on PDO-222, so this is a nice test case.
        if ((ledgerItems == null) || (ledgerItems.size() != 9)) {
            logger.error("PDO-222 should have exactly 9 unbilled ledger entries");
        }

        // save the session with the appropriate id, all ledger entries get the billing session tied to it.
        BillingSession billingSession = new BillingSession(341L, billedDate, user.getDomainUserId(), ledgerItems);
        billingSessionDao.persist(billingSession);
        logger.info("Registered Manual billing");
    }
}
