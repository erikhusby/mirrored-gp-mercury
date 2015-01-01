package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.STUBBY, enabled = true)
public class BillingAccessEjbTest extends ContainerTest {

    @Inject
    private BillingSessionDao billingSessionDao;

    @Inject
    private BillingEjb billingEjb;

    @Inject
    private BillingAdaptor billingAdaptor;

    @Inject
    private BillingSessionAccessEjb billingSessionAccessEjb;
    private String[] sampleNameList;
    private String billingSessionBusinessKey;

    @BeforeMethod
    public void setUp() throws Exception {

        if (billingSessionDao == null) {
            return;
        }

        sampleNameList = new String[]{"SM-2342", "SM-9291", "SM-2349", "SM-9944", "SM-4444", "SM-4441", "SM-1112",
                "SM-4488"};

        BillingSession billingSession = BillingEjbJiraDelayedTest.writeFixtureDataOneSamplePerProductOrder(
                billingSessionDao, sampleNameList);

        billingSessionBusinessKey = billingSession.getBusinessKey();
    }

    public void testApplicationLock() throws Exception {
        Assert.assertFalse( billingSessionAccessEjb.isSessionLocked(billingSessionBusinessKey));

        BillingSession foundSession = billingSessionAccessEjb.findAndLockSession(billingSessionBusinessKey);
        Assert.assertNotNull(foundSession);
        Assert.assertEquals(foundSession.getBusinessKey(), billingSessionBusinessKey);
        Assert.assertTrue(billingSessionAccessEjb.isSessionLocked(billingSessionBusinessKey));

        try {
            foundSession = billingSessionAccessEjb.findAndLockSession(billingSessionBusinessKey);
            Assert.fail();
        } catch (BillingException be) {
            Assert.assertEquals(be.getMessage(), BillingEjb.LOCKED_SESSION_TEXT);
        }

        billingSessionAccessEjb.saveAndUnlockSession(foundSession);

        Assert.assertFalse(billingSessionAccessEjb.isSessionLocked(billingSessionBusinessKey));
      }
}
