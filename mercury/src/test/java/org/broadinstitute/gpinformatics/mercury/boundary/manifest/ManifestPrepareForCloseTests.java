package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ResearchProjectTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestStatusErrorMatcher.hasError;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestPrepareForCloseTests {

    private static final String DATA_PROVIDER_RECORD_STATUS = "recordStatusProvider";
    private static final String TEST_RESEARCH_PROJECT_KEY = "RP-1000";
    private static final ManifestRecord.ErrorStatus NO_ERROR = null;
    private ManifestSession session;
    private BspUser user = new BSPUserList.QADudeUser("LU", 342L);

    @BeforeMethod
    public void setUp() {
        ResearchProject researchProject =
                ResearchProjectTestFactory.createTestResearchProject(TEST_RESEARCH_PROJECT_KEY);

        session = ManifestTestFactory.buildManifestSession(researchProject.getBusinessKey(), "DoctorZhivago", user);

        ManifestTestFactory.addRecord(session, NO_ERROR, ManifestRecord.Status.SCANNED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, "Fred"));
    }

    @Test
    public void testPrepareForCloseOnCleanSession() {
        ManifestStatus manifestStatus = session.generateSessionStatusForClose();
        assertThat(manifestStatus.getSamplesInManifest(), is(1));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(1));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(1));

        Assert.assertTrue(manifestStatus.getErrorMessages().isEmpty(),
                "Clean session should have no errors.");
    }

    @DataProvider(name = DATA_PROVIDER_RECORD_STATUS)
    private Object[][] statusForValidationErrorProvider() {
        return new Object[][]{
                {ManifestRecord.Status.UPLOADED},
                {ManifestRecord.Status.UPLOAD_ACCEPTED}};
    }

    @Test(dataProvider = DATA_PROVIDER_RECORD_STATUS)
    public void testValidationError(ManifestRecord.Status status) {
        setManifestRecordStatus(status);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(1));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(1));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(0));

        assertThat(manifestStatus.getErrorMessages(), hasSize(1));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
    }

    @Test
    public void testValidationForDuplicateSample() {
        addDuplicateManifestRecord();

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(2));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(1));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(1));

        assertThat(manifestStatus.getErrorMessages(), hasSize(1));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID));
    }

    public void testValidationForGenderMismatchSample() {
        addRecord(session, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(2));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(2));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(2));

        assertThat(manifestStatus.getErrorMessages(), hasSize(1));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISMATCHED_GENDER));
    }

    private static void addRecord(ManifestSession session, ManifestRecord.ErrorStatus errorStatus,
                                  ManifestRecord.Status status) {
        ManifestTestFactory.addRecord(session, errorStatus, status, ImmutableMap.<Metadata.Key, String>of());
    }

    public void testValidationForUnscannedAndDuplicates() {
        addDuplicateManifestRecord();
        addRecord(session, null, ManifestRecord.Status.UPLOAD_ACCEPTED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(3));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(2));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(1));

        assertThat(manifestStatus.getErrorMessages(), hasSize(2));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID));
    }

    public void testValidationForUnscannedAndDuplicatesAndMismatchedGender() {
        addDuplicateManifestRecord();
        addRecord(session, null, ManifestRecord.Status.UPLOAD_ACCEPTED);
        addRecord(session, ManifestRecord.ErrorStatus.MISMATCHED_GENDER, ManifestRecord.Status.SCANNED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(4));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(3));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(2));

        assertThat(manifestStatus.getErrorMessages(), hasSize(3));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISMATCHED_GENDER));

    }

    private void addDuplicateManifestRecord() {
        ManifestRecord record = session.getRecords().iterator().next();
        String value = record.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue();

        ManifestTestFactory
                .addRecord(session, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                        ImmutableMap.of(Metadata.Key.SAMPLE_ID, value));
    }

    private void setManifestRecordStatus(ManifestRecord.Status status) {
        session.getRecords().iterator().next().setStatus(status);
    }
}
