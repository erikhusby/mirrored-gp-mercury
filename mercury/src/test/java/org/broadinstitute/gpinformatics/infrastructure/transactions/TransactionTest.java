package org.broadinstitute.gpinformatics.infrastructure.transactions;

import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.testng.annotations.Test;

/**
 * Tests for our expectations of how the extended persistence context should interact with container managed
 * transactions.
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class TransactionTest extends ContainerTest {

    public void setUp() {
        StaticPlate plate =
                new StaticPlate("TransactionTest" + System.currentTimeMillis(), StaticPlate.PlateType.Eppendorf96);
    }
}
