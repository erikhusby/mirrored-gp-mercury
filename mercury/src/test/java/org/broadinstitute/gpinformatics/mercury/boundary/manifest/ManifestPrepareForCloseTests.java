package org.broadinstitute.gpinformatics.mercury.boundary.manifest;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestEvent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestRecord;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;

public class ManifestPrepareForCloseTests {

    private ManifestSession session;

    @BeforeMethod
    public void setUp() {
        String sessionPrefix = "DoctorZhivago";
        BspUser user = new BspUser();
        // todo use rp test factory and manifest test factory?
        session = new ManifestSession(new ResearchProject(new BspUser()),sessionPrefix,user);
        session.addRecord(new ManifestRecord(new Metadata(Metadata.Key.SAMPLE_ID, "Fred")));

        for (ManifestRecord manifestRecord : session.getRecords()) {
            manifestRecord.setStatus(ManifestRecord.Status.SCANNED);
        }
    }


    @Test
    public void testPrepareForCloseOnCleanSession() {
        Assert.assertTrue(session.buildErrorMessagesForSession().isEmpty(),
                          "Clean session should have no errors.");
    }

    @Test
    public void testValidationErrorForUploadedSample() {
        setManifestRecordStatus(ManifestRecord.Status.UPLOADED);

        Collection<String> errors = session.buildErrorMessagesForSession();
        Assert.assertEquals(errors.size(),1);
        Assert.assertTrue(errors.iterator().next().contains(ManifestRecord.ErrorStatus.MISSING_SAMPLE.getMessage()));
    }

    @Test
    public void testValidationErrorUploadAcceptedSample() {
        // todo parameterize with test above this
        setManifestRecordStatus(ManifestRecord.Status.UPLOAD_ACCEPTED);

        Collection<String> errors = session.buildErrorMessagesForSession();
        Assert.assertEquals(errors.size(),1);
        Assert.assertTrue(errors.iterator().next().contains(ManifestRecord.ErrorStatus.MISSING_SAMPLE.getMessage()));
    }

    @Test
    public void testValidationForDuplicateSample() {
        addDuplicateManifestRecord();

        Collection<String> errors = session.buildErrorMessagesForSession();
        Assert.assertEquals(errors.size(),1);
        Assert.assertTrue(errors.iterator().next().contains(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID.getMessage()));
    }

    private void addDuplicateManifestRecord() {
        ManifestRecord record = session.getRecords().iterator().next();
        ManifestRecord duplicateRecord = new ManifestRecord(record.getMetadataByKey(Metadata.Key.SAMPLE_ID));
        duplicateRecord.setStatus(ManifestRecord.Status.UPLOADED);
        duplicateRecord.addManifestEvent(new ManifestEvent(ManifestEvent.Severity.QUARANTINED,"Put the sample down and put your hands up."));

        session.addRecord(duplicateRecord);
    }

    private void setManifestRecordStatus(ManifestRecord.Status status) {
        session.getRecords().iterator().next().setStatus(status);
    }
}
