package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Database free test of ManifestRecords, entities representing individual samples within a Buick manifest used for
 * sample registration.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestRecordTest {

    /**
     * Tests basic ManifestRecord creation and Metadata lookup.
     */
    public void createRecord() throws Exception {
        Metadata.Key KEY_1 = Metadata.Key.SAMPLE_ID;
        Metadata.Key KEY_2 = Metadata.Key.GENDER;
        Metadata.Key KEY_3 = Metadata.Key.PATIENT_ID;
        String VALUE_1 = "value1";
        String VALUE_2 = "value2";
        String VALUE_3 = "value3";
        ManifestRecord.Status NEW_STATUS = ManifestRecord.Status.ABANDONED;

        ManifestRecord.ErrorStatus NEW_ERROR_STATUS = ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID;
        // Test with no specified Status or ErrorStatus.
        ManifestSession sessionIn = new ManifestSession();
        ManifestRecord testRecord = new ManifestRecord(
                new Metadata(KEY_1, VALUE_1), new Metadata(KEY_2, VALUE_2), new Metadata(KEY_3, VALUE_3));
        sessionIn.addRecord(testRecord);
        // Basic sanity check of retrieving Metadata by key.
        Assert.assertEquals(testRecord.getMetadataByKey(KEY_1).getValue(), VALUE_1);
        Assert.assertEquals(testRecord.getMetadataByKey(KEY_2).getValue(), VALUE_2);
        Assert.assertEquals(testRecord.getMetadataByKey(KEY_3).getValue(), VALUE_3);
        // Default status should be UPLOADED.
        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.UPLOADED);
        // Default error status should be null (no error).
        Assert.assertNull(testRecord.getErrorStatus());

        // Test with specified Status and ErrorStatus.
        testRecord.setStatus(NEW_STATUS);
        testRecord.setErrorStatus(NEW_ERROR_STATUS);

        Assert.assertEquals(testRecord.getStatus(), NEW_STATUS);
        Assert.assertEquals(testRecord.getErrorStatus(), NEW_ERROR_STATUS);
    }
}
