package org.broadinstitute.gpinformatics.athena.control.dao.billing;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Tests for the billing session
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION,enabled=true)
public class BillingSessionDaoTest {

    @Inject
    private BillingSessionDao billingSessionDao
            ;
}
