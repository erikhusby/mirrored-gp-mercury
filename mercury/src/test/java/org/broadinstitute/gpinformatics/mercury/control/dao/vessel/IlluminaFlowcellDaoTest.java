package org.broadinstitute.gpinformatics.mercury.control.dao.vessel;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.testng.annotations.Test;

import javax.inject.Inject;

/**
 * Test persist and fetch
 */
@Test(groups = {TestGroups.STUBBY})
public class IlluminaFlowcellDaoTest extends StubbyContainerTest {

    @Inject
    private IlluminaFlowcellDao illuminaFlowcellDao;

    @Test
    public void testFindByBarcode() throws Exception {
        IlluminaFlowcell junk = illuminaFlowcellDao.findByBarcode("junk");
        Assert.assertNull(junk, "Unexpected flowcell");
    }
}
