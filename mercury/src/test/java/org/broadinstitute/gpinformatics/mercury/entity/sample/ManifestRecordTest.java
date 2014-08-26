package org.broadinstitute.gpinformatics.mercury.entity.sample;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.Assert;
import org.testng.annotations.Test;

@Test(groups = TestGroups.DATABASE_FREE)
public class ManifestRecordTest {

    private static final Metadata.Key KEY_1 = Metadata.Key.SAMPLE_ID;
    private static final Metadata.Key KEY_2 = Metadata.Key.GENDER;
    private static final Metadata.Key KEY_3 = Metadata.Key.PATIENT_ID;
    private static final String VALUE_1 = "value1";
    private static final String VALUE_2 = "value2";
    private static final String VALUE_3 = "value3";

    public void testCreateRecord() throws Exception {

        // Test with no specified Status or ErrorStatus.
        ManifestRecord testRecord =
                ManifestTestFactory.buildManifestRecord(
                        ImmutableMap.of(KEY_1, VALUE_1, KEY_2, VALUE_2, KEY_3, VALUE_3));

        Assert.assertEquals(testRecord.getMetadataByKey(KEY_1).getValue(), VALUE_1);
        Assert.assertEquals(testRecord.getMetadataByKey(KEY_2).getValue(), VALUE_2);
        Assert.assertEquals(testRecord.getMetadataByKey(KEY_3).getValue(), VALUE_3);
        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.UPLOADED);
        Assert.assertNull(testRecord.getErrorStatus());

        // Test with specified Status and ErrorStatus.
        testRecord.setStatus(ManifestRecord.Status.ABANDONED);
        testRecord.setErrorStatus(ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);

        Assert.assertEquals(testRecord.getStatus(), ManifestRecord.Status.ABANDONED);
        Assert.assertEquals(testRecord.getErrorStatus(), ManifestRecord.ErrorStatus.DUPLICATE_SAMPLE_ID);
    }
}
