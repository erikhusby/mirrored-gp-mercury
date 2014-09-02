package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Database free test of ManifestRecords, entities representing individual samples within a Buick manifest used for
 * sample registration.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestRecordTest {

    private Metadata.Key key_1 = Metadata.Key.SAMPLE_ID;
    private Metadata.Key key_2 = Metadata.Key.GENDER;
    private Metadata.Key key_3 = Metadata.Key.PATIENT_ID;
    private String value_1 = "value1";
    private String value_2 = "value2";
    private String value_3 = "value3";
    private ManifestRecord.Status new_status = ManifestRecord.Status.ABANDONED;
    private ManifestRecord.ErrorStatus new_error_status = ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID;

    /**
     * Tests basic ManifestRecord creation and Metadata lookup.
     */
    public void createRecord() throws Exception {

        ManifestSession sessionIn = new ManifestSession();

            // Test with no specified Status or ErrorStatus.
        ManifestRecord testRecord = new ManifestRecord(
                new Metadata(key_1, value_1), new Metadata(key_2, value_2), new Metadata(key_3, value_3));
        sessionIn.addRecord(testRecord);
        // Basic sanity check of retrieving Metadata by key.
        Assert.assertEquals(testRecord.getMetadataByKey(key_1).getValue(), value_1);
        Assert.assertEquals(testRecord.getMetadataByKey(key_2).getValue(), value_2);
        Assert.assertEquals(testRecord.getMetadataByKey(key_3).getValue(), value_3);
        // Default status should be UPLOADED.
        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.UPLOADED);
        // Default error status should be null (no error).
        Assert.assertNull(testRecord.getErrorStatus());

    }

    public void testStatusUpdate() throws Exception {
        // Test with no specified Status or ErrorStatus.
        ManifestRecord testRecord = new ManifestRecord(
                new Metadata(key_1, value_1), new Metadata(key_2, value_2), new Metadata(key_3, value_3));

        // Test with specified Status and ErrorStatus.
        testRecord.setStatus(new_status);
        testRecord.setErrorStatus(new_error_status);

        Assert.assertEquals(testRecord.getStatus(), new_status);
        Assert.assertEquals(testRecord.getErrorStatus(), new_error_status);
    }
}
