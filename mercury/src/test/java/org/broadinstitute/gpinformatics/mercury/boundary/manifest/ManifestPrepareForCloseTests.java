package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

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

import java.util.Collection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsEmptyCollection.empty;

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

        Collection<String> errors = manifestStatus.getErrorMessages();
        Assert.assertEquals(errors.size(), 1);
        Assert.assertTrue(
                errors.iterator().next().contains(ManifestRecord.ErrorStatus.MISSING_SAMPLE.getBaseMessage()));
    }

    @Test
    public void testValidationErrorUploadAcceptedSample() {
        // todo parameterize with test above this
        setManifestRecordStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(1));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(1));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(0));

        Collection<String> errors = manifestStatus.getErrorMessages();
        Assert.assertEquals(errors.size(), 1);
        Assert.assertTrue(
                errors.iterator().next().contains(ManifestRecord.ErrorStatus.MISSING_SAMPLE.getBaseMessage()));
    }

    @Test
    public void testValidationForDuplicateSample() {
        addDuplicateManifestRecord();

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(2));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(1));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(1));

        Collection<String> errors = manifestStatus.getErrorMessages();
        Assert.assertEquals(errors.size(), 1);
        Assert.assertTrue(
                errors.iterator().next().contains(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage()));
    }

    public void testValidationForGenderMismatchSample() {
        ManifestTestFactory.addRecord(session, ManifestRecord.ErrorStatus.MISMATCHED_GENDER,
                ManifestRecord.Status.SCANNED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(2));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(2));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(2));


        assertThat(manifestStatus.getErrorMessages(), is(not(empty())));
        assertThat(manifestStatus.getErrorMessages().size(), is(1));
        assertThat(manifestStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.MISMATCHED_GENDER.getBaseMessage())));
    }

    public void testValidationForUnscannedAndDuplicates() {
        addDuplicateManifestRecord();
        ManifestTestFactory.addRecord(session, null, ManifestRecord.Status.UPLOAD_ACCEPTED);

        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(3));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(2));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(1));

        assertThat(manifestStatus.getErrorMessages(), is(not(empty())));
        assertThat(manifestStatus.getErrorMessages().size(), is(2));
        assertThat(manifestStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.MISSING_SAMPLE.getBaseMessage())));
        assertThat(manifestStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage())));

    }

    public void testValidationForUnscannedAndDuplicatesAndMismatchedGender() {
        addDuplicateManifestRecord();
        ManifestTestFactory.addRecord(session, null, ManifestRecord.Status.UPLOAD_ACCEPTED);
        ManifestTestFactory.addRecord(session, ManifestRecord.ErrorStatus.MISMATCHED_GENDER,
                ManifestRecord.Status.SCANNED);


        ManifestStatus manifestStatus = session.generateSessionStatusForClose();

        assertThat(manifestStatus.getSamplesInManifest(), is(4));
        assertThat(manifestStatus.getSamplesEligibleInManifest(), is(3));
        assertThat(manifestStatus.getSamplesSuccessfullyScanned(), is(2));

        assertThat(manifestStatus.getErrorMessages(), is(not(empty())));
        assertThat(manifestStatus.getErrorMessages().size(), is(3));
        assertThat(manifestStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.MISSING_SAMPLE.getBaseMessage())));
        assertThat(manifestStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getBaseMessage())));
        assertThat(manifestStatus.getErrorMessages(), hasItem(containsString(ManifestRecord.ErrorStatus.MISMATCHED_GENDER.getBaseMessage())));

    }

    private void addDuplicateManifestRecord() {
        ManifestRecord record = session.getRecords().iterator().next();

        ManifestTestFactory.addRecord(session,
                ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID, ManifestRecord.Status.UPLOADED,
                Metadata.Key.SAMPLE_ID, record.getMetadataByKey(Metadata.Key.SAMPLE_ID).getValue());
    }

    private void setManifestRecordStatus(ManifestRecord.Status status) {
        session.getRecords().iterator().next().setStatus(status);
    }
}
