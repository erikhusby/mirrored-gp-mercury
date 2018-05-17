package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.athena.control.dao.billing.BillingSessionDao;
import org.broadinstitute.gpinformatics.athena.entity.billing.BillingSession;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Date;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.STUBBY, enabled = true)
@Dependent
public class BillingAccessEjbTest extends StubbyContainerTest {

    public BillingAccessEjbTest(){}

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

        final long time = (new Date()).getTime();
        sampleNameList = new String[]{"SM-" + time, "SM-"+ time+1, "SM-"+ time+2, "SM-"+ time+3, "SM-"+ time+4, "SM-"+ time+5, "SM-"+ time+6,
                "SM-"+ time+7};

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
