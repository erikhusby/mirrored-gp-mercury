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
 * Database free tests of ManifestSessions.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionTest {

    private ManifestSession testSession;
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

    public void basicProperties() throws Exception {
        Assert.assertEquals(testSession.getResearchProject(), testRp);
        Assert.assertEquals(testSession.getSessionName(), sessionPrefix + testSession.getManifestSessionId());
        Assert.assertEquals(testSession.getStatus(), ManifestSession.SessionStatus.OPEN);

        Assert.assertEquals(testSession.getCreatedBy(), testUser.getUserId());
        Assert.assertEquals(testSession.getModifiedBy(), testUser.getUserId());

        BSPUserList.QADudeUser modifyUser = new BSPUserList.QADudeUser("LM", 43L);
        testSession.setModifiedBy(modifyUser);

        Assert.assertEquals(testSession.getModifiedBy(), modifyUser.getUserId());
    }

    public void addRecord() throws Exception {
        ManifestRecord testRecord = buildManifestRecord(testSession);

        Assert.assertTrue(testSession.getRecords().contains(testRecord));
        Assert.assertEquals(testRecord.getSession(), testSession);
    }

    private ManifestRecord buildManifestRecord(ManifestSession sessionIn) {
        return ManifestTestFactory.buildManifestRecord(null, ImmutableMap.of(
                Metadata.Key.SAMPLE_TYPE, "value1",
                Metadata.Key.TUMOR_NORMAL, "value2",
                Metadata.Key.BUICK_COLLECTION_DATE, "value3"), sessionIn);
    }

    public void addLogEntries() throws Exception {
        ManifestRecord testRecord = buildManifestRecord(testSession);
        ManifestEvent logEntryWithoutRecord = new ManifestEvent("Failed to Upload Record in session",
                ManifestEvent.Type.ERROR);
        testSession.addLogEntry(logEntryWithoutRecord);

        Assert.assertEquals(testSession.getLogEntries().size(), 1);
        ManifestEvent logEntryWithRecord = new ManifestEvent("Failed to Upload Record in session", testRecord,
                ManifestEvent.Type.ERROR);
        testSession.addLogEntry(logEntryWithRecord);

        Assert.assertEquals(testRecord.getLogEntries().size(), 1);
        Assert.assertEquals(testSession.getLogEntries().size(), 2);
    }
}
