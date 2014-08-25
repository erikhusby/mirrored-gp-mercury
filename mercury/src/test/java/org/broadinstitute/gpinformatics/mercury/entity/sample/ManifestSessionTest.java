package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionTest {

//    ManifestSessionStub testSession;
    ManifestSession testSession;
    private ResearchProject testRp;
    private String sessionPrefix;
    private BspUser testUser;

    @BeforeMethod
    public void setUp() throws Exception {

        testRp = ResearchProjectTestFactory.createTestResearchProject("RP-332");
        sessionPrefix = "testPrefix";
        testUser = new BSPUserList.QADudeUser("LU", 33L);

        testSession = new ManifestSession(testRp, sessionPrefix, testUser);

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

        Metadata metadata1 = new Metadata(Metadata.Key.SAMPLE_TYPE, "value1");
        Metadata metadata2 = new Metadata(Metadata.Key.TUMOR_NORMAL, "value2");
        Metadata metadata3 = new Metadata(Metadata.Key.COLLECTION_DATE, "value3");

        ManifestRecord testRecord = new ManifestRecord(Arrays.asList(metadata1, metadata2, metadata3));
        testSession.addRecord(testRecord);

        Assert.assertTrue(testSession.getRecords().contains(testRecord));

        ManifestEventLog logEntry = new ManifestEventLog("Failed to Upload Record in session",
                ManifestEventLog.Type.ERROR);
        testSession.addLogEntry(logEntry);

        Assert.assertEquals(testSession.getLogEntries().size() , 1);
        ManifestEventLog logEntry2 = new ManifestEventLog("Failed to Upload Record in session",testRecord,
                ManifestEventLog.Type.ERROR);
        testSession.addLogEntry(logEntry);

        Assert.assertEquals(testRecord.getLogEntries().size(), 1);

        Assert.assertEquals(testSession.getLogEntries().size(), 2);
 
    }

}
