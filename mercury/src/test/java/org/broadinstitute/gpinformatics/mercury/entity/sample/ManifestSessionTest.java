package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.UpdateData;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestStatusErrorMatcher.hasError;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;


/**
 * Database free tests of ManifestSessions.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestSessionTest {

    private static final String DATA_PROVIDER_RECORD_STATUS = "recordStatusProvider";
    private static final String SAMPLE_ID_1 = "SM-1";
    private static final String SAMPLE_ID_2 = "SM-2";
    private static final String SAMPLE_ID_3 = "SM-3";
    private static final List<String> SAMPLES_IN_MANIFEST = Arrays.asList(SAMPLE_ID_1, SAMPLE_ID_2, SAMPLE_ID_3);
    private static final int NUM_SAMPLES_IN_MANIFEST = SAMPLES_IN_MANIFEST.size();
    private ManifestSession session;
    private ResearchProject testRp;
    private String sessionPrefix;
    private BspUser testUser;

    @BeforeMethod
    public void setUp() throws Exception {
        testRp = ResearchProjectTestFactory.createTestResearchProject("RP-332");
        sessionPrefix = "testPrefix";
        testUser = new BSPUserList.QADudeUser("LU", 33L);

        session = new ManifestSession(testRp, sessionPrefix, testUser, false,
                ManifestSessionEjb.AccessioningProcessType.CRSP);

        for (String sampleId : SAMPLES_IN_MANIFEST) {
            ManifestRecord manifestRecord = buildManifestRecord(session, sampleId);
            manifestRecord.setStatus(ManifestRecord.Status.SCANNED);
        }
    }

    public void basicProperties() throws Exception {
        Assert.assertEquals(session.getResearchProject(), testRp);
        assertThat(session.getSessionName(), containsString(sessionPrefix));
        assertThat(session.getSessionName(), containsString(String.valueOf(session.getManifestSessionId())));
        Assert.assertEquals(session.getStatus(), ManifestSession.SessionStatus.OPEN);

        UpdateData updateData = session.getUpdateData();
        Assert.assertEquals(updateData.getCreatedBy(), testUser.getUserId());

        String LAB_MANAGER = "LM";
        long ARBITRARY_QA_DUDE_LM_USER_ID = 43L;

        BSPUserList.QADudeUser modifyUser = new BSPUserList.QADudeUser(LAB_MANAGER, ARBITRARY_QA_DUDE_LM_USER_ID);
        updateData.setModifiedBy(modifyUser);

        Assert.assertEquals(updateData.getModifiedBy(), modifyUser.getUserId());
    }

    public void addRecord() throws Exception {
        ManifestRecord testRecord = buildManifestRecord(session, SAMPLE_ID_1);
        Assert.assertTrue(session.getRecords().contains(testRecord));
        Assert.assertEquals(testRecord.getManifestSession(), session);
    }

    private ManifestRecord buildManifestRecord(ManifestSession manifestSession, String sampleId) {
        ManifestRecord manifestRecord = new ManifestRecord(ManifestTestFactory.buildMetadata(ImmutableMap.of(
                Metadata.Key.SAMPLE_ID, sampleId,
                Metadata.Key.MATERIAL_TYPE, "value1",
                Metadata.Key.TUMOR_NORMAL, "value2",
                Metadata.Key.BUICK_COLLECTION_DATE, "value3")));
        manifestSession.addRecord(manifestRecord);
        return manifestRecord;
    }

    public void addLogEntries() throws Exception {
        ManifestRecord testRecord = buildManifestRecord(session, SAMPLE_ID_1);

        Assert.assertEquals(session.getManifestEvents().size(), 0);
        ManifestEvent manifestEventWithRecord = new ManifestEvent(ManifestEvent.Severity.ERROR,
                "Failed to Upload Record in session", testRecord);
        session.addManifestEvent(manifestEventWithRecord);

        Assert.assertEquals(testRecord.getManifestEvents().size(), 1);
        Assert.assertEquals(session.getManifestEvents().size(), 1);
    }

    /**
     * A happy path test, all samples in the manifest have scanned and nothing is wrong.
     */
    public void allSamplesScanned() {
        assertThat(session.hasErrors(), is(false));
        ManifestStatus manifestStatus = session.generateSessionStatusForClose();
        assertThat(manifestStatus.getErrorMessages(), is(empty()));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(3));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(manifestStatus.getSamplesInManifest(), is(3));
    }


    public void missingSample() {
        setSampleStatus(SAMPLE_ID_1, ManifestRecord.Status.UPLOAD_ACCEPTED);
        assertThat(session.hasErrors(), is(false));
        ManifestStatus manifestStatus = session.generateSessionStatusForClose();
        assertThat(manifestStatus.getErrorMessages(), is(not(empty())));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));

        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(2));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(1));
        assertThat(manifestStatus.getSamplesInManifest(), is(3));
    }

    private void setSampleStatus(String sampleId, ManifestRecord.Status status) {
        for (ManifestRecord manifestRecord : session.getRecords()) {
            if (manifestRecord.getValueByKey(Metadata.Key.SAMPLE_ID).equals(sampleId)) {
                manifestRecord.setStatus(status);
            }
        }
    }

    public void testPrepareForCloseOnCleanSession() {
        ManifestStatus manifestStatus = session.generateSessionStatusForClose();
        assertThat(manifestStatus.getSamplesInManifest(), is(NUM_SAMPLES_IN_MANIFEST));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(NUM_SAMPLES_IN_MANIFEST));

        Assert.assertTrue(manifestStatus.getErrorMessages().isEmpty(),
                "Clean session should have no errors.");
    }

    @DataProvider(name = DATA_PROVIDER_RECORD_STATUS)
    private Object[][] statusForValidationErrorProvider() {
        return new Object[][]{
                {ManifestRecord.Status.UPLOADED, 0},
                {ManifestRecord.Status.UPLOAD_ACCEPTED, 1}};
    }

    @Test(dataProvider = DATA_PROVIDER_RECORD_STATUS)
    public void testValidationError(ManifestRecord.Status status, int numElligibleForaccessioning) {
        setManifestRecordStatus(status);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(NUM_SAMPLES_IN_MANIFEST));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(numElligibleForaccessioning));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(NUM_SAMPLES_IN_MANIFEST - 1));

        assertThat(manifestStatus.getErrorMessages(), hasSize(1));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
    }

    public void testValidationForDuplicateSample() {
        addDuplicateManifestRecord();

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(NUM_SAMPLES_IN_MANIFEST + 1));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(NUM_SAMPLES_IN_MANIFEST));

        assertThat(manifestStatus.getErrorMessages(), hasSize(0));
    }

    public void testValidationForGenderMismatchSample() {
        addRecord(session, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(NUM_SAMPLES_IN_MANIFEST + 1));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(0));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(NUM_SAMPLES_IN_MANIFEST + 1));

        assertThat(manifestStatus.getErrorMessages(), hasSize(0));
    }

    private static void addRecord(ManifestSession session, ManifestRecord.ErrorStatus errorStatus,
                                  ManifestRecord.Status status) {
        ManifestTestFactory.addRecord(session, errorStatus, status, ImmutableMap.<Metadata.Key, String>of(),
                EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));
    }

    public void testValidationForUnscannedAndDuplicates() {
        addDuplicateManifestRecord();
        addRecord(session, null, ManifestRecord.Status.UPLOAD_ACCEPTED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(NUM_SAMPLES_IN_MANIFEST + 2));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(1));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(NUM_SAMPLES_IN_MANIFEST));

        assertThat(manifestStatus.getErrorMessages(), hasSize(1));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
    }

    public void testValidationForUnscannedAndDuplicatesAndMismatchedGender() {
        addDuplicateManifestRecord();
        addRecord(session, null, ManifestRecord.Status.UPLOAD_ACCEPTED);
        addRecord(session, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(NUM_SAMPLES_IN_MANIFEST + 3));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(1));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(NUM_SAMPLES_IN_MANIFEST + 1));

        assertThat(manifestStatus.getErrorMessages(), hasSize(1));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
    }

    public void testValidationForUploadAccepted() {
        setAllManifestRecordStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getErrorMessages(), hasSize(3));
        assertThat(manifestStatus.getSamplesInManifest(), is(NUM_SAMPLES_IN_MANIFEST));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(0));
        assertThat(manifestStatus.getSamplesEligibleForAccessioningInManifest(), is(NUM_SAMPLES_IN_MANIFEST));
    }

    public void testTransferUnUsedSample() throws Exception {

        session.setStatus(ManifestSession.SessionStatus.COMPLETED);
        setAllManifestRecordStatus(ManifestRecord.Status.ACCESSIONED);

        ManifestRecord testRecord = session.getRecords().iterator().next();
        ManifestRecord recordForTransfer = session.findRecordForTransferByKey(Metadata.Key.SAMPLE_ID,
                testRecord.getSampleId());

        assertThat(testRecord, is(equalTo(recordForTransfer)));

    }

    public void testTransferUsedSample() throws Exception {

        session.setStatus(ManifestSession.SessionStatus.COMPLETED);
        setAllManifestRecordStatus(ManifestRecord.Status.SAMPLE_TRANSFERRED_TO_TUBE);

        ManifestRecord testRecord = session.getRecords().iterator().next();
        try {
            session.findRecordForTransferByKey(Metadata.Key.SAMPLE_ID, testRecord.getSampleId());
            Assert.fail();
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString(ManifestRecord.ErrorStatus.SOURCE_ALREADY_TRANSFERRED.getBaseMessage()));
        }
    }

    private void addDuplicateManifestRecord() {
        ManifestRecord record = session.getRecords().iterator().next();
        String value = record.getValueByKey(Metadata.Key.SAMPLE_ID);

        ManifestTestFactory
                .addRecord(session, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                        ImmutableMap.of(Metadata.Key.SAMPLE_ID, value), EnumSet.of(Metadata.Key.BROAD_2D_BARCODE));
    }

    private void setManifestRecordStatus(ManifestRecord.Status status) {
        session.getRecords().iterator().next().setStatus(status);
    }

    private void setAllManifestRecordStatus(ManifestRecord.Status status) {
        for (ManifestRecord manifestRecord : session.getRecords()) {
            manifestRecord.setStatus(status);
        }
    }
}
