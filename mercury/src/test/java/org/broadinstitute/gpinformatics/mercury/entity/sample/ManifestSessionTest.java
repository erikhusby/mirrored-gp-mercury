package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionTest {

    ManifestSessionStub testSession;
    private ResearchProject testRp;
    private String sessionPrefix;
    private BspUser testUser;

    @BeforeMethod
    public void setUp() throws Exception {

        testRp = ResearchProjectTestFactory.createTestResearchProject("RP-332");
        sessionPrefix = "testPrefix";
        testUser = new BSPUserList.QADudeUser("LU", 33L);

        testSession = new ManifestSessionStub(testRp, sessionPrefix, testUser);

    }

    public void testAddRecord() throws Exception {

        Assert.assertEquals(testSession.getResearchProject(), testRp);
        Assert.assertEquals(testSession.createSessionName(), sessionPrefix+testSession.getSessionId());
        Assert.assertEquals(testSession.getStatus(), ManifestSession.SessionStatus.OPEN);

        Assert.assertEquals(testSession.getCreatedBy() , testUser.getUserId());
        Assert.assertEquals(testSession.getModifiedBy(), testUser.getUserId());

        BSPUserList.QADudeUser modifyUser = new BSPUserList.QADudeUser("LM", 43L);
        testSession.setModifiedBy(modifyUser);

        Assert.assertEquals(testSession.getModifiedBy(), modifyUser.getUserId());

        ManifestRecord testRecord = new ManifestRecord();

        testSession.addRecord(testRecord);

        Assert.assertTrue(testSession.getRecords().contains(testRecord));


    }

    private class ManifestSessionStub extends ManifestSession {
        public ManifestSessionStub(ResearchProject researchProject, String sessionPrefix,
                                   BspUser createdBy) {
            super(researchProject, sessionPrefix, createdBy);
        }

        @Override
        protected Long getSessionId() {
            return super.getSessionId();
        }
    }
}
