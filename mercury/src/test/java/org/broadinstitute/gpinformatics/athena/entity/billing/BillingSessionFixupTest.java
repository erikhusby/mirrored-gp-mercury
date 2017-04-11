/**
 * Using this package for the fixup so that we can access the package protected constructor for setting the ID.
 */
package org.broadinstitute.gpinformatics.athena.entity.billing;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.boundary.billing.BillingEjb;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.control.dao.billing.LedgerEntryDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.PROD;

@Test(groups = TestGroups.FIXUP)
public class BillingSessionFixupTest extends Arquillian {

    private static final Log logger = LogFactory.getLog(BillingSessionFixupTest.class);

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private LedgerEntryDao ledgerEntryDao;

    @Inject
    private BillingEjb billingEjb;

    @Inject
    private BSPUserList userList;

    @Inject
    private UserBean userBean;

    // Use (RC, "rc"), (PROD, "prod") to push the backfill to RC and production respectively.
    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV, "dev");
    }

    /**
     * Create a completed billing session.
     */
    @Test(enabled = false)
    public void createCompleteBillingSession() {

        // Setting the date will end the session.
        Calendar cal = Calendar.getInstance();
        cal.set(2013, Calendar.MARCH, 31);
        Date billedDate = cal.getTime();

        // The user is needed for annotating the session.
        BspUser user = userList.getByUsername("hrafal");

        // Add the ledger entries that we want to this billing session.
        // These will be ignored as this fixup script has ben run and this is only to allow compile.
        List<String> errorMessages = new ArrayList<>();
        Set<LedgerEntry> ledgerItems =
            ledgerEntryDao.findWithoutBillingSessionByOrderList(Collections.singletonList("PDO-222"), errorMessages);

        Assert.assertNotNull(ledgerItems);
        Assert.assertEquals(ledgerItems.size(), 9, "PDO-222 should have exactly 9 unbilled ledger entries");

        for (LedgerEntry entry : ledgerItems) {
            entry.setQuoteId("DNA32K");
        }

        // Save the session with the appropriate id, all ledger entries get the billing session tied to it.
        BillingSession billingSession = new BillingSession(billedDate, user.getUserId(), ledgerItems);

        billingSessionDao.persist(billingSession);
        logger.info("Registered Manual billing");
    }

    /**
     * This adds the semi-monthly rollup date type to the sessions that were billed that way (everything in the past).
     */
    @Test(enabled = false)
    public void addBillingSessionType() {
        List<BillingSession> sessions  = billingSessionDao.findAll();

        for (BillingSession session : sessions) {
            // If there is a billed date but no type, the type must be old style 15 day billing.
            if ((session.getBilledDate() != null) && (session.getBillingSessionType() == null)) {
                session.setBillingSessionType(BillingSession.BillingSessionType.ROLLUP_SEMI_MONTHLY);
            }
        }

        billingSessionDao.persistAll(Collections.emptyList());

        logger.info("Registered Manual billing");
    }

    @Test(enabled = false)
    public void endBillingSession() {

        userBean.loginOSUser();

        BillingSession sessionToEnd = billingSessionDao.findByBusinessKey("BILL-9314");
        billingEjb.endSession(sessionToEnd);

        billingSessionDao.persist(new FixupCommentary("ending Billing Session for Steve"));
    }

    @Test(enabled = false)
    public void alterQuoteInSessionSupport2695() {
        userBean.loginOSUser();

        BillingSession session = billingSessionDao.findByBusinessKey("BILL-9361");

        for (LedgerEntry ledgerEntry : session.getLedgerEntryItems()) {
            ledgerEntry.setQuoteId("MMMJLI");
        }

        billingSessionDao.persist(new FixupCommentary("SUPPORT-2695: Changed the Quote for PDO-11232 on ledger entries to allow billing to proceed in Mercury"));
    }

    @Test(enabled = false)
    public void alterQuoteInSessionGplim4730() {
        userBean.loginOSUser();

        BillingSession session = billingSessionDao.findByBusinessKey("BILL-9373");

        for (LedgerEntry ledgerEntry : session.getLedgerEntryItems()) {
            ledgerEntry.setQuoteId("GPIFX");
        }

        billingSessionDao.persist(new FixupCommentary("GPLIM-4730: Changed the Quote for PDO-11370 on ledger entries to allow billing to proceed in Mercury"));
    }
}
