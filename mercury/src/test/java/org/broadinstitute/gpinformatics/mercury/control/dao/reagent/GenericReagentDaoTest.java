package org.broadinstitute.gpinformatics.mercury.control.dao.reagent;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Test retrieval of entities
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class GenericReagentDaoTest extends StubbyContainerTest {

    public GenericReagentDaoTest(){}

    @Inject
    private GenericReagentDao genericReagentDao;

    @Test
    public void testFindByReagentNameLotExpiration() {
        SimpleDateFormat testPrefixDateFormat = new SimpleDateFormat("MMddHHmmss");

        String reagentName = "TestReagent" + testPrefixDateFormat.format(new Date());
        String lot = "1234";
        genericReagentDao.persist(new GenericReagent(reagentName, lot, null));
        Date expiration = new Date();
        genericReagentDao.persist(new GenericReagent(reagentName, lot, expiration));

        GenericReagent nullDate = genericReagentDao.findByReagentNameLotExpiration(reagentName, lot, null);
        GenericReagent withDate = genericReagentDao.findByReagentNameLotExpiration(reagentName, lot, expiration);
        Assert.assertNotNull(nullDate);
        Assert.assertNotNull(withDate);
        Assert.assertNotEquals(nullDate, withDate);
    }
}