package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.hamcrest.CoreMatchers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;


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
            manifestRecord.setStatus(ManifestRecord.Status.SCANNED);
        }
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
        ManifestRecord testRecord = buildManifestRecord(testSession, SAMPLE_ID_1);
        Assert.assertTrue(testSession.getRecords().contains(testRecord));
        Assert.assertEquals(testRecord.getManifestSession(), testSession);
    }

    private ManifestRecord buildManifestRecord(ManifestSession manifestSession, String sampleId) {
        ManifestRecord manifestRecord = new ManifestRecord(ManifestTestFactory.buildMetadata(ImmutableMap.of(
                Metadata.Key.SAMPLE_ID, sampleId,
                Metadata.Key.SAMPLE_TYPE, "value1",
                Metadata.Key.TUMOR_NORMAL, "value2",
                Metadata.Key.COLLECTION_DATE, "value3")));
        manifestSession.addRecord(manifestRecord);
        return manifestRecord;
    }

    public void addLogEntries() throws Exception {
        ManifestRecord testRecord = buildManifestRecord(testSession, SAMPLE_ID_1);
        ManifestEvent manifestEventWithoutRecord = new ManifestEvent(ManifestEvent.Severity.ERROR,
                "Failed to Upload Record in session"
        );
        testSession.addManifestEvent(manifestEventWithoutRecord);

        Assert.assertEquals(testSession.getManifestEvents().size(), 1);
        ManifestEvent manifestEventWithRecord = new ManifestEvent(ManifestEvent.Severity.ERROR,
                "Failed to Upload Record in session",
                testRecord);
        testSession.addManifestEvent(manifestEventWithRecord);

        Assert.assertEquals(testRecord.getManifestEvents().size(), 1);
        Assert.assertEquals(testSession.getManifestEvents().size(), 2);
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
        setSampleStatus(SAMPLE_ID_1, ManifestRecord.Status.UPLOADED);
        assertThat(testSession.hasErrors(), is(false));
        testSession.validateForClose();
        assertThat(testSession.hasErrors(), is(true));
    }

    private void setSampleStatus(String sampleId, ManifestRecord.Status status) {
        for (ManifestRecord manifestRecord : testSession.getRecords()) {
            if (manifestRecord.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue().equals(sampleId)) {
                manifestRecord.setStatus(status);
            }
        }
    }

    public void successfulScanCollaboratorTubeInTubeTransfer() throws TubeTransferException {
        String collaboratorBarcode = SAMPLE_ID_1;
        ManifestRecord manifestRecord = testSession.findScannedRecord(collaboratorBarcode);
        assertThat(manifestRecord, is(notNullValue()));
    }

    public void notReadyScanCollaboratorTubeInTubeTransfer() {
        String collaboratorBarcode = SAMPLE_ID_1;
        setSampleStatus(collaboratorBarcode, ManifestRecord.Status.UPLOADED);
        try {
            testSession.findScannedRecord(collaboratorBarcode);
            Assert.fail();
        } catch (TubeTransferException e) {
            assertThat(e.getErrorStatus(), is(
                    CoreMatchers.equalTo(ManifestRecord.ErrorStatus.NOT_READY_FOR_ACCESSIONING)));
        }
    }

    public void notInManifestCollaboratorTubeInTubeTransfer() {
        String collaboratorBarcode = SAMPLE_ID_1 + "_UNRECOGNIZED";
        setSampleStatus(collaboratorBarcode, ManifestRecord.Status.UPLOADED);
        try {
            testSession.findScannedRecord(collaboratorBarcode);
            Assert.fail();
        } catch (TubeTransferException e) {
            assertThat(e.getErrorStatus(), is(CoreMatchers.equalTo(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST)));
        }
    }

    public void scanSampleInManifest() throws Exception {

        setSampleStatus(SAMPLE_ID_1, ManifestRecord.Status.UPLOAD_ACCEPTED);

        ManifestRecord record = testSession.scanSample(SAMPLE_ID_1);
        assertThat(record.getStatus(), is(CoreMatchers.equalTo(ManifestRecord.Status.SCANNED)));

        assertThat(testSession.getManifestEvents(), is(empty()));
        try {
            ManifestRecord record2 = testSession.scanSample(SAMPLE_ID_1 + "_NOTIN");
            Assert.fail();
        } catch (TubeTransferException tte) {
            assertThat(tte.getErrorStatus(), is(CoreMatchers.equalTo(ManifestRecord.ErrorStatus.NOT_IN_MANIFEST)));
            assertThat(testSession.getManifestEvents(), is(not(empty())));
            assertThat(testSession.getManifestEvents().size(), is(equalTo(1)));
        }

        String errorRecordID = "20923842";
        ManifestRecord errorRecord = buildManifestRecord(testSession, errorRecordID);
        setSampleStatus(errorRecordID, ManifestRecord.Status.UPLOADED);
        testSession.addManifestEvent(new ManifestEvent(
                ManifestEvent.Severity.QUARANTINED,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.formatMessage("Sample ID", errorRecordID),
                errorRecord));

        try {
            testSession.scanSample(errorRecordID);
            Assert.fail();
        } catch (TubeTransferException e) {
            assertThat(e.getErrorStatus(), is(equalTo(ManifestRecord.ErrorStatus.PREVIOUS_ERRORS_UNABLE_TO_CONTINUE)));
        }

    }
}
