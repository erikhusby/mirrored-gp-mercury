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

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Database free tests of ManifestSessions.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionTest {

    private static final String SAMPLE_ID_1 = "SM-1";
    private static final String SAMPLE_ID_2 = "SM-2";
    private static final String SAMPLE_ID_3 = "SM-3";
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

        for (String sampleId : Arrays.asList(SAMPLE_ID_1, SAMPLE_ID_2, SAMPLE_ID_3)) {
            ManifestRecord manifestRecord = buildManifestRecord(testSession, sampleId);
            testSession.addRecord(manifestRecord);
            manifestRecord.setStatus(ManifestRecord.Status.SCANNED);
        }
    }

    public void basicProperties() throws Exception {
        Assert.assertEquals(testSession.getResearchProject(), testRp);
        Assert.assertEquals(testSession.createSessionName(), sessionPrefix + testSession.getManifestSessionId());
        Assert.assertEquals(testSession.getStatus(), ManifestSession.SessionStatus.OPEN);

        Assert.assertEquals(testSession.getCreatedBy(), testUser.getUserId());
        Assert.assertEquals(testSession.getModifiedBy(), testUser.getUserId());

        BSPUserList.QADudeUser modifyUser = new BSPUserList.QADudeUser("LM", 43L);
        testSession.setModifiedBy(modifyUser);

        Assert.assertEquals(testSession.getModifiedBy(), modifyUser.getUserId());
    }

    public void addRecord() throws Exception {
        ManifestRecord testRecord = buildManifestRecord(testSession, SAMPLE_ID_1);

        testSession.addRecord(testRecord);

        Assert.assertTrue(testSession.getRecords().contains(testRecord));
        Assert.assertEquals(testRecord.getSession(), testSession);
    }

    private ManifestRecord buildManifestRecord(ManifestSession manifestSession, String sampleId) {
        return ManifestTestFactory.buildManifestRecord(manifestSession, ImmutableMap.of(
                Metadata.Key.SAMPLE_ID, sampleId,
                Metadata.Key.SAMPLE_TYPE, "value1",
                Metadata.Key.TUMOR_NORMAL, "value2",
                Metadata.Key.COLLECTION_DATE, "value3"));
    }

    public void addLogEntries() throws Exception {
        ManifestRecord testRecord = buildManifestRecord(testSession, SAMPLE_ID_1);
        ManifestEvent logEntryWithoutRecord = new ManifestEvent("Failed to Upload Record in session",
                ManifestEvent.Type.ERROR);
        testSession.addLogEntry(logEntryWithoutRecord);

        Assert.assertEquals(testSession.getLogEntries().size(), 1);
        ManifestEvent logEntryWithRecord = new ManifestEvent("Failed to Upload Record in session",
                ManifestEvent.Type.ERROR, testRecord
        );
        testSession.addLogEntry(logEntryWithRecord);

        Assert.assertEquals(testRecord.getLogEntries().size(), 1);
        Assert.assertEquals(testSession.getLogEntries().size(), 2);
    }

    /**
     * A happy path test, all samples in the manifest have scanned and nothing is wrong.
     */
    public void allSamplesScanned() {
        assertThat(testSession.hasErrors(), is(false));
        testSession.validateForClose();
        assertThat(testSession.hasErrors(), is(false));
    }


    public void missingSample() {
        for (ManifestRecord manifestRecord : testSession.getRecords()) {
            if (manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(SAMPLE_ID_1)) {
                manifestRecord.setStatus(ManifestRecord.Status.UPLOADED);
            }
        }
        assertThat(testSession.hasErrors(), is(false));
        testSession.validateForClose();
        assertThat(testSession.hasErrors(), is(true));
    }

    public void scanCollaboratorTubeInTubeTransfer() {

    }
}
