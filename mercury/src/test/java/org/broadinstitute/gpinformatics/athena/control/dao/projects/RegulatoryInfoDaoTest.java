package org.broadinstitute.gpinformatics.athena.control.dao.projects;

import org.broadinstitute.gpinformatics.athena.entity.project.RegulatoryInfo;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@Test(groups = TestGroups.STUBBY)
@Dependent
public class RegulatoryInfoDaoTest extends StubbyContainerTest {

    public RegulatoryInfoDaoTest(){}

    @Inject
    private RegulatoryInfoDao regulatoryInfoDao;

    private RegulatoryInfo regulatoryInfo1;
    private RegulatoryInfo regulatoryInfo2;
    private RegulatoryInfo regulatoryInfo3;

    private String dupeIdentifier;
    private String nonDupeIdentifier;

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {

        if(regulatoryInfoDao == null) {
            return;
        }

        Date today = new Date();
        dupeIdentifier = "testConsent" + today.getTime();

        regulatoryInfo1 = new RegulatoryInfo("Test first consent", RegulatoryInfo.Type.ORSP_NOT_HUMAN_SUBJECTS_RESEARCH,
                dupeIdentifier);
        regulatoryInfo2 = new RegulatoryInfo("Test first consent DupeName", RegulatoryInfo.Type.ORSP_NOT_ENGAGED,
                dupeIdentifier);
        nonDupeIdentifier = "testConsent no dupe" + today.getTime();
        regulatoryInfo3 = new RegulatoryInfo("Test third consent", RegulatoryInfo.Type.ORSP_NOT_ENGAGED,
                nonDupeIdentifier);

        regulatoryInfoDao.persist(regulatoryInfo1);
        regulatoryInfoDao.persist(regulatoryInfo2);
        regulatoryInfoDao.persist(regulatoryInfo3);
        regulatoryInfoDao.flush();
        regulatoryInfoDao.clear();
    }

    @AfterMethod
    public void tearDown() throws Exception {

        if(regulatoryInfoDao == null) {
            return;
        }

        regulatoryInfo1 = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfo1.getRegulatoryInfoId());
        regulatoryInfo2 = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfo2.getRegulatoryInfoId());
        regulatoryInfo3 = regulatoryInfoDao.findById(RegulatoryInfo.class, regulatoryInfo3.getRegulatoryInfoId());

        regulatoryInfoDao.remove(regulatoryInfo1);
        regulatoryInfoDao.remove(regulatoryInfo2);
        regulatoryInfoDao.remove(regulatoryInfo3);
        regulatoryInfoDao.flush();

        dupeIdentifier = "";
        nonDupeIdentifier = "";
    }

    public void testFindByIdentifier() throws Exception {
        List<RegulatoryInfo> dupeIdentifiers = regulatoryInfoDao.findByIdentifier(dupeIdentifier);

        Assert.assertEquals(dupeIdentifiers.size(), 2);
        Assert.assertTrue(dupeIdentifiers.contains(regulatoryInfo1));
        Assert.assertTrue(dupeIdentifiers.contains(regulatoryInfo2));

        List<RegulatoryInfo> nonDupeIdentifiers = regulatoryInfoDao.findByIdentifier(nonDupeIdentifier);

        Assert.assertEquals(nonDupeIdentifiers.size(), 1);
        Assert.assertTrue(nonDupeIdentifiers.contains(regulatoryInfo3));
    }
}
