package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionTest {

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

    public void testBasicProperties() throws Exception {

        Assert.assertEquals(testSession.getResearchProject(), testRp);
        Assert.assertEquals(testSession.createSessionName(), sessionPrefix+testSession.getManifestSessionId());
        Assert.assertEquals(testSession.getStatus(), ManifestSession.SessionStatus.OPEN);

        Assert.assertEquals(testSession.getCreatedBy() , testUser.getUserId());
        Assert.assertEquals(testSession.getModifiedBy(), testUser.getUserId());

        BSPUserList.QADudeUser modifyUser = new BSPUserList.QADudeUser("LM", 43L);
        testSession.setModifiedBy(modifyUser);

        Assert.assertEquals(testSession.getModifiedBy(), modifyUser.getUserId());

        testAddRecord();

    }

    public void testAddRecord() throws Exception{
        ManifestRecord testRecord = ManifestTestFactory.buildManifestRecord(null, ImmutableMap.of(
                Metadata.Key.SAMPLE_TYPE, "value1",
                Metadata.Key.TUMOR_NORMAL, "value2",
                Metadata.Key.COLLECTION_DATE, "value3"));

        testSession.addRecord(testRecord);

        Assert.assertTrue(testSession.getRecords().contains(testRecord));
        Assert.assertEquals(testRecord.getSession(), testSession);
    }

    public void testAddLogEntries() throws Exception{
        ManifestRecord testRecord = ManifestTestFactory.buildManifestRecord(null, ImmutableMap.of(
                Metadata.Key.SAMPLE_TYPE, "value1",
                Metadata.Key.TUMOR_NORMAL, "value2",
                Metadata.Key.COLLECTION_DATE, "value3"));
        ManifestEvent logEntry = new ManifestEvent("Failed to Upload Record in session",
                ManifestEvent.Type.ERROR);
        testSession.addLogEntry(logEntry);

        Assert.assertEquals(testSession.getLogEntries().size(), 1);
        ManifestEvent logEntry2 = new ManifestEvent("Failed to Upload Record in session",testRecord,
                ManifestEvent.Type.ERROR);
        testSession.addLogEntry(logEntry);

        Assert.assertEquals(testRecord.getLogEntries().size(), 1);

        Assert.assertEquals(testSession.getLogEntries().size(), 2);
    }
}
