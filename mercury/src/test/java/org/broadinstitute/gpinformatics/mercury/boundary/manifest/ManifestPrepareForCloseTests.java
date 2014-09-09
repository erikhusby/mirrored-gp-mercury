package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestStatusErrorMatcher.hasError;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestPrepareForCloseTests {

    private ManifestSession session;

    @BeforeMethod
    public void setUp() {
        String sessionPrefix = "DoctorZhivago";
        BspUser user = new BspUser();
        // todo use rp test factory and manifest test factory?
        session = new ManifestSession(new ResearchProject(new BspUser()), sessionPrefix, user);
        session.addRecord(new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, "Fred")));

        for (ManifestRecord manifestRecord : session.getRecords()) {
            manifestRecord.setStatus(ManifestRecord.Status.SCANNED);
        }
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

    @Test
    public void testValidationErrorForUploadedSample() {
        setManifestRecordStatus(ManifestRecord.Status.UPLOADED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(1));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(1));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(0));

        assertThat(manifestStatus.getErrorMessages(), hasSize(1));
        assertThat(manifestStatus, hasError(ManifestRecord.ErrorStatus.MISSING_SAMPLE));
    }

    @Test
    public void testValidationErrorUploadAcceptedSample() {
        // todo parameterize with test above this
        setManifestRecordStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);

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

        ManifestTestFactory.addRecord(session, ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                ImmutableMap.of(Metadata.Key.SAMPLE_ID, record.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue()));
    }

    private void setManifestRecordStatus(ManifestRecord.Status status) {
        session.getRecords().iterator().next().setStatus(status);
    }
}
